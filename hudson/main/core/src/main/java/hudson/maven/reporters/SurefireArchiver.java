package hudson.maven.reporters;

import hudson.Util;
import hudson.maven.MavenBuild;
import hudson.maven.MavenBuildProxy;
import hudson.maven.MavenBuildProxy.BuildCallable;
import hudson.maven.MavenBuilder;
import hudson.maven.MavenModule;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.maven.MojoInfo;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.TestResultProjectAction;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Records the surefire test result.
 * @author Kohsuke Kawaguchi
 */
public class SurefireArchiver extends MavenReporter {
    private TestResult result;

    public boolean preExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener) throws InterruptedException, IOException {
        if (isSurefireTest(mojo)) {
            // tell surefire:test to keep going even if there was a failure,
            // so that we can record this as yellow.
            // note that because of the way Maven works, just updating system property at this point is too late
            XmlPlexusConfiguration c = (XmlPlexusConfiguration) mojo.configuration.getChild("testFailureIgnore");
            if(c!=null && c.getValue().equals("${maven.test.failure.ignore}") && System.getProperty("maven.test.failure.ignore")==null)
                c.setValue("true");
        }
        return true;
    }

    public boolean postExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, final BuildListener listener, Throwable error) throws InterruptedException, IOException {
        if (!isSurefireTest(mojo)) return true;

        listener.getLogger().println("[HUDSON] Recording test results");

        File reportsDir;
        try {
            reportsDir = mojo.getConfigurationValue("reportsDirectory", File.class);
        } catch (ComponentConfigurationException e) {
            e.printStackTrace(listener.fatalError("Unable to obtain the reportsDirectory from surefire:test mojo"));
            build.setResult(Result.FAILURE);
            return true;
        }

        if(reportsDir.exists()) {
            // surefire:test just skips itself when the current project is not a java project

            FileSet fs = Util.createFileSet(reportsDir,"*.xml");
            DirectoryScanner ds = fs.getDirectoryScanner();

            if(ds.getIncludedFiles().length==0)
                // no test in this module
                return true;

            if(result==null)
                result = new TestResult(build.getTimestamp().getTimeInMillis() - 1000/*error margin*/, ds);
            else
                result.parse(build.getTimestamp().getTimeInMillis() - 1000/*error margin*/, ds);

            int failCount = build.execute(new BuildCallable<Integer, IOException>() {
                public Integer call(MavenBuild build) throws IOException, InterruptedException {
                    SurefireReport sr = build.getAction(SurefireReport.class);
                    if(sr==null)
                        build.getActions().add(new SurefireReport(build, result, listener));
                    else
                        sr.setResult(result,listener);
                    if(result.getFailCount()>0)
                        build.setResult(Result.UNSTABLE);
                    build.registerAsProjectAction(SurefireArchiver.this);
                    return result.getFailCount();
                }
            });

            // if surefire plugin is going to kill maven because of a test failure,
            // intercept that (or otherwise build will be marked as failure)
            if(failCount>0 && error instanceof MojoFailureException) {
                MavenBuilder.markAsSuccess = true;
            }
        }

        return true;
    }


    public Action getProjectAction(MavenModule module) {
        return new TestResultProjectAction(module);
    }

    private boolean isSurefireTest(MojoInfo mojo) {
        return mojo.pluginName.matches("org.apache.maven.plugins", "maven-surefire-plugin") && mojo.getGoal().equals("test");
    }

    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }

    public static final class DescriptorImpl extends MavenReporterDescriptor {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

        private DescriptorImpl() {
            super(SurefireArchiver.class);
        }

        public String getDisplayName() {
            return "Publish surefire reports";
        }

        public SurefireArchiver newAutoInstance(MavenModule module) {
            return new SurefireArchiver();
        }
    }

    private static final long serialVersionUID = 1L;
}
