package hudson.scm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;

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

    public Descriptor<SCM> getDescriptor() {
        return DESCRIPTOR;
    }

    public void buildEnvVars(Map env) {
        // noop
    }

    public FilePath getModuleRoot(FilePath workspace) {
        return workspace;
    }

    public ChangeLogParser createChangeLogParser() {
        return new NullChangeLogParser();
    }

    static final Descriptor<SCM> DESCRIPTOR = new Descriptor<SCM>(NullSCM.class) {
        public String getDisplayName() {
            return "None";
        }

        public SCM newInstance(HttpServletRequest req) {
            return new NullSCM();
        }
    };
}
