package hudson.maven;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import static hudson.Util.fixNull;
import hudson.maven.agent.Main;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Hudson;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Run.RunnerAbortedException;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.RemoteInputStream;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.SocketInputStream;
import hudson.remoting.SocketOutputStream;
import hudson.remoting.Which;
import hudson.tasks.Maven.MavenInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.IOException2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.JarURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Launches the maven process.
 *
 * @author Kohsuke Kawaguchi
 */
final class MavenProcessFactory implements ProcessCache.Factory {
    private final MavenModuleSet mms;
    private final Launcher launcher;
    /**
     * Environment variables to be set to the maven process.
     * The same variables are exposed to the system property as well.
     */
    private final Map<String,String> envVars;

    /**
     * Optional working directory. Because of the process reuse, we can't always guarantee
     * that the returned Maven process has this as the working directory. But for the
     * aggregator style build, the process reuse is disabled, so in practice this always works.
     *
     * Also, Maven is supposed to work correctly regardless of the process current directory,
     * so a good behaving maven project shouldn't rely on the current project.
     */
    private final FilePath workDir;

    MavenProcessFactory(MavenModuleSet mms, Launcher launcher, Map<String, String> envVars, FilePath workDir) {
        this.mms = mms;
        this.launcher = launcher;
        this.envVars = envVars;
        this.workDir = workDir;
    }

    /**
     * Represents a bi-directional connection.
     *
     * <p>
     * This implementation is remoting aware, so it can be safely sent to the remote callable object.
     *
     * <p>
     * When we run Maven on a slave, the master may not have a direct TCP/IP connectivty to the slave.
     * That means the {@link Channel} between the master and the Maven needs to be tunneled through
     * the channel between master and the slave, then go to TCP socket to the Maven.
     */
    private static final class Connection implements Serializable {
        public InputStream in;
        public OutputStream out;

        Connection(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        private Object writeReplace() {
            return new Connection(new RemoteInputStream(in),new RemoteOutputStream(out));
        }

        private Object readResolve() {
            // ObjectInputStream seems to access data at byte-level and do not do any buffering,
            // so if we are remoted, buffering would be crucial.
            this.in = new BufferedInputStream(in);
            this.out = new BufferedOutputStream(out);
            return this;
        }

        private static final long serialVersionUID = 1L;
    }

    interface Acceptor {
        Connection accept() throws IOException;
        int getPort();
    }

    static final class AcceptorImpl implements Acceptor, Serializable {
        private final ServerSocket serverSocket;
        private Socket socket;

        AcceptorImpl(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public Connection accept() throws IOException {
            socket = serverSocket.accept();
            // we'd only accept one connection
            serverSocket.close();

            return new Connection(new SocketInputStream(socket),new SocketOutputStream(socket));
        }

        public int getPort() {
            return serverSocket.getLocalPort();
        }

        /**
         * When sent to the remote node, send a proxy.
         */
        private Object writeReplace() {
            return Channel.current().export(Acceptor.class, this);
        }
    }

