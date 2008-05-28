package hudson.slaves;

import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Slave;
import hudson.model.Messages;
import hudson.model.LargeText;
import hudson.model.Node;
import hudson.remoting.Channel;
import hudson.remoting.Which;
import hudson.remoting.VirtualChannel;
import hudson.remoting.Callable;
import hudson.util.StreamTaskListener;
import hudson.util.NullStream;
import hudson.util.RingBufferLogHandler;
import hudson.FilePath;
import hudson.Util;
import hudson.maven.agent.Main;
import hudson.maven.agent.PluginManagerInterceptor;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link Computer} for {@link Slave}s.
 *
 * @author Kohsuke Kawaguchi
 */
public final class SlaveComputer extends Computer {
    private volatile Channel channel;
    private Boolean isUnix;
    private ComputerLauncher launcher;

    /**
     * Number of failed attempts to reconnect to this node
     * (so that if we keep failing to reconnect, we can stop
     * trying.)
     */
    private transient int numRetryAttempt;

    /**
     * This is where the log from the remote agent goes.
     */
    private File getLogFile() {
        return new File(Hudson.getInstance().getRootDir(),"slave-"+nodeName+".log");
    }

    public SlaveComputer(Slave slave) {
        super(slave);
    }

    /**
     * True if this computer is a Unix machine (as opposed to Windows machine).
     *
     * @return
     *      null if the computer is disconnected and therefore we don't know whether it is Unix or not.
     */
    public Boolean isUnix() {
        return isUnix;
    }

    public Slave getNode() {
        return (Slave)super.getNode();
    }

    @Override
    @Deprecated
    public boolean isJnlpAgent() {
        return launcher instanceof JNLPLauncher;
    }

    @Override
    public boolean isLaunchSupported() {
        return launcher.isLaunchSupported();
    }

    public ComputerLauncher getLauncher() {
        return launcher;
    }

    public void launch() {
        if(channel!=null)   return;

        closeChannel();
        Computer.threadPoolForRemoting.execute(new Runnable() {
            public void run() {
                // do this on another thread so that the lengthy launch operation
                // (which is typical) won't block UI thread.
                launcher.launch(SlaveComputer.this, new StreamTaskListener(openLogFile()));
            }
        });
    }

    public OutputStream openLogFile() {
        OutputStream os;
        try {
            os = new FileOutputStream(getLogFile());
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Failed to create log file "+getLogFile(),e);
            os = new NullStream();
        }
        return os;
    }

    private final Object channelLock = new Object();

    /**
     * Creates a {@link Channel} from the given stream and sets that to this slave.
     */
    public void setChannel(InputStream in, OutputStream out, OutputStream launchLog, Channel.Listener listener) throws IOException, InterruptedException {
        synchronized(channelLock) {
            if(this.channel!=null)
                throw new IllegalStateException("Already connected");

            Channel channel = new Channel(nodeName,threadPoolForRemoting, Channel.Mode.NEGOTIATE,
                in,out, launchLog);
            channel.addListener(new Channel.Listener() {
                public void onClosed(Channel c,IOException cause) {
                    SlaveComputer.this.channel = null;
                }
            });
            channel.addListener(listener);

            PrintWriter log = new PrintWriter(launchLog,true);

            {// send jars that we need for our operations
                // TODO: maybe I should generalize this kind of "post initialization" processing
                FilePath dst = new FilePath(channel,getNode().getRemoteFS());
                new FilePath(Which.jarFile(Main.class)).copyTo(dst.child("maven-agent.jar"));
                log.println("Copied maven-agent.jar");
                new FilePath(Which.jarFile(PluginManagerInterceptor.class)).copyTo(dst.child("maven-interceptor.jar"));
                log.println("Copied maven-interceptor.jar");
            }

            isUnix = channel.call(new DetectOS());
            log.println(isUnix? Messages.Slave_UnixSlave():Messages.Slave_WindowsSlave());

            // install log handler
            channel.call(new LogInstaller());

            numRetryAttempt = 0;

            // prevent others from seeing a channel that's not properly initialized yet
            this.channel = channel;
        }
        Hudson.getInstance().getQueue().scheduleMaintenance();
    }

