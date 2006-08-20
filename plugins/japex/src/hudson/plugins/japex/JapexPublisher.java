package hudson.plugins.japex;

import hudson.Launcher;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.tasks.Publisher;
import org.apache.tools.ant.taskdefs.Copy;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;

/**
 * Records the japex test report for builds.
 *
 * @author Kohsuke Kawaguchi
 */
public class JapexPublisher extends Publisher {
    /**
     * Relative path to the Japex XML report file.
     */
    private String includes;

    public JapexPublisher(String japexReport) {
        this.includes = japexReport;
    }

    public String getIncludes() {
        return includes;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
        File file = new File(build.getProject().getWorkspace().getLocal(), includes);
        listener.getLogger().println("Recording japex report "+file);

        Copy copy = new Copy();
        copy.setProject(new org.apache.tools.ant.Project());
        copy.setFile(file);
        copy.setTofile(getJapexReport(build));
        copy.execute();

        return true;
    }

    /**
     * Gets the location of the report file for the given build.
     */
    static File getJapexReport(Build build) {
        return new File(build.getRootDir(),"japex.xml");
    }

    public Action getProjectAction(Project project) {
        return new JapexReportAction(project);
    }

    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor<Publisher> DESCRIPTOR = new Descriptor<Publisher>(JapexPublisher.class) {
        public String getDisplayName() {
            return "Record Japex test report";
        }

        public String getHelpFile() {
            return "/plugin/japex/help.html";
        }

        public Publisher newInstance(StaplerRequest req) {
            return new JapexPublisher(req.getParameter("japex.includes"));
        }
    };
}
