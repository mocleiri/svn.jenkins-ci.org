package hudson.plugins.jwsdp_sqe;

import hudson.Launcher;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Result;
import hudson.tasks.Publisher;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Collects SQE test reports and convert them into JUnit format.
 *
 * @author Kohsuke Kawaguchi
 */
public class SQETestResultPublisher implements Publisher {

    private final String includes;

    public SQETestResultPublisher(String includes) {
        this.includes = includes;
    }

    /**
     * Ant "&lt;fileset @includes="..." /> pattern to specify SQE XML files
     */
    public String getIncludes() {
        return includes;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
        FileSet fs = new FileSet();
        org.apache.tools.ant.Project p = new org.apache.tools.ant.Project();
        fs.setProject(p);
        fs.setDir(build.getProject().getWorkspace().getLocal());
        fs.setIncludes(includes);
        DirectoryScanner ds = fs.getDirectoryScanner(p);

        if(ds.getIncludedFiles().length==0) {
            listener.getLogger().println("No SQE test report files wer efound. Configuration error?");
            // no test result. Most likely a configuration error or fatal problem
            build.setResult(Result.FAILURE);
        }

        SQETestAction action = new SQETestAction(build, ds, listener);
        build.getActions().add(action);

        Report r = action.getResult();

        if(r.getTotalCount()==0) {
            listener.getLogger().println("Test reports were found but none of them are new. Did tests run?");
            // no test result. Most likely a configuration error or fatal problem
            build.setResult(Result.FAILURE);
        }

        if(r.getFailCount()>0)
            build.setResult(Result.UNSTABLE);

        return true;
    }

    public Action getProjectAction(Project project) {
        return null;
    }

    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor<Publisher> DESCRIPTOR = new Descriptor<Publisher>(SQETestResultPublisher.class) {
        public String getDisplayName() {
            return "Publish SQE test result report";
        }

        public String getHelpFile() {
            return "/plugin/jwsdp-sqe/help.html";
        }

        public Publisher newInstance(StaplerRequest req) {
            return new SQETestResultPublisher(req.getParameter("sqetest_includes"));
        }
    };
}
