package hudson;

import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.remoting.VirtualChannel;
import hudson.remoting.Channel;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.RemoteInputStream;
import hudson.remoting.Pipe;
import hudson.remoting.Callable;
import hudson.Proc.LocalProc;
import hudson.Proc.RemoteProc;
import hudson.util.StreamCopyThread;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Starts a process.
 *
 * <p>
 * This hides the difference between running programs locally vs remotely.
 *
 *
 * <h2>'env' parameter</h2>
 * <p>
 * To allow important environment variables to be copied over to the remote machine,
 * the 'env' parameter shouldn't contain default inherited environment variables
 * (which often contains machine-specific information, like PATH, TIMEZONE, etc.)
 *
 * <p>
 * {@link Launcher} is responsible for inheriting environment variables.
 *
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Launcher {

    protected final TaskListener listener;

    protected final VirtualChannel channel;

    public Launcher(TaskListener listener, VirtualChannel channel) {
        this.listener = listener;
        this.channel = channel;
    }

    /**
     * Gets the channel that can be used to run a program remotely.
     *
     * @return
     *      null if the target node is not configured to support this.
     *      this is a transitional measure.
     *      Note that a launcher for the master is always non-null.
     */
    public VirtualChannel getChannel() {
        return channel;
    }

    public final Proc launch(String cmd, Map<String,String> env, OutputStream out, FilePath workDir) throws IOException {
        return launch(cmd,Util.mapToEnv(env),out,workDir);
    }

    public final Proc launch(String[] cmd, Map<String,String> env, OutputStream out, FilePath workDir) throws IOException {
        return launch(cmd,Util.mapToEnv(env),out,workDir);
    }

    public final Proc launch(String[] cmd, Map<String,String> env, InputStream in, OutputStream out) throws IOException {
        return launch(cmd,Util.mapToEnv(env),in,out);
    }

    public final Proc launch(String cmd,String[] env,OutputStream out, FilePath workDir) throws IOException {
        return launch(Util.tokenize(cmd),env,out,workDir);
    }

    public final Proc launch(String[] cmd,String[] env,OutputStream out, FilePath workDir) throws IOException {
        return launch(cmd,env,null,out,workDir);
    }

    public final Proc launch(String[] cmd,String[] env,InputStream in,OutputStream out) throws IOException {
        return launch(cmd,env,in,out,null);
    }

    /**
     * @param in
     *      null if there's no input.
     * @param workDir
     *      null if the working directory could be anything.
     * @param out
     *      stdout and stderr of the process will be sent to this stream.
     *      the stream won't be closed.
     */
    public abstract Proc launch(String[] cmd,String[] env,InputStream in,OutputStream out, FilePath workDir) throws IOException;

    /**
     * Launches a specified process and connects its input/output to a {@link Channel}, then
     * return it.
     *
     * <p>
     * When the returned channel is terminated, the process will be killed.
     *
     * @param out
     *      Where the stderr from the launched process will be sent.
     * @param envVars
     *      Environment variable overrides. In adition to what the current process
     *      is inherited (if this is going to be launched from a slave agent, that
     *      becomes the "current" process), these variables will be also set.
     */
    public abstract Channel launchChannel(String[] cmd, OutputStream out, FilePath workDir, Map<String,String> envVars) throws IOException, InterruptedException;

    /**
     * Returns true if this {@link Launcher} is going to launch on Unix.
     */
    public boolean isUnix() {
        return File.pathSeparatorChar==':';
    }

    /**
     * Prints out the command line to the listener so that users know what we are doing.
     */
    protected final void printCommandLine(String[] cmd, FilePath workDir) {
        StringBuffer buf = new StringBuffer();
        if (workDir != null) {
            buf.append('[');
            if(showFullPath)
                buf.append(workDir.getRemote());
            else
                buf.append(workDir.getRemote().replaceFirst("^.+[/\\\\]", ""));
            buf.append("] ");
        }
        buf.append('$');
        for (String c : cmd) {
            buf.append(' ');
            if(c.indexOf(' ')>=0) {
                if(c.indexOf('"')>=0)
                    buf.append('\'').append(c).append('\'');
                else
                    buf.append('"').append(c).append('"');
            } else
                buf.append(c);
        }
        listener.getLogger().println(buf.toString());
    }

    /**
     * {@link Launcher} that launches process locally.
     */
    public static class LocalLauncher extends Launcher {
        public LocalLauncher(TaskListener listener) {
            this(listener,Hudson.MasterComputer.localChannel);
        }

        public LocalLauncher(TaskListener listener, VirtualChannel channel) {
            super(listener, channel);
        }

        public Proc launch(String[] cmd,String[] env,InputStream in,OutputStream out, FilePath workDir) throws IOException {
            printCommandLine(cmd, workDir);
            return new LocalProc(cmd,Util.mapToEnv(inherit(env)),in,out, toFile(workDir));
        }

        private File toFile(FilePath f) {
            return f==null ? null : new File(f.getRemote());
        }

        public Channel launchChannel(String[] cmd, OutputStream out, FilePath workDir, Map<String,String> envVars) throws IOException {
            printCommandLine(cmd, workDir);

            final Process proc = Runtime.getRuntime().exec(cmd, Util.mapToEnv(Launcher.inherit(envVars)), toFile(workDir));

            Thread t2 = new StreamCopyThread(cmd+": stderr copier", proc.getErrorStream(), out);
            t2.start();

            return new Channel("locally launched channel on "+cmd,
                Computer.threadPoolForRemoting, proc.getInputStream(), proc.getOutputStream(), out) {

                /**
                 * Kill the process when the channel is severed.
                 */
                protected synchronized void terminate(IOException e) {
                    super.terminate(e);
                    proc.destroy();
                }
            };
        }

        /**
         * Expands the list of environment variables by inheriting current env variables.
         */
        private Map<String,String> inherit(String[] env) {
            Map<String,String> m = new HashMap<String,String>(EnvVars.masterEnvVars);
            for (String e : env) {
                int index = e.indexOf('=');
                String key = e.substring(0,index);
                String value = e.substring(index+1);
                if(value.length()==0)
                    m.remove(key);
                else
                    m.put(key,value);
            }
            return m;
        }
    }

    /**
     * Launches processes remotely by using the given channel.
     */
    public static class RemoteLauncher extends Launcher {
        private final boolean isUnix;

        public RemoteLauncher(TaskListener listener, VirtualChannel channel, boolean isUnix) {
            super(listener, channel);
            this.isUnix = isUnix;
        }

        public Proc launch(final String[] cmd, final String[] env, InputStream _in, OutputStream _out, FilePath _workDir) throws IOException {
            printCommandLine(cmd,_workDir);

            final OutputStream out = new RemoteOutputStream(new CloseProofOutputStream(_out));
            final InputStream  in  = _in==null ? null : new RemoteInputStream(_in);
            final String workDir = _workDir==null ? null : _workDir.getRemote();

            return new RemoteProc(getChannel().callAsync(new RemoteLaunchCallable(cmd, env, in, out, workDir)));
        }

        public Channel launchChannel(String[] cmd, OutputStream err, FilePath _workDir, Map<String,String> envOverrides) throws IOException, InterruptedException {
            printCommandLine(cmd, _workDir);

            Pipe out = Pipe.createRemoteToLocal();
            final String workDir = _workDir==null ? null : _workDir.getRemote();

            OutputStream os = getChannel().call(new RemoteChannelLaunchCallable(cmd, out, err, workDir, envOverrides));

            return new Channel("remotely launched channel on "+channel,
                Computer.threadPoolForRemoting, out.getIn(), new BufferedOutputStream(os));
        }

        @Override
        public boolean isUnix() {
            return isUnix;
        }
    }

    private static class RemoteLaunchCallable implements Callable<Integer,IOException> {
        private final String[] cmd;
        private final String[] env;
        private final InputStream in;
        private final OutputStream out;
        private final String workDir;

        public RemoteLaunchCallable(String[] cmd, String[] env, InputStream in, OutputStream out, String workDir) {
            this.cmd = cmd;
            this.env = env;
            this.in = in;
            this.out = out;
            this.workDir = workDir;
        }

        public Integer call() throws IOException {
            Proc p = new LocalLauncher(TaskListener.NULL).launch(cmd, env, in, out,
                workDir ==null ? null : new FilePath(new File(workDir)));
            return p.join();
        }

        private static final long serialVersionUID = 1L;
    }

    private static class RemoteChannelLaunchCallable implements Callable<OutputStream,IOException> {
        private final String[] cmd;
        private final Pipe out;
        private final String workDir;
        private final OutputStream err;
        private final Map<String,String> envOverrides;

        public RemoteChannelLaunchCallable(String[] cmd, Pipe out, OutputStream err, String workDir, Map<String,String> envOverrides) {
            this.cmd = cmd;
            this.out = out;
            this.err = new RemoteOutputStream(err);
            this.workDir = workDir;
            this.envOverrides = envOverrides;
        }

        public OutputStream call() throws IOException {
            Process p = Runtime.getRuntime().exec(cmd,
                Util.mapToEnv(inherit(envOverrides)),
                workDir == null ? null : new File(workDir));

            new StreamCopyThread("stdin copier for remote agent on "+cmd,
                p.getInputStream(), out.getOut()).start();
            new StreamCopyThread("stderr copier for remote agent on "+cmd,
                p.getErrorStream(), err).start();

            // TODO: don't we need to join?

            return new RemoteOutputStream(p.getOutputStream());
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Expands the list of environment variables by inheriting current env variables.
     */
    private static Map<String,String> inherit(Map<String,String> overrides) {
        Map<String,String> m = new HashMap<String,String>(EnvVars.masterEnvVars);
        m.putAll(overrides);
        return m;
    }

    /**
     * Debug option to display full current path instead of just the last token.
     */
    public static boolean showFullPath = false;
}
