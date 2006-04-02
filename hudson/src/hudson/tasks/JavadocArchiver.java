package hudson.tasks;

import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Project;
import hudson.Launcher;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * Publishes javadoc to {@link Project#getJavadocDir()}.
 *
 * @author Kohsuke Kawaguchi
 */
public class JavadocArchiver extends AntBasedBuildStep {
    /**
     * Path to the javadoc directory.
     */
    private final String javadocDir;

    public JavadocArchiver(String javadocDir) {
        this.javadocDir = javadocDir;
    }

    public String getJavadocDir() {
        return javadocDir;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
        // TODO: run tar or something for better remote copy
        File javadoc = new File(build.getParent().getWorkspace().getLocal(), javadocDir);
        if(!javadoc.exists()) {
            listener.error("The specified javadoc directory doesn't exist: "+javadoc);
            return false;
        }
        if(!javadoc.isDirectory()) {
            listener.error("The specified javadoc directory isn't a directory: "+javadoc);
            return false;
        }

        listener.getLogger().println("Publishing javadoc");

        File target = build.getParent().getJavadocDir();
        target.mkdirs();

        Copy copyTask = new Copy();
        copyTask.setProject(new org.apache.tools.ant.Project());
        copyTask.setTodir(target);
        FileSet src = new FileSet();
        src.setDir(javadoc);
        copyTask.addFileset(src);

        execTask(copyTask, listener);

        return true;
    }

    public BuildStepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }


    public static final BuildStepDescriptor DESCRIPTOR = new BuildStepDescriptor(JavadocArchiver.class) {
        public String getDisplayName() {
            return "Publish javadoc";
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new JavadocArchiver(req.getParameter("javadoc_dir"));
        }
    };
}
