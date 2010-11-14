package org.jvnet.hudson.plugins.repositoryconnector;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.VariableResolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.SubArtifact;

/**
 * This builder allows to resolve artifacts from a repository and copy it to any
 * location.
 * 
 * @author domi
 */
public class ArtifactDeployer extends Notifier implements Serializable {

	private static final long serialVersionUID = 1L;

	static Logger log = Logger.getLogger(ArtifactDeployer.class.getName());

	public boolean enableRepoLogging = true;
	public final String repoId;
	public final String snapshotRepoId;
	public List<Artifact> artifacts;

	@DataBoundConstructor
	public ArtifactDeployer(List<Artifact> artifacts, String repoId, String snapshotRepoId, boolean enableRepoLogging) {
		this.enableRepoLogging = enableRepoLogging;
		this.artifacts = artifacts != null ? artifacts : new ArrayList<Artifact>();
		this.repoId = repoId;
		this.snapshotRepoId = snapshotRepoId;
	}

	public boolean enableRepoLogging() {
		return enableRepoLogging;
	}

	public Set<Repository> getRepos() {
		return getResolverDescriptor().getRepos();
	}

	private Repository getRepoById(String id) {
		for (Repository repo : getRepos()) {
			if (repo.getId().equals(id)) {
				return repo;
			}
		}
		return null;
	}

	/*
	 * @see hudson.tasks.BuildStep#getRequiredMonitorService()
	 */
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {

		final PrintStream logger = listener.getLogger();

		final VariableResolver<String> variableResolver = build.getBuildVariableResolver();

		try {
			for (Artifact a : artifacts) {

				final String version = resolveVariable(variableResolver, a.getVersion());
				final String classifier = resolveVariable(variableResolver, a.getClassifier());
				final String artifactId = resolveVariable(variableResolver, a.getArtifactId());
				final String groupId = resolveVariable(variableResolver, a.getGroupId());
				final String extension = resolveVariable(variableResolver, a.getExtension());

				String tmpRepoId = version.contains("SNAPSHOT") ? snapshotRepoId : repoId;
				final Repository repo = getRepoById(tmpRepoId);
				Aether aether = new Aether(repo, new File(getResolverDescriptor().getLocalRepository()), logger, enableRepoLogging);

				String fileName = a.getTargetFileName();
				FilePath source = new FilePath(build.getWorkspace(), fileName);
				final File targetFile = File.createTempFile(fileName, null);
				FilePath target = new FilePath(targetFile);

				logger.println("copy source " + source.toURI() + " to master " + target.toURI());
				source.copyTo(target);

				org.sonatype.aether.artifact.Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
				logger.println("deploy artifact: " + artifactId);
				artifact = artifact.setFile(targetFile);
				org.sonatype.aether.artifact.Artifact pom = new SubArtifact(artifact, null, "pom");
				final File tmpPom = getTempPom(a);
				pom = pom.setFile(tmpPom);

				aether.install(artifact, pom);
				aether.deploy(artifact, pom, repo.getUrl());

				// clean the resources
				targetFile.delete();
				tmpPom.delete();
				aether = null;
			}
		} catch (DeploymentException e) {
			logger.println("ERROR: possible causes: in case of a SNAPSHOT deployment: does your remote repository allow SNAPSHOT deployments?, in case of a release dpeloyment: is this version of the artifact already deployed then does your repository allow updating artifacts?");
			return logError("DeploymentException: ", logger, e);
		} catch (IOException e) {
			return logError("IOException: ", logger, e);
		} catch (InterruptedException e) {
			return logError("InterruptedException: ", logger, e);
		} catch (Exception e) {
			return logError("Exception: ", logger, e);
		}
		return true;
	}

	private String resolveVariable(VariableResolver<String> variableResolver, String potentalVaraible) {
		String value = potentalVaraible;
		if (potentalVaraible.startsWith("${") && potentalVaraible.endsWith("}")) {
			value = potentalVaraible.substring(2, potentalVaraible.length() - 1);
			value = variableResolver.resolve(value);
			log.log(Level.FINE, "resolve " + potentalVaraible + " to " + value);
			System.out.println("resolve " + potentalVaraible + " to " + value);
		}
		return value;
	}

	private boolean logError(String msg, final PrintStream logger, Exception e) {
		log.log(Level.SEVERE, msg, e);
		logger.println(msg);
		e.printStackTrace(logger);
		return false;
	}

	private ArtifactResolver.DescriptorImpl getResolverDescriptor() {
		final ArtifactResolver.DescriptorImpl resolverDescriptor = (ArtifactResolver.DescriptorImpl) Hudson.getInstance().getBuilder("ArtifactResolver");
		return resolverDescriptor;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			load();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "Artifact Deployer";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

			save();
			return true;
		}

	}

	private File getTempPom(Artifact artifact) {
		File tmpPom = null;
		try {
			final String preparedPom = this.preparedPom(artifact);
			tmpPom = File.createTempFile("pom" + artifact.getArtifactId(), ".xml");
			FileUtils.writeStringToFile(tmpPom, preparedPom);
		} catch (IOException e) {
			log.log(Level.SEVERE, "not able to create temporal pom: " + e.getMessage());
		}
		return tmpPom;
	}

	private String preparedPom(Artifact artifact) {
		String pomContent = null;
		try {
			final InputStream stream = this.getClass().getResourceAsStream("/org/jvnet/hudson/plugins/repositoryconnector/ArtifactDeployer/pom.tmpl");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
			StringBuilder stringBuilder = new StringBuilder();
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}
			bufferedReader.close();
			pomContent = stringBuilder.toString();
			pomContent.replace("ARTIFACTID", artifact.getArtifactId());
			pomContent.replace("GROUPID", artifact.getArtifactId());
			pomContent.replace("VERSION", artifact.getArtifactId());
			pomContent.replace("PACKAGING", artifact.getArtifactId());
			// FIXME what about classifier?
			// pomContent.replace("PACKAGING", artifact.getArtifactId());
		} catch (Exception e) {
			log.log(Level.SEVERE, "not able to create temporal pom: " + e.getMessage());
		}
		return pomContent;
	}
}
