package hudson.scm;

import hudson.EnvVars;
import hudson.Proc;
import hudson.Launcher;
import hudson.FilePath;
import hudson.model.BuildListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Common implementation between {@link CVSSCM} and {@link SubversionSCM}.
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractCVSFamilySCM implements SCM {
    /**
     * Invokes the command with the specified command line option and wait for its completion.
     *
     * @param dir
     *      if launching locally this is a local path, otherwise a remote path.
     */
    protected final boolean run(Launcher launcher, String cmd, BuildListener listener, FilePath dir) throws IOException {
        //listener.getLogger().println("$ "+cmd);

        Map env = createEnvVarMap();

        int r = launcher.launch(cmd,env,listener.getLogger(),dir).join();
        if(r!=0)
            listener.fatalError(getDescriptor().getDisplayName()+" failed");

        return r==0;
    }

    protected final Map createEnvVarMap() {
        Map env = new HashMap();
        buildEnvVars(env);
        return env;
    }

    protected final boolean createEmptyChangeLog(File changelogFile, BuildListener listener) {
        try {
            FileWriter w = new FileWriter(changelogFile);
            w.write("<changelog/>");
            w.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace(listener.error(e.getMessage()));
            return false;
        }
    }

    protected final String nullify(String s) {
        if(s==null)     return null;
        if(s.trim().length()==0)    return null;
        return s;
    }
}
