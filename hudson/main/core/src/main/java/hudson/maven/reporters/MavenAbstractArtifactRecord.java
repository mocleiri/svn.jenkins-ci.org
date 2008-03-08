package hudson.maven.reporters;

import hudson.maven.MavenEmbedder;
import hudson.maven.MavenUtil;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.model.TaskThread;
import hudson.model.TaskAction;
import hudson.security.Permission;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.embedder.MavenEmbedderException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * UI to redeploy artifacts after the fact.
 *
 * <p>
 * There are two types &mdash; one for the module, the other for the whole project.
 * The semantics specific to these cases are defined in subtypes.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class MavenAbstractArtifactRecord<T extends AbstractBuild<?,?>> extends TaskAction {
    /**
     * Gets the parent build object to which this record is registered.
     */
    public abstract T getBuild();

    public final String getIconFileName() {
        return "redo.gif";
    }

    public final String getDisplayName() {
        return "Redeploy Artifacts";
    }

    public final String getUrlName() {
        return "redeploy";
    }

    protected Permission getPermission() {
        return REDEPLOY;
    }

    /**
     * Performs a redeployment.
     */
    public final void doRedeploy(StaplerRequest req, StaplerResponse rsp,
                           @QueryParameter("id") final String id,
                           @QueryParameter("url") final String repositoryUrl,
                           @QueryParameter("uniqueVersion") final boolean uniqueVersion) throws ServletException, IOException {
        getBuild().checkPermission(REDEPLOY);

        new TaskThread(this) {
            protected void perform(TaskListener listener) throws Exception {
                MavenEmbedder embedder = MavenUtil.createEmbedder(listener, null);
                ArtifactRepositoryLayout layout =
                    (ArtifactRepositoryLayout) embedder.getContainer().lookup( ArtifactRepositoryLayout.ROLE,"default");
                ArtifactRepositoryFactory factory =
                    (ArtifactRepositoryFactory) embedder.lookup(ArtifactRepositoryFactory.ROLE);

                ArtifactRepository repository = factory.createDeploymentArtifactRepository(
                        id, repositoryUrl, layout, uniqueVersion);

                deploy(embedder,repository,listener);

                embedder.stop();
            }
        }.start();

        rsp.sendRedirect(".");
    }

    /**
     * Deploys the artifacts to the specified {@link ArtifactRepository}.
     *
     * @param embedder
     *      This component hosts all the Maven components we need to do the work.
     * @param deploymentRepository
     *      The remote repository to deploy to.
     * @param listener
     *      The status and error goes to this listener.
     */
    public abstract void deploy(MavenEmbedder embedder, ArtifactRepository deploymentRepository, TaskListener listener) throws MavenEmbedderException, IOException, ComponentLookupException, ArtifactDeploymentException;


    /**
     * Permission for redeploying artifacts.
     */
    public static final Permission REDEPLOY = AbstractProject.BUILD;
}
