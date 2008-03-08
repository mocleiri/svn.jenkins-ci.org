package hudson.maven.reporters;

import hudson.maven.AggregatableAction;
import hudson.maven.MavenAggregatedReport;
import hudson.maven.MavenBuild;
import hudson.maven.MavenEmbedder;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.MavenUtil;
import hudson.model.Action;
import hudson.model.TaskListener;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * {@link Action} that remembers {@link MavenArtifact artifact}s that are built.
 *
 * Dfines the methods and UIs to do (delayed) deployment and installation. 
 *
 * @author Kohsuke Kawaguchi
 * @see MavenArtifactArchiver
 */
public class MavenArtifactRecord extends MavenAbstractArtifactRecord<MavenBuild> implements AggregatableAction {
    /**
     * The build to which this record belongs.
     */
    public final MavenBuild parent;

    /**
     * POM artifact.
     */
    public final MavenArtifact pomArtifact;

    /**
     * The main artifact (like jar or war, but could be anything.)
     *
     * If this is a POM module, the main artifact contains the same value as {@link #pomArtifact}.
     */
    public final MavenArtifact mainArtifact;

    /**
     * Attached artifacts. Can be empty but never null.
     */
    public final List<MavenArtifact> attachedArtifacts;

    public MavenArtifactRecord(MavenBuild parent, MavenArtifact pomArtifact, MavenArtifact mainArtifact, List<MavenArtifact> attachedArtifacts) {
        assert parent!=null;
        assert pomArtifact!=null;
        assert attachedArtifacts!=null;
        if(mainArtifact==null)  mainArtifact=pomArtifact;

        this.parent = parent;
        this.pomArtifact = pomArtifact;
        this.mainArtifact = mainArtifact;
        this.attachedArtifacts = attachedArtifacts;
    }

    public MavenBuild getParent() {
        return parent;
    }

    public boolean isPOM() {
        return mainArtifact.isPOM();
    }

    public MavenAggregatedReport createAggregatedAction(MavenModuleSetBuild build, Map<MavenModule, List<MavenBuild>> moduleBuilds) {
        return new MavenAggregatedArtifactRecord(build);
    }

    @Override
    public void deploy(MavenEmbedder embedder, ArtifactRepository deploymentRepository, TaskListener listener) throws MavenEmbedderException, IOException, ComponentLookupException, ArtifactDeploymentException {
        ArtifactDeployer deployer = (ArtifactDeployer) embedder.lookup(ArtifactDeployer.ROLE);
        ArtifactFactory factory = (ArtifactFactory) embedder.lookup(ArtifactFactory.ROLE);
        PrintStream logger = listener.getLogger();

        Artifact main = mainArtifact.toArtifact(factory,parent);
        if(!isPOM())
            main.addMetadata(new ProjectArtifactMetadata(main,pomArtifact.getFile(parent)));

        // deploy the main artifact. This also deploys the POM
        logger.println(Messages.MavenArtifact_DeployingMainArtifact(main.getFile().getName()));
        deployer.deploy(main.getFile(),main,deploymentRepository,embedder.getLocalRepository());

        for (MavenArtifact aa : attachedArtifacts) {
            logger.println(Messages.MavenArtifact_DeployingAttachedArtifact(main.getFile().getName()));
            Artifact a = aa.toArtifact(factory, parent);
            deployer.deploy(a.getFile(),a,deploymentRepository,embedder.getLocalRepository());
        }
    }

    /**
     * Installs the artifact to the local Maven repository.
     */
    public void install(TaskListener listener) throws MavenEmbedderException, IOException, ComponentLookupException, ArtifactInstallationException {
        MavenEmbedder embedder = MavenUtil.createEmbedder(listener,null);
        ArtifactInstaller installer = (ArtifactInstaller) embedder.lookup(ArtifactInstaller.class.getName());
        ArtifactFactory factory = (ArtifactFactory) embedder.lookup(ArtifactFactory.class.getName());

        Artifact main = mainArtifact.toArtifact(factory,parent);
        if(!isPOM())
            main.addMetadata(new ProjectArtifactMetadata(main,pomArtifact.getFile(parent)));
        installer.install(mainArtifact.getFile(parent),main,embedder.getLocalRepository());

        for (MavenArtifact aa : attachedArtifacts)
            installer.install(aa.getFile(parent),aa.toArtifact(factory,parent),embedder.getLocalRepository());

        embedder.stop();
    }

    public void recordFingerprints() throws IOException {
        // record fingerprints
        if(mainArtifact!=null)
            mainArtifact.recordFingerprint(parent);
        for (MavenArtifact a : attachedArtifacts)
            a.recordFingerprint(parent);
    }
}
