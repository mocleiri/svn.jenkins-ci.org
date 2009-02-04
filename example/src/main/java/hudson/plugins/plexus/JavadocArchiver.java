package hudson.plugins.plexus;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Project;
import hudson.model.ProminentProjectAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.Publisher;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Saves Javadoc for the project and publish them.
 * 
 * @plexus.component role="hudson.tasks.Publisher" role-hint="javadoc"
 */
public class JavadocArchiver extends Publisher {
	/**
	 * Path to the Javadoc directory in the workspace.
	 */
	private String javadocDir;
	public void setJavadocDir(String javadocDir) {
		this.javadocDir = javadocDir;
	}

	public void setKeepAll(boolean keepAll) {
		this.keepAll = keepAll;
	}

	/**
	 * If true, retain javadoc for all the successful builds.
	 */
	private boolean keepAll;

	/**
	 * All injected components need to be marked as transient, so xstream doesn't
	 * serialize them
	 * 
	 * @plexus.requirement role="hudson.model.Descriptor" role-hint="javadoc"
	 */
	private transient Descriptor<Publisher> descriptor;
	

	public String getJavadocDir() {
		return javadocDir;
	}

	public boolean isKeepAll() {
		return keepAll;
	}

	/**
	 * Gets the directory where the Javadoc is stored for the given project.
	 */
	private static File getJavadocDir(AbstractItem project) {
		return new File(project.getRootDir(), "javadoc");
	}

	/**
	 * Gets the directory where the Javadoc is stored for the given build.
	 */
	private static File getJavadocDir(Run run) {
		return new File(run.getRootDir(), "javadoc");
	}

	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException {
		listener.getLogger().println("Publishing Javadoc");

		FilePath javadoc = build.getParent().getWorkspace().child(javadocDir);
		FilePath target = new FilePath(keepAll ? getJavadocDir(build)
				: getJavadocDir(build.getProject()));

		try {
			if (javadoc.copyRecursiveTo("**/*", target) == 0) {
				if (build.getResult().isBetterOrEqualTo(Result.UNSTABLE)) {
					// If the build failed, don't complain that there was no
					// javadoc.
					// The build probably didn't even get to the point where it
					// produces javadoc.
					listener.error("No javadoc found in " + javadoc);
				}
				build.setResult(Result.FAILURE);
				return true;
			}
		} catch (IOException e) {
			Util.displayIOException(e, listener);
			e.printStackTrace(listener.fatalError(
					"Unable to copy javadoc from %s to %s", javadoc, target));
			build.setResult(Result.FAILURE);
			return true;
		}

		// add build action, if javadoc is recorded for each build
		if (keepAll)
			build.addAction(new JavadocBuildAction(build));

		return true;
	}

	public Action getProjectAction(Project project) {
		return new JavadocAction(project);
	}

	public Descriptor<Publisher> getDescriptor() {
		return descriptor;
	}

	protected static abstract class BaseJavadocAction implements Action {
		public String getUrlName() {
			return "javadoc";
		}

		public String getDisplayName() {
			if (new File(dir(), "help-doc.html").exists())
				return "Javadoc";
			else
				return "Document";
		}

		public String getIconFileName() {
			if (dir().exists())
				return "help.gif";
			else
				// hide it since we don't have javadoc yet.
				return null;
		}

		public void doDynamic(StaplerRequest req, StaplerResponse rsp)
				throws IOException, ServletException, InterruptedException {
			new DirectoryBrowserSupport(this, getTitle()).serveFile(req, rsp,
					new FilePath(dir()), "help.gif", false);
		}

		protected abstract String getTitle();

		protected abstract File dir();
	}

	public static class JavadocAction extends BaseJavadocAction implements
			ProminentProjectAction {
		private final AbstractItem project;

		public JavadocAction(AbstractItem project) {
			this.project = project;
		}

		protected File dir() {
			// Would like to change AbstractItem to AbstractProject, but is
			// that a backwards compatible change?
			if (project instanceof AbstractProject) {
				AbstractProject abstractProject = (AbstractProject) project;

				Run run = abstractProject.getLastSuccessfulBuild();
				if (run != null) {
					File javadocDir = getJavadocDir(run);

					if (javadocDir.exists())
						return javadocDir;
				}
			}

			return getJavadocDir(project);
		}

		protected String getTitle() {
			return project.getDisplayName() + " javadoc";
		}
	}

	public static class JavadocBuildAction extends BaseJavadocAction {
		private final AbstractBuild<?, ?> build;

		public JavadocBuildAction(AbstractBuild<?, ?> build) {
			this.build = build;
		}

		protected String getTitle() {
			return build.getDisplayName() + " javadoc";
		}

		protected File dir() {
			return getJavadocDir(build);
		}
	}
}
