package hudson.scm;

import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.Launcher;
import hudson.FilePath;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * No {@link SCM}.
 *
 * @author Kohsuke Kawaguchi
 */
public class NullSCM implements SCM {
    public boolean calcChangeLog(Build build, File changelogFile, Launcher launcher, BuildListener listener) {
        return true;
    }

    public boolean checkout(Build build, Launcher launcher, FilePath remoteDir, BuildListener listener) throws IOException {
        return true;
    }

    public SCMDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public void buildEnvVars(Map env) {
        // noop
    }

    public FilePath getModuleRoot(FilePath workspace) {
        return workspace;
    }

    static final SCMDescriptor DESCRIPTOR = new SCMDescriptor(NullSCM.class) {
        public String getDisplayName() {
            return "None";
        }

        public SCM newInstance(HttpServletRequest req) {
            return new NullSCM();
        }
    };
}
