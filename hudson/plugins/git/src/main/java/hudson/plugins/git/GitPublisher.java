package hudson.plugins.git;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormFieldValidator;

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

	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException {

		SCM scm = build.getProject().getScm();
		
		if( !(scm instanceof GitSCM) )
		{
			
			return false;
		}
		GitSCM gitSCM = (GitSCM)scm;
		
		IGitAPI git = new GitAPI(GitSCM.DescriptorImpl.DESCRIPTOR.getGitExe(),
				launcher, build.getProject().getWorkspace(), listener);

		// We delete the old tag generated by the SCM plugin
		String buildnumber = "hudson-" + build.getProject().getName() + "-"
				+ build.getNumber();
		git.deleteTag(buildnumber);

		// And add the success / fail state into the tag.
		buildnumber += "-" + build.getResult().toString();

		git.tag(buildnumber, "Hudson Build #" + build.getNumber());

		if( gitSCM.getDoMerge() && build.getResult().isBetterOrEqualTo(Result.SUCCESS))
		{
			git.push("HEAD:"+gitSCM.getMergeTarget());
		}
		else
		{
			git.push(null);
		}
		
		return true;
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
