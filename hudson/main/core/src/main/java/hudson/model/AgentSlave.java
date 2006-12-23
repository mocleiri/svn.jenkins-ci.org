package hudson.model;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Proc.RemoteProc;
import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.RemoteInputStream;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.VirtualChannel;
import hudson.util.StreamCopyThread;
import hudson.util.StreamTaskListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Slave} controlled via the remoting agent.
 *
 * @author Kohsuke Kawaguchi
 */
public class AgentSlave extends Slave {
    /**
     * Launch command.
     */
    private String command;

    public AgentSlave(String name, String description, String remoteFS, int numExecutors, Mode mode, String command) throws FormException {
        super(name, description, remoteFS, numExecutors, mode);
        this.command = command;
    }

    @Override
    public long getClockDifference() throws IOException {
        VirtualChannel channel = getComputer().getChannel();
        if(channel==null)   return 0;   // can't check

        try {
            long startTime = System.currentTimeMillis();
            long slaveTime = channel.call(new Callable<Long,RuntimeException>() {
                public Long call() {
                    return System.currentTimeMillis();
                }
            });
            long endTime = System.currentTimeMillis();

            return (startTime+endTime)/2 - slaveTime;
        } catch (InterruptedException e) {
            return 0;   // couldn't check
        }
    }

    @Override
    public FilePath getWorkspaceRoot() {
        return null;
    }

    public Computer createComputer() {
        return new ComputerImpl(this);
    }

    public Launcher createLauncher(TaskListener listener) {
        return new Launcher(listener, getComputer().getChannel()) {

            public Proc launch(final String[] cmd, final String[] env, InputStream _in, OutputStream _out, FilePath _workDir) throws IOException {
                printCommandLine(cmd,_workDir);

                final OutputStream out = new RemoteOutputStream(_out);
                final InputStream  in  = new RemoteInputStream(_in);
                final String workDir = _workDir.getRemote();

                return new RemoteProc(getChannel().callAsync(new Callable<Integer, IOException>() {
                    public Integer call() throws IOException {
                        Proc p = new LocalLauncher(TaskListener.NULL).launch(cmd, env, in, out,
                            new FilePath(new File(workDir)));
                        return p.join();
                    }
                }));
            }

            @Override
            public boolean isUnix() {
                // Windows can handle '/' as a path separator but Unix can't,
                // so err on Unix side
                return remoteFS.indexOf("\\")==-1;
            }
        };
    }

    public static final class ComputerImpl extends Computer {
        private Channel channel;

        private ComputerImpl(final AgentSlave slave) {
            super(slave);

            // launch the slave agent asynchronously
            threadPoolForRemoting.execute(new Runnable() {
                // TODO: do this only for nodes that are so configured.
                // TODO: support passive connection via JNLP
                public void run() {
                    ByteArrayOutputStream log = new ByteArrayOutputStream();
                    StreamTaskListener listener = new StreamTaskListener(log);
                    try {
                        Process proc = Runtime.getRuntime().exec(slave.command);

                        // capture error information from stderr. this will terminate itself
                        // when the process is killed.
                        new StreamCopyThread("stderr copier for remote agent on "+slave.getNodeName(),
                            proc.getErrorStream(), log).start();

                        channel = new Channel(nodeName,threadPoolForRemoting,
                            proc.getInputStream(),proc.getOutputStream(), log);

                        logger.info("slave agent launched for "+slave.getNodeName());
                    } catch (IOException e) {
                        Util.displayIOException(e,listener);

                        String msg = Util.getWin32ErrorMessage(e);
                        if(msg==null)   msg="";
                        else            msg=" : "+msg;
                        logger.log(Level.SEVERE, "Unable to launch the slave agent for "+slave.getNodeName()+msg,e);
                    }
                }
            });
        }

        @Override
        public VirtualChannel getChannel() {
            return channel;
        }

        @Override
        protected void kill() {
            super.kill();

            Channel c = channel;
            channel = null;
            if(c!=null)
                try {
                    c.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Failed to terminate channel to "+getDisplayName(),e);
                }
        }

        protected Class<AgentSlave> getNodeClass() {
            return AgentSlave.class;
        }

        private static final Logger logger = Logger.getLogger(ComputerImpl.class.getName());
    }

    private static final Logger logger = Logger.getLogger(AgentSlave.class.getName());
}
