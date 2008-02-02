package hudson.tasks;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.maven.AbstractMavenProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.util.FormFieldValidator;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

import net.sf.json.JSONObject;

/**
 * Copies the artifacts into an archive directory.
 *
 * @author Kohsuke Kawaguchi
 */
public class ArtifactArchiver extends Publisher {

    /**
     * Comma- or space-separated list of patterns of files/directories to be archived.
     */
    private final String artifacts;

    /**
     * Possibly null 'excludes' pattern as in Ant.
     */
    private final String excludes;

    /**
     * Just keep the last successful artifact set, no more.
     */
    private final boolean latestOnly;

    @DataBoundConstructor
    public ArtifactArchiver(String artifacts, String excludes, boolean latestOnly) {
        this.artifacts = artifacts.trim();
        this.excludes = Util.fixEmptyAndTrim(excludes);
        this.latestOnly = latestOnly;
    }

    public String getArtifacts() {
        return artifacts;
    }

    public String getExcludes() {
        return excludes;
    }

    public boolean isLatestOnly() {
        return latestOnly;
    }

    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        AbstractProject<?,?> p = build.getProject();

        File dir = build.getArtifactsDir();
        dir.mkdirs();

        try {
            if(p.getWorkspace().copyRecursiveTo(artifacts,excludes,new FilePath(dir))==0) {
                listener.error("No artifacts found that match the file pattern \""+artifacts+"\". Configuration error?");
                build.setResult(Result.FAILURE);
                return true;
            }
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            e.printStackTrace(listener.error("Failed to archive artifacts: "+artifacts));
            return true;
        }

        if(latestOnly) {
            AbstractBuild<?,?> b = p.getLastSuccessfulBuild();
            if(b!=null) {
                while(true) {
                    b = b.getPreviousBuild();
                    if(b==null)     break;

                    // remove old artifacts
                    File ad = b.getArtifactsDir();
                    if(ad.exists()) {
                        listener.getLogger().println("Deleting old artifacts from "+b.getDisplayName());
                        try {
                            Util.deleteRecursive(ad);
                        } catch (IOException e) {
                            e.printStackTrace(listener.error(e.getMessage()));
                        }
                    }
                }
            }
        }

        return true;
    }

    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }


    public static final Descriptor<Publisher> DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
            super(ArtifactArchiver.class);
        }

        public String getDisplayName() {
            return "Archive the artifacts";
        }

        public String getHelpFile() {
            return "/help/project-config/archive-artifact.html";
        }

        /**
         * Performs on-the-fly validation on the file mask wildcard.
         */
        public void doCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator.WorkspaceFileMask(req,rsp).process();
        }

        public ArtifactArchiver newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(ArtifactArchiver.class,formData);
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            // for Maven, this happens automatically.
            // TODO: we should still consider enabling this for additional controls?
            return !AbstractMavenProject.class.isAssignableFrom(jobType);
        }
    }
}
