/*
 * The MIT License
 * 
 * Copyright (c) 2011, Harald Wellmann
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.refit.jenkins;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.bind.JAXBException;

import org.kohsuke.stapler.DataBoundConstructor;

import com.googlecode.refit.jenkins.jaxb.Summary;

/**
 * A Publisher plugin for Jenkins which publishes the Fit test reports from the latest build
 * at a fixed URL, similar to the Javadoc archiver.
 * <p>
 * The plugin works both for Maven and freestyle builds. The only requirement is that Fit reports
 * were produced in a previous build step, e.g. by running a FitSuite under JUnit or by running
 * the refit-maven-plugin in a Maven build.
 * <p>
 * The report directory (relative to this project's workspace folder) must be defined on the
 * Jenkins project configuration page.
 * 
 * @author Harald Wellmann
 */
@SuppressWarnings("unchecked")
public class ReFitArchiver extends Recorder {

    private String reportPath;
    

    @DataBoundConstructor
    public ReFitArchiver(String reportPath) {
        this.reportPath = reportPath;
    }

    public String getReportPath() {
        return reportPath;
    }
    
    
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * Copies the Fit reports of a given build to an archive child folder. The reports of the
     * last successful build are made available via a project action.
     * <p>
     * The build result may change from stable to unstable when there are Fit test failures
     * or exceptions.  
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        
        // Early exit when the build has failed. There's little chance of finding any reports
        // in this case anyway.
        if (build.getResult().isWorseOrEqualTo(Result.FAILURE)) {
            logger.println("[reFit] not collecting results due to build failure");
            return true;
        }

        // Fail the build if the report directory does not exist. This is most probably caused
        // by incorrect input on the configuration page.
        FilePath report = build.getWorkspace().child(reportPath);
        if (! report.exists()) {
            logger.println("[reFit] report directory " + report + " does not exist");
            build.setResult(Result.FAILURE);
            return true;
        }
        
        // Read the reFit summary
        Summary summary = getSummary(report);
        int numTests = summary.getNumTests();
        ReFitTestResult testResult = new ReFitTestResult(build, summary);
        
        // Create an Action with containing the test results counts to be persisted for this
        // build. This will be used for generating a trend graph.
        ReFitBuildAction action = new ReFitBuildAction(testResult);
        build.getActions().add(action);
                
        logger.println("[reFit] found " + numTests + " Fit tests");
        if (! summary.isPassed()) {
            build.setResult(Result.UNSTABLE);
        }
        
        // Archive the entire reFit output folder.
        FilePath archive = new FilePath(ReFitPlugin.getBuildReportFolder(build));
        report.copyRecursiveTo(archive);

        return true;
    }

    /**
     * Unmarshals the reFit summary from the report directory.
     * @param reportDir  directory containing reFit reports
     * @return JAXB object representing the test summary
     * @throws IOException
     */
    public Summary getSummary(FilePath reportDir) throws IOException {
        InputStream is = reportDir.child(ReportReader.FIT_REPORT_XML).read();
        try {
            Summary summary = new ReportReader().readXml(is);
            return summary;
        }
        catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    /**
     * Returns the project-level actions of this plugin. This is required to make the actions
     * available to the user via relative URLs.
     * <p>
     * There is an action for displaying the latest test summary, and another one for generating
     * a trend graph.
     */
    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
        ReFitSummaryAction fitAction = new ReFitSummaryAction(project);
        ReFitTrendAction trendAction = new ReFitTrendAction(project);
        return Arrays.asList(fitAction, trendAction);
    }
}
