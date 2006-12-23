package hudson.model;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * {@link Slave} that doesn't have the remoting infrastructure.
 * For compatibility with Hudson &lt; 1.68.
 *
 * @author Kohsuke Kawaguchi
 */
public class LegacySlave extends Slave {
    /**
     * Commands to run to post a job on this machine.
     */
    private final String command;

    /**
     * Path to the root of the remote workspace of this node
     * from the view point of the master,
     * such as "/net/slave1/hudson"
     */
    private final File localFS;

    public LegacySlave(String name, String description, String command, String remoteFS, File localFS, int numExecutors, Mode mode) throws FormException {
        super(name,description,remoteFS,numExecutors,mode);
        this.command = command;
        this.localFS = localFS;
    }

    public String getCommand() {
        return command;
    }

    public String[] getCommandTokens() {
        return Util.tokenize(command);
    }

    public String getRemoteFS() {
        return remoteFS;
    }

    public File getLocalFS() {
        return localFS;
    }

    /**
     * Gets the file path that represents the root directory of the Hudson
     * workspace on this slave.
     */
    public FilePath getFilePath() {
        return new FilePath(localFS,remoteFS);
    }

    @Override
    public long getClockDifference() throws IOException {
        File testFile = new File(localFS,"clock.skew");
        FileOutputStream os = new FileOutputStream(testFile);
        long now = new Date().getTime();
        os.close();

        long r = now - testFile.lastModified();

        testFile.delete();

        return r;
    }

    public Launcher createLauncher(TaskListener listener) {
        if(command.length()==0) // local alias
            return new Launcher.LocalLauncher(listener);

        return new Launcher.LocalLauncher(listener, null) {
            @Override
            public Proc launch(String[] cmd,String[] env,InputStream in,OutputStream out, FilePath workDir) throws IOException {
                return super.launch(prepend(cmd,env,workDir), env, in, out, workDir);
            }

            @Override
            public boolean isUnix() {
                // Err on Unix, since we expect that to be the common slaves
                return remoteFS.indexOf('\\')==-1;
            }

            private String[] prepend(String[] cmd, String[] env, FilePath workDir) {
                ArgumentListBuilder r = new ArgumentListBuilder();
                r.add(getFilePath().child("bin").child("slave").getRemote());
                r.addQuoted(workDir.getRemote());
                for (String s : env) {
                    int index =s.indexOf('=');
                    r.add(s.substring(0,index));
                    r.add(s.substring(index+1));
                }
                r.add("--");
                for (String c : cmd) {
                    // ssh passes the command and parameters in one string.
                    // see RFC 4254 section 6.5.
                    // so the consequence that we need to give
                    // {"ssh",...,"ls","\"a b\""} to list a file "a b".
                    // If we just do
                    // {"ssh",...,"ls","a b"} (which is correct if this goes directly to Runtime.exec),
                    // then we end up executing "ls","a","b" on the other end.
                    //
                    // I looked at rsh source code, and that behave the same way.
                    if(c.indexOf(' ')>=0)
                        r.addQuoted(c);
                    else
                        r.add(c);
                }
                return r.toCommandArray();
            }

        };
    }

    public Computer createComputer() {
        return new ComputerImpl(this);
    }

    public FilePath getWorkspaceRoot() {
        return getFilePath().child("workspace");
    }

    public static final class ComputerImpl extends Computer {
        private ComputerImpl(LegacySlave slave) {
            super(slave);
        }

        @Override
        public VirtualChannel getChannel() {
            return null;
        }

        protected Class<LegacySlave> getNodeClass() {
            return LegacySlave.class;
        }
    }
}
