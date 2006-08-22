package hudson.plugins.japex;

import com.sun.japex.report.TestSuiteReport;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Result;
import hudson.tasks.Publisher;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;

/**
 * Records the japex test report for builds.
 *
 * @author Kohsuke Kawaguchi
 */
public class JapexPublisher extends Publisher {
    /**
     * Relative path to the Japex XML report files.
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
        listener.getLogger().println("Recording japex reports "+includes);

        org.apache.tools.ant.Project antProject = new org.apache.tools.ant.Project();

        FileSet fs = new FileSet();
        fs.setDir(build.getProject().getWorkspace().getLocal());
        fs.setIncludes(includes);

        DirectoryScanner ds = fs.getDirectoryScanner(antProject);
        String[] includedFiles = ds.getIncludedFiles();

        if(includedFiles.length==0) {
            listener.error("No matching file found. Configuration error?");
            build.setResult(Result.FAILURE);
            return true;
        }

        File outDir = getJapexReport(build);
        outDir.mkdir();

        for (String f : includedFiles) {
            File file = new File(ds.getBasedir(),f);

            if(file.lastModified()<build.getTimestamp().getTimeInMillis()) {
                listener.getLogger().println("Ignoring old file: "+file);
                continue;
            }

            listener.getLogger().println(file);

            String configName;

            try {
                TestSuiteReport rpt = new TestSuiteReport(file);
                configName = rpt.getParameters().get("configFile").replace('/','.');
            } catch (Exception e) {
                // TestSuiteReport ctor does throw RuntimeException
                e.printStackTrace(listener.error(e.getMessage()));
                continue;
            }

            // copy
            File dst = new File(outDir,configName);

            Copy copy = new Copy();
            copy.setProject(antProject);
            copy.setFile(file);
            copy.setTofile(dst);
            copy.execute();
        }

        return true;
    }

    /**
     * Gets the directory to store report files
     */
    static File getJapexReport(Build build) {
        return new File(build.getRootDir(),"japex");
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
