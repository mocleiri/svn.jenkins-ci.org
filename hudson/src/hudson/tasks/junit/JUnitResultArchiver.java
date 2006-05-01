package hudson.tasks.junit;

import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.AntBasedBuildStep;
import hudson.tasks.BuildStep;
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

        TestResult r = action.getResult();

        if(r.getPassCount()==0 && r.getFailCount()==0)
            // no test result. Most likely a configuration error or fatal problem
            build.setResult(Result.FAILURE);

        if(r.getFailCount()>0)
            build.setResult(Result.UNSTABLE);

        return true;
    }

    public String getTestResults() {
        return testResults;
    }


    public Descriptor<BuildStep> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor<BuildStep> DESCRIPTOR = new Descriptor<BuildStep>(JUnitResultArchiver.class) {
        public String getDisplayName() {
            return "Publish JUnit test result report";
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new JUnitResultArchiver(req.getParameter("junitreport_includes"));
        }
    };
}
