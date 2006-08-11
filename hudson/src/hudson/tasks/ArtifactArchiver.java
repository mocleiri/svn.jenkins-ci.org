package hudson.tasks;

import hudson.Launcher;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * Copies the artifacts into an archive directory.
 *
 * @author Kohsuke Kawaguchi
 */
public class ArtifactArchiver extends AntBasedBuildStep {

    /**
     * Comma-separated list of files/directories to be archived.
     */
    private final String artifacts;

    public ArtifactArchiver(String artifacts) {
        this.artifacts = artifacts;
    }

    public String getArtifacts() {
        return artifacts;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        listener.getLogger().println("Removing artifacts from the previous build");

        File dir = build.getArtifactsDir();
        if(!dir.exists())   return true;

        Delete delTask = new Delete();
        delTask.setProject(new org.apache.tools.ant.Project());
        delTask.setDir(dir);
        delTask.setIncludes(artifacts);

        execTask(delTask,listener);

        return true;
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
        Project p = build.getProject();

        Copy copyTask = new Copy();
        copyTask.setProject(new org.apache.tools.ant.Project());
        File dir = build.getArtifactsDir();
        dir.mkdirs();
        copyTask.setTodir(dir);
        FileSet src = new FileSet();
        src.setDir(p.getWorkspace().getLocal());
        src.setIncludes(artifacts);
        copyTask.addFileset(src);

        execTask(copyTask, listener);

        return true;
    }

    public Action getProjectAction(Project project) {
        return null;
    }

    public Descriptor<BuildStep> getDescriptor() {
        return DESCRIPTOR;
    }


    public static final Descriptor<BuildStep> DESCRIPTOR = new Descriptor<BuildStep>(ArtifactArchiver.class) {
        public String getDisplayName() {
            return "Archive the artifacts";
        }

        public String getHelpFile() {
            return "/help/project-config/archive-artifact.html";
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new ArtifactArchiver(req.getParameter("artifacts"));
        }
    };
}