    /**
     * Opens a server socket and returns {@link Acceptor} so that
     * we can accept a connection later on it.
     */
    private static final class SocketHandler implements Callable<Acceptor,IOException> {
        public Acceptor call() throws IOException {
            // open a TCP socket to talk to the launched Maven process.
            // let the OS pick up a random open port
            ServerSocket ss = new ServerSocket();
            ss.bind(null); // new InetSocketAddress(InetAddress.getLocalHost(),0));

            return new AcceptorImpl(ss);
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Starts maven process.
     */
    public Channel newProcess(BuildListener listener, OutputStream out) throws IOException, InterruptedException {
        if(debug)
            listener.getLogger().println("Using env variables: "+ envVars);
        try {
            final Acceptor acceptor = launcher.getChannel().call(new SocketHandler());

            final ArgumentListBuilder cmdLine = buildMavenCmdLine(listener,acceptor.getPort());
            String[] cmds = cmdLine.toCommandArray();
            final Proc proc = launcher.launch(cmds, envVars, out, workDir);

            Connection con = acceptor.accept();

            return new Channel("Channel to Maven "+ Arrays.toString(cmds),
                Computer.threadPoolForRemoting, con.in, con.out) {

                /**
                 * Kill the process when the channel is severed.
                 */
                protected synchronized void terminate(IOException e) {
                    super.terminate(e);
                    try {
                        proc.kill();
                    } catch (IOException x) {
                        // we are already in the error recovery mode, so just record it and move on
                        LOGGER.log(Level.INFO, "Failed to terminate the severed connection",x);
                    } catch (InterruptedException x) {
                        // process the interrupt later
                        Thread.currentThread().interrupt();
                    }
                }

                public synchronized void close() throws IOException {
                    super.close();
                    // wait for Maven to complete
                    try {
                        proc.join();
                    } catch (InterruptedException e) {
                        // process the interrupt later
                        Thread.currentThread().interrupt();
                    }
                }
            };

//            return launcher.launchChannel(buildMavenCmdLine(listener).toCommandArray(),
//                out, workDir, envVars);
        } catch (IOException e) {
            if(fixNull(e.getMessage()).contains("java: not found")) {
                // diagnose issue #659
                JDK jdk = mms.getJDK();
                if(jdk==null)
                    throw new IOException2(mms.getDisplayName()+" is not configured with a JDK, but your PATH doesn't include Java",e);
            }
            throw e;
        }
    }

    /**
     * Builds the command line argument list to launch the maven process.
     *
     * UGLY.
     */
    private ArgumentListBuilder buildMavenCmdLine(BuildListener listener,int tcpPort) throws IOException, InterruptedException {
        MavenInstallation mvn = getMavenInstallation();
        if(mvn==null) {
            listener.error("Maven version is not configured for this project. Can't determine which Maven to run");
            throw new RunnerAbortedException();
        }
        if(mvn.getMavenHome()==null) {
            listener.error("Maven '%s' doesn't have its home set",mvn.getName());
            throw new RunnerAbortedException();
        }

        // find classworlds.jar
        String classWorldsJar = launcher.getChannel().call(new GetClassWorldsJar(mvn.getMavenHome(),listener));

        boolean isMaster = getCurrentNode()== Hudson.getInstance();
        FilePath slaveRoot=null;
        if(!isMaster)
            slaveRoot = getCurrentNode().getRootPath();

        ArgumentListBuilder args = new ArgumentListBuilder();
        JDK jdk = mms.getJDK();
        if(jdk==null)
            args.add("java");
        else
            args.add(jdk.getJavaHome()+"/bin/java");

        if(debugPort!=0)
            args.add("-Xrunjdwp:transport=dt_socket,server=y,address="+debugPort);
        if(yjp)
            args.add("-agentlib:yjpagent=tracing");

        args.addTokenized(getMavenOpts());

        args.add("-cp");
        args.add(
            (isMaster? Which.jarFile(Main.class).getAbsolutePath():slaveRoot.child("maven-agent.jar").getRemote())+
            (launcher.isUnix()?":":";")+classWorldsJar);
        args.add(Main.class.getName());

        // M2_HOME
        args.add(mvn.getMavenHome());

        // remoting.jar
        args.add(launcher.getChannel().call(new GetRemotingJar()));
        // interceptor.jar
        args.add(isMaster?
            Which.jarFile(hudson.maven.agent.PluginManagerInterceptor.class).getAbsolutePath():
            slaveRoot.child("maven-interceptor.jar").getRemote());
        args.add(tcpPort);
        return args;
    }

    public String getMavenOpts() {
        String opts = mms.getMavenOpts();
        if (opts == null)
            return null;

        for (String key : envVars.keySet())
            opts = opts.replace("${" + key + "}", envVars.get(key));
        
        return opts;
    }

    public MavenInstallation getMavenInstallation() {
        return mms.getMaven();
    }

    public JDK getJava() {
        return mms.getJDK();
    }

    /**
     * Finds classworlds.jar
     */
    private static final class GetClassWorldsJar implements Callable<String,IOException> {
        private final String mvnHome;
        private final TaskListener listener;

        private GetClassWorldsJar(String mvnHome, TaskListener listener) {
            this.mvnHome = mvnHome;
            this.listener = listener;
        }

        public String call() throws IOException {
            File home = new File(mvnHome);
            File bootDir = new File(home, "core/boot");
            File[] classworlds = bootDir.listFiles(CLASSWORLDS_FILTER);
            if(classworlds==null || classworlds.length==0) {
                // Maven 2.0.6 puts it to a different place
                bootDir = new File(home, "boot");
                classworlds = bootDir.listFiles(CLASSWORLDS_FILTER);
                if(classworlds==null || classworlds.length==0) {
                    listener.error(Messages.MavenProcessFactory_ClassWorldsNotFound(home));
                    throw new RunnerAbortedException();
                }
            }
            return classworlds[0].getAbsolutePath();
        }
    }

    private static final class GetRemotingJar implements Callable<String,IOException> {
        public String call() throws IOException {
            URL classFile = Main.class.getClassLoader().getResource(hudson.remoting.Launcher.class.getName().replace('.','/')+".class");

            // JNLP returns the URL where the jar was originally placed (like http://hudson.dev.java.net/...)
            // not the local cached file. So we need a rather round about approach to get to
            // the local file name.
            URLConnection con = classFile.openConnection();
            if (con instanceof JarURLConnection) {
                JarURLConnection connection = (JarURLConnection) con;
                return connection.getJarFile().getName();
            }

            return Which.jarFile(hudson.remoting.Launcher.class).getPath();
        }
    }

    /**
     * Returns the current {@link Node} on which we are buildling.
     */
    private Node getCurrentNode() {
        return Executor.currentExecutor().getOwner().getNode();
    }

    private static final FilenameFilter CLASSWORLDS_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.startsWith("classworlds") && name.endsWith(".jar");
        }
    };

    /**
     * Set true to produce debug output.
     */
    public static boolean debug = false;

    /**
     * If not 0, launch Maven with a debugger port.
     */
    public static int debugPort;

    public static boolean profile = Boolean.getBoolean("hudson.maven.profile");
    
    /**
     * If true, launch Maven with YJP offline profiler agent.
     */
    public static boolean yjp = Boolean.getBoolean("hudson.maven.yjp");

    static {
        String port = System.getProperty("hudson.maven.debugPort");
        if(port!=null)
            debugPort = Integer.parseInt(port);
    }

    private static final Logger LOGGER = Logger.getLogger(MavenProcessFactory.class.getName());
}
