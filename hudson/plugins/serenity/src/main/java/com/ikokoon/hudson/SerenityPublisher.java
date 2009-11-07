package com.ikokoon.hudson;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.IOException;
import java.io.PrintStream;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.ikokoon.IConstants;

/**
 * This class runs at the end of the build, called by Hudson. The purpose is to copy the database file from the output directory for the reports
 * directory where the build action can present the data to the front end.
 * 
 * @author Michael Couck
 * @since 12.07.09
 * @version 01.00
 */
@SuppressWarnings("unchecked")
public class SerenityPublisher extends Recorder {

	protected static final Logger logger = Logger.getLogger(SerenityPublisher.class);
	/** The description for Hudson. */
	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	/** The pattern for the object database file. */
	private String coverageReportPattern;

	@DataBoundConstructor
	public SerenityPublisher(String coverageReportPattern) {
		logger.info("SerenityPublisher:" + coverageReportPattern);
		this.coverageReportPattern = coverageReportPattern;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener buildListener) throws InterruptedException, IOException {
		logger.info("SerenityPublisher:perform");
		PrintStream consolePrintStream = buildListener.getLogger();
		consolePrintStream.println("Looking for database file : " + coverageReportPattern);
		if (coverageReportPattern == null) {
			consolePrintStream.println("Skipping coverage reports as coverageReportPattern is null");
			return false;
		}
		if (!Result.SUCCESS.equals(build.getResult())) {
			buildListener.getLogger().println("Build was not successful... but will still publish the report");
			// return true;
		}

		buildListener.getLogger().println("Publishing Serenity reports...");

		FilePath[] reports = new FilePath[0];
		final FilePath moduleRoot = build.getWorkspace();
		try {
			reports = moduleRoot.list(coverageReportPattern);
		} catch (IOException e) {
			Util.displayIOException(e, buildListener);
			e.printStackTrace(buildListener.fatalError("Unable to find Serenity results"));
			build.setResult(Result.FAILURE);
		}

		if (reports.length == 0) {
			buildListener.getLogger().println("No coverage results were found using the pattern '" + coverageReportPattern + "'.");
			build.setResult(Result.FAILURE);
			return true;
		}

		if (reports.length > 1) {
			buildListener.getLogger().println("Serenity publisher found more than one report that match the pattern. Impossible, but true.");
			build.setResult(Result.FAILURE);
			return true;
		}

		FilePath buildTarget = new FilePath(build.getRootDir());
		FilePath singleReport = reports[0];
		FilePath targetPath = new FilePath(buildTarget, IConstants.DATABASE_FILE);
		try {
			buildListener.getLogger().println(
					"Publishing serenity db from : " + singleReport.toURI().getRawPath() + ", to : " + targetPath.toURI().getRawPath());
			singleReport.copyTo(targetPath);
		} catch (IOException e) {
			Util.displayIOException(e, buildListener);
			e.printStackTrace(buildListener.fatalError("Unable to copy Serenity database file from : " + singleReport + ", to : " + buildTarget));
			build.setResult(Result.FAILURE);
		}

		buildListener.getLogger().println("Accessing Serenity results...");
		ISerenityResult coverageResult = new SerenityResult(build);
		SerenityBuildAction buildAction = new SerenityBuildAction(build, coverageResult);
		build.getActions().add(buildAction);

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public Action getProjectAction(hudson.model.Project project) {
		logger.info("SerenityPublisher:getProjectAction");
		return new SerenityProjectAction(project);
	}

	public void setCoverageReportPattern(String coverageReportPattern) {
		logger.info("SerenityPublisher:setCoverageReportPattern");
		this.coverageReportPattern = coverageReportPattern;
	}

	public String getCoverageReportPattern() {
		logger.info("SerenityPublisher:getCoverageReportPattern");
		return coverageReportPattern;
	}

	/**
	 * Descriptor for {@link SerenityPublisher}. Used as a singleton. The class is marked as public so that it can be accessed from views.
	 * <p/>
	 * <p/>
	 * See <tt>views/hudson/plugins/coverage/CoveragePublisher/*.jelly</tt> for the actual HTML fragment for the configuration screen.
	 */
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		/**
		 * Constructs a new DescriptorImpl.
		 */
		DescriptorImpl() {
			super(SerenityPublisher.class);
			logger.info("SerenityPublisher:DescriptorImpl");
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			logger.info("SerenityPublisher:getDisplayName");
			return "Publish Serenity Report";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			logger.info("SerenityPublisher:configure");
			req.bindParameters(this, "serenity.");
			save();
			return super.configure(req, json);
		}

		/**
		 * Creates a new instance of {@link SerenityPublisher} from a submitted form.
		 */
		@Override
		public SerenityPublisher newInstance(StaplerRequest req, JSONObject json) throws FormException {
			logger.info("SerenityPublisher:newInstance");
			SerenityPublisher instance = req.bindParameters(SerenityPublisher.class, "serenity.");
			return instance;
		}
	}

	public BuildStepMonitor getRequiredMonitorService() {
		logger.info("SerenityPublisher:getRequiredMonitorService");
		return BuildStepMonitor.STEP;
	}
}