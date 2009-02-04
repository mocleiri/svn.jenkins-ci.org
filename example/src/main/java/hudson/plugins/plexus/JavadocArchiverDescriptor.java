package hudson.plugins.plexus;

import hudson.maven.AbstractMavenProject;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;
import hudson.plexus.PlexusUtil;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormFieldValidator;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @plexus.component role="hudson.model.Descriptor" role-hint="javadoc"
 */
public class JavadocArchiverDescriptor extends BuildStepDescriptor<Publisher> {

	public JavadocArchiverDescriptor() {
		super(JavadocArchiver.class);
	}

	public String getDisplayName() {
		return "Publish Javadoc";
	}

	public Publisher newInstance(StaplerRequest req, JSONObject formData)
			throws FormException {
		Publisher result = PlexusUtil.lookupComponent(Publisher.class, "plexus",
				"javadoc", null);

		// this only binds simple parameters ! Recent Hudson has bindJSON(
		// Object bean, JSONObject src )
		// which is better
		req.bindParameters(result, "plexus.");

		return result;
	}

	/**
	 * Performs on-the-fly validation on the file mask wildcard.
	 */
	public void doCheck(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException {
		new FormFieldValidator.WorkspaceDirectory(req, rsp).process();
	}

	public boolean isApplicable(Class<? extends AbstractProject> jobType) {
		// for Maven, javadoc archiving kicks in automatically
		return !AbstractMavenProject.class.isAssignableFrom(jobType);
	}

}