package hudson.tasks.junit;

import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.AntBasedBuildStep;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.Launcher;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import javax.servlet.http.HttpServletRequest;

/**
 * Generates HTML report from JUnit test result XML files.
 *
 * @author Kohsuke Kawaguchi
 */
public class JUnitResultArchiver extends AntBasedBuildStep {

    /**
     * {@link FileSet} "includes" string, like "foo/bar/*.xml"
     */
    private final String testResults;

    public JUnitResultArchiver(String testResults) {
        this.testResults = testResults;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true; // noop
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
        FileSet fs = new FileSet();
        Project p = new Project();
        fs.setProject(p);
        fs.setDir(build.getProject().getWorkspace().getLocal());
        fs.setIncludes(testResults);

        TestResultAction action = new TestResultAction(build, fs.getDirectoryScanner(p), listener);
        build.getActions().add(action);

        if(action.getResult().getFailCount()>0)
            build.setResult(Result.UNSTABLE);

        return true;
    }

    public String getTestResults() {
        return testResults;
    }


    public BuildStepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor DESCRIPTOR = new Descriptor();

    public static final class Descriptor extends BuildStepDescriptor {
        public Descriptor() {
            super(JUnitResultArchiver.class);
        }

        public String getDisplayName() {
            return "Publish JUnit test result report";
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new JUnitResultArchiver(req.getParameter("junitreport_includes"));
        }
    }}
