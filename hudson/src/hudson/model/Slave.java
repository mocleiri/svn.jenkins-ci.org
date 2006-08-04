package hudson.model;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Information about a Hudson slave node.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Slave implements Node {
    /**
     * Name of this slave node.
     */
    private final String name;

    /**
     * Description of this node.
     */
    private final String description;

    /**
     * Commands to run to post a job on this machine.
     */
    private final String command;

    /**
     * Path to the root of the workspace
     * from within this node, such as "/hudson"
     */
    private final String remoteFS;

    /**
     * Path to the root of the remote workspace of this node,
     * such as "/net/slave1/hudson"
     */
    private final File localFS;

    /**
     * Number of executors of this node.
     */
    private int numExecutors = 2;

    /**
     * Job allocation strategy.
     */
    private Mode mode;

    public Slave(String name, String description, String command, String remoteFS, File localFS, int numExecutors, Mode mode) {
        this.name = name;
        this.description = description;
        this.command = command;
        this.remoteFS = remoteFS;
        this.localFS = localFS;
        this.numExecutors = numExecutors;
        this.mode = mode;
    }

    public String getNodeName() {
        return name;
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

    public String getDescription() {
        return description;
    }

    public FilePath getFilePath() {
        return new FilePath(localFS,remoteFS);
    }

    public int getNumExecutors() {
        return numExecutors;
    }

    public Mode getMode() {
        return mode;
    }

    public Launcher createLauncher(BuildListener listener) {
        if(command.length()==0) // local alias
            return new Launcher(listener);


        return new Launcher(listener) {
            @Override
            public Proc launch(String[] cmd, String[] env, OutputStream out, FilePath workDir) throws IOException {
                return super.launch(prepend(cmd,env,workDir), env, null, out);
            }

            @Override
            public Proc launch(String[] cmd, String[] env, InputStream in, OutputStream out) throws IOException {
                return super.launch(prepend(cmd,env,CURRENT_DIR), env, in, out);
            }

            @Override
            public boolean isUnix() {
                // Err on Unix, since we expect that to be the common slaves
                return remoteFS.indexOf('\\')==-1;
            }

            private String[] prepend(String[] cmd, String[] env, FilePath workDir) {
                List<String> r = new ArrayList<String>();
                r.addAll(Arrays.asList(getCommandTokens()));
                r.add(getFilePath().child("bin").child("slave").getRemote());
                r.add(workDir.getRemote());
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
                        c = '"'+c+'"';
                    r.add(c);
                }
                return r.toArray(new String[r.size()]);
            }
        };
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Slave that = (Slave) o;

        return name.equals(that.name);

    }

    public int hashCode() {
        return name.hashCode();
    }

    private static final FilePath CURRENT_DIR = new FilePath(new File("."));
}
