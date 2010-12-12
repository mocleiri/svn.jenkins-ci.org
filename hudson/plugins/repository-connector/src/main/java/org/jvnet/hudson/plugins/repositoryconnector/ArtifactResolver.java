package org.jvnet.hudson.plugins.repositoryconnector;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherResult;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.ArtifactResolutionException;

/**
 * This builder allows to resolve artifacts from a repository and copy it to any
 * location.
 * 
 * @author domi
 */
public class ArtifactResolver extends Builder implements Serializable {

	private static final long serialVersionUID = 1L;

	static Logger log = Logger.getLogger(ArtifactResolver.class.getName());

	private static final String DEFAULT_TARGET = "target";

	public String targetDirectory;
	public List<Artifact> artifacts;
	public boolean failOnError = true;
	public boolean enableRepoLogging = true;
	public final String snapshotUpdatePolicy;
	public final String releaseUpdatePolicy;
	public final String snapshotChecksumPolicy;
	public final String releaseChecksumPolicy;

	@DataBoundConstructor
	public ArtifactResolver(String targetDirectory, List<Artifact> artifacts, boolean failOnError, boolean enableRepoLogging, String snapshotUpdatePolicy,
			String snapshotChecksumPolicy, String releaseUpdatePolicy, String releaseChecksumPolicy) {
		this.artifacts = artifacts != null ? artifacts : new ArrayList<Artifact>();
		this.targetDirectory = StringUtils.isBlank(targetDirectory) ? DEFAULT_TARGET : targetDirectory;
		this.failOnError = failOnError;
		this.enableRepoLogging = enableRepoLogging;
		this.releaseUpdatePolicy = releaseUpdatePolicy;
		this.releaseChecksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_WARN;
		this.snapshotUpdatePolicy = snapshotUpdatePolicy;
		this.snapshotChecksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_WARN;
	}

	public String getTargetDirectory() {
		return StringUtils.isBlank(targetDirectory) ? DEFAULT_TARGET : targetDirectory;
	}

	public boolean failOnError() {
		return failOnError;
	}

	public boolean enableRepoLogging() {
		return enableRepoLogging;
	}

	/**
	 * gets the artifacts
	 * 
	 * @return
	 */
	public List<Artifact> getArtifacts() {
		return artifacts;
	}

	File getLocalRepoPath() {
		String localRepositoryLocation = getDescriptor().getLocalRepository();
		// default to local repo within java temp dir
		if (StringUtils.isBlank(localRepositoryLocation)) {
			localRepositoryLocation = System.getProperty("java.io.tmpdir") + "/repositoryconnector-repo";
		}
		return new File(localRepositoryLocation);
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {

		final PrintStream logger = listener.getLogger();
		final Collection<Repository> repositories = getDescriptor().getRepos();

		File localRepo = getLocalRepoPath();
		if (!localRepo.exists()) {
			log.info("create local repo directory: " + localRepo.getAbsolutePath());
			localRepo.mkdirs();
		}

		boolean failed = download(build, logger, repositories, localRepo);

		if (failed && failOnError) {
			return false;
		}
		return true;
	}

	private boolean download(AbstractBuild<?, ?> build, final PrintStream logger, final Collection<Repository> repositories, File localRepository) {
		boolean hasError = false;

		Aether aether = new Aether(repositories, localRepository, logger, enableRepoLogging, snapshotUpdatePolicy, snapshotChecksumPolicy, releaseUpdatePolicy,
				releaseChecksumPolicy);

		for (Artifact artifact : artifacts) {

			try {

				String version = artifact.getVersion();

				AetherResult result = aether.resolve(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension(),
						version);

				List<File> resolvedFiles = result.getResolvedFiles();
				for (File file : resolvedFiles) {

					String fileName = StringUtils.isBlank(artifact.getTargetFileName()) ? file.getName() : artifact.getTargetFileName();
					FilePath source = new FilePath(file);
					FilePath target = new FilePath(build.getWorkspace(), getTargetDirectory() + "/" + fileName);
					boolean wasDeleted = target.delete();
					if (wasDeleted) {
						logger.println("deleted " + target.toURI());
					}
					logger.println("copy " + file + " to " + target.toURI());
					source.copyTo(target);

				}

			} catch (DependencyCollectionException e) {
				hasError = logError("failed collecting dependency info for " + artifact, logger, e);
			} catch (ArtifactResolutionException e) {
				hasError = logError("failed to resolve artifact for " + artifact, logger, e);
			} catch (IOException e) {
				hasError = logError("failed collecting dependency info for " + artifact, logger, e);
			} catch (InterruptedException e) {
				hasError = logError("interuppted failed to copy file for " + artifact, logger, e);
			}

		}
		return hasError;
	}

	private boolean logError(String msg, final PrintStream logger, Exception e) {
		log.log(Level.SEVERE, msg, e);
		logger.println(msg);
		e.printStackTrace(logger);
		return true;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		private static final List<Repository> DEFAULT_REPOS = new ArrayList<Repository>();
		static {
			DEFAULT_REPOS.add(new Repository("central", "default", "http://repo1.maven.org/maven2", null, null, false));
		}

		private Set<Repository> repos = new HashSet<Repository>();

		private String localRepository = "";

		public DescriptorImpl() {
			load();
			if (repos.isEmpty()) {
				repos.addAll(DEFAULT_REPOS);
			}
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "Artifact Resolver (Repository Connector)";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

			localRepository = formData.getString("localRepository");

			repos.clear();

			final Object repoJson = formData.get("repos");
			if (repoJson instanceof JSONArray) {
				final JSONArray jsonArray = (JSONArray) repoJson;
				for (Object object : jsonArray) {
					addRepo(req, (JSONObject) object);
				}
			} else {
				// there might be only a single repo config
				addRepo(req, (JSONObject) repoJson);
			}

			if (repos.isEmpty()) {
				repos.addAll(DEFAULT_REPOS);
			}

			save();
			return true;
		}

		/**
		 * adds a dynamic permission configuration with the data extracted form
		 * the jsonObject.
		 * 
		 */
		private void addRepo(StaplerRequest req, JSONObject jsonObject) {
			final Repository repo = req.bindJSON(Repository.class, jsonObject);
			if (repo != null) {
				if (!StringUtils.isBlank(repo.getUrl())) {
					repos.add(repo);
				}
			}
		}

		public String getLocalRepository() {
			return localRepository;
		}

		public Set<Repository> getRepos() {
			return repos;
		}

	}
}