    @Override
    public VirtualChannel getChannel() {
        return channel;
    }

    public List<LogRecord> getLogRecords() throws IOException, InterruptedException {
        if(channel==null)
            return Collections.emptyList();
        else
            return channel.call(new Callable<List<LogRecord>,RuntimeException>() {
                public List<LogRecord> call() {
                    return new ArrayList<LogRecord>(SLAVE_LOG_HANDLER.getView());
                }
            });
    }

    public void doDoDisconnect(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
        disconnect();
        rsp.sendRedirect(".");
    }

    @Override
    public void disconnect() {
        Computer.threadPoolForRemoting.execute(new Runnable() {
            public void run() {
                // do this on another thread so that any lengthy disconnect operation
                // (which could be typical) won't block UI thread.
                StreamTaskListener listener = new StreamTaskListener(openLogFile());
                launcher.beforeDisconnect(SlaveComputer.this, listener);
                closeChannel();
                launcher.afterDisconnect(SlaveComputer.this, listener);
            }
        });
    }

    public void doLaunchSlaveAgent(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        if(channel!=null) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        launch();

        // TODO: would be nice to redirect the user to "launching..." wait page,
        // then spend a few seconds there and poll for the completion periodically.
        rsp.sendRedirect("log");
    }

    public void tryReconnect() {
        numRetryAttempt++;
        if(numRetryAttempt<6 || (numRetryAttempt%12)==0) {
            // initially retry several times quickly, and after that, do it infrequently.
            logger.info("Attempting to reconnect "+nodeName);
            launch();
        }
    }

    /**
     * Gets the string representation of the slave log.
     */
    public String getLog() throws IOException {
        return Util.loadFile(getLogFile());
    }

    /**
     * Handles incremental log.
     */
    public void doProgressiveLog( StaplerRequest req, StaplerResponse rsp) throws IOException {
        new LargeText(getLogFile(),false).doProgressText(req,rsp);
    }

    /**
     * Serves jar files for JNLP slave agents.
     */
    public Slave.JnlpJar getJnlpJars(String fileName) {
        return new Slave.JnlpJar(fileName);
    }

    @Override
    protected void kill() {
        super.kill();
        closeChannel();
    }

    public RetentionStrategy getRetentionStrategy() {
        return getNode().getRetentionStrategy();
    }

    /**
     * If still connected, disconnect.
     */
    private void closeChannel() {
        // TODO: race condition between this and the setChannel method.
        Channel c = channel;
        channel = null;
        isUnix = null;
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to terminate channel to " + getDisplayName(), e);
            }
        }
    }

    @Override
    protected void setNode(Node node) {
        super.setNode(node);
        launcher = ((Slave)node).getLauncher();

        // maybe the configuration was changed to relaunch the slave, so try to re-launch now.
        launch();
    }

    private static final Logger logger = Logger.getLogger(SlaveComputer.class.getName());

    private static final class DetectOS implements Callable<Boolean,IOException> {
        public Boolean call() throws IOException {
            return File.pathSeparatorChar==':';
        }
    }

    /**
     * This field is used on each slave node to record log records on the slave.
     */
    private static final RingBufferLogHandler SLAVE_LOG_HANDLER = new RingBufferLogHandler();

    private static class LogInstaller implements Callable<Void,RuntimeException> {
        public Void call() {
            // avoid double installation of the handler
            Logger logger = Logger.getLogger("hudson");
            logger.removeHandler(SLAVE_LOG_HANDLER);
            logger.addHandler(SLAVE_LOG_HANDLER);
            return null;
        }
        private static final long serialVersionUID = 1L;
    }
}
