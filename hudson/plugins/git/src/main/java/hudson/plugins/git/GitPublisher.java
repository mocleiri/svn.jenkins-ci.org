package hudson.plugins.git;

import hudson.Launcher;
import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.plugins.git.util.BuildData;
import hudson.remoting.VirtualChannel;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormFieldValidator;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class GitPublisher extends Publisher implements Serializable {

	public Descriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}

	public boolean needsToRunAfterFinalized() {
		return true;
	}

	public boolean perform(final AbstractBuild<?, ?> build,
			Launcher launcher, final BuildListener listener)
			throws InterruptedException {

		final SCM scm = build.getProject().getScm();

		if (!(scm instanceof GitSCM)) {
			return false;
		}

		final String projectName = build.getProject().getName();
		final int buildNumber = build.getNumber();
		final Result buildResult = build.getResult();
		
		boolean canPerform;
		try {
			canPerform = build.getProject().getWorkspace().act(
					new FileCallable<Boolean>() {
						public Boolean invoke(File workspace,
								VirtualChannel channel) throws IOException {

							GitSCM gitSCM = (GitSCM) scm;

							IGitAPI git = new GitAPI(
									GitSCM.DescriptorImpl.DESCRIPTOR
											.getGitExe(), build.getProject().getWorkspace(),
									listener);

							// We delete the old tag generated by the SCM plugin
							String buildnumber = "hudson-"
									+ projectName  + "-"
									+ buildNumber;
							git.deleteTag(buildnumber);

							// And add the success / fail state into the tag.
							buildnumber += "-" + buildResult.toString();

							git.tag(buildnumber, "Hudson Build #"
									+ buildNumber);

							if (gitSCM.getMergeOptions().doMerge()
									&& buildResult.isBetterOrEqualTo(
											Result.SUCCESS)) {
								listener.getLogger().println("Pushing tag " + buildnumber + " to " + gitSCM.getMergeOptions().getMergeTarget() + " branch of origin repository");
								git.push("HEAD:" + gitSCM.getMergeOptions().getMergeTarget());
							} else {
								listener.getLogger().println("Pushing tag " + buildnumber + " to origin repository");
								git.push(null);
							}

							return true;
						}
					});
		} catch (IOException e) {
			listener.error("Failed to push tags to origin repository: " + e.getMessage());
			return false;
		}
		return canPerform;
	}

	public static final Descriptor<Publisher> DESCRIPTOR = new DescriptorImpl();

	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			super(GitPublisher.class);
		}

		public String getDisplayName() {
			return "Push GIT tags back to origin repository";
		}

		public String getHelpFile() {
			return "/plugin/git/gitPublisher.html";
		}

		/**
		 * Performs on-the-fly validation on the file mask wildcard.
		 */
		public void doCheck(StaplerRequest req, StaplerResponse rsp)
				throws IOException, ServletException {
			new FormFieldValidator.WorkspaceFileMask(req, rsp).process();
		}

		public GitPublisher newInstance(StaplerRequest req, JSONObject formData)
				throws FormException {
			return new GitPublisher();
		}

		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}

}
