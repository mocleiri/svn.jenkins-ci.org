package hudson.maven;

import hudson.Launcher;
import hudson.maven.reporters.MavenAbstractArtifactRecord;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.embedder.MavenEmbedderException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * {@link Publisher} for {@link MavenModuleSetBuild} to deploy artifacts
 * after a build is fully succeeded. 
 *
 * @author Kohsuke Kawaguchi
 * @since 1.191
 */
public class RedeployPublisher extends Publisher {
    private final String id;
    private final String repositoryUrl;
    private final boolean uniqueVersion;

    @DataBoundConstructor
    public RedeployPublisher(String id, String repositoryUrl, boolean uniqueVersion) {
        this.id = id;
        this.repositoryUrl = repositoryUrl;
        this.uniqueVersion = uniqueVersion;
    }

    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        MavenAbstractArtifactRecord mar = getAction(build);
        if(mar==null) {
            listener.getLogger().println("No artifacts are recorded. Is this a Maven project?");
            build.setResult(Result.FAILURE);
            return true;
        }

        try {
            MavenEmbedder embedder = MavenUtil.createEmbedder(listener, null);
            ArtifactRepositoryLayout layout =
                (ArtifactRepositoryLayout) embedder.getContainer().lookup( ArtifactRepositoryLayout.ROLE,"default");
            ArtifactRepositoryFactory factory =
                (ArtifactRepositoryFactory) embedder.lookup(ArtifactRepositoryFactory.ROLE);

            ArtifactRepository repository = factory.createDeploymentArtifactRepository(
                    id, repositoryUrl, layout, uniqueVersion);

            mar.deploy(embedder,repository,listener);

            embedder.stop();
            return true;
        } catch (MavenEmbedderException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        } catch (ComponentLookupException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        } catch (ArtifactDeploymentException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        }
        // failed
        build.setResult(Result.FAILURE);
        return true;
    }

    /**
     * Obtains the {@link MavenAbstractArtifactRecord} that we'll work on.
     * <p>
     * This allows promoted-builds plugin to reuse the code for delayed deployment. 
     */
    protected MavenAbstractArtifactRecord getAction(AbstractBuild<?, ?> build) {
        return build.getAction(MavenAbstractArtifactRecord.class);
    }

    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
            super(RedeployPublisher.class);
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return jobType==MavenModuleSet.class;
        }

        public String getHelpFile() {
            return "/help/maven/redeploy.html";
        }

        public RedeployPublisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(RedeployPublisher.class,formData);
        }

        public String getDisplayName() {
            return "Deploy artifacts to Maven repository";
        }
    }
}
