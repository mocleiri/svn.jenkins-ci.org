package hudson.model;

import hudson.Util;
import hudson.Launcher;
import hudson.Proc;
import hudson.FilePath;

import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Information about a Hudson slave node.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Slave {
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
     * Path to the root of the remote workspace of this node,
     * such as "/net/slave1/hudson"
     */
    private final String remoteFS;

    /**
     * Path to the root of the workspace
     * from within this node, such as "/hudson"
     */
    private final File localFS;

    public Slave(String name, String description, String command, String remoteFS, File localFS) {
        this.name = name;
        this.description = description;
        this.command = command;
        this.remoteFS = remoteFS;
        this.localFS = localFS;
    }

    public String getName() {
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

    /**
     * Returns a {@link Launcher} for executing programs remotely.
     */
    public Launcher createLauncher(BuildListener listener) {
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
                r.add("~/bin/slave");
                r.add(workDir.getRemote());
                for (String s : env) {
                    int index =s.indexOf('=');
                    r.add(s.substring(0,index));
                    r.add(s.substring(index+1));
                }
                r.add("--");
                r.addAll(Arrays.asList(cmd));
                return r.toArray(new String[r.size()]);
            }
        };
    }

    private static final FilePath CURRENT_DIR = new FilePath(new File("."));
}
