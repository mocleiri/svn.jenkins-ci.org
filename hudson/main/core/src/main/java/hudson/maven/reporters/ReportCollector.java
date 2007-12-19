package hudson.maven.reporters;

import hudson.maven.MavenModule;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.maven.MavenBuildProxy;
import hudson.maven.MojoInfo;
import hudson.maven.MavenBuild;
import hudson.model.BuildListener;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;

import java.io.IOException;
import java.io.File;
import java.util.Locale;

/**
 * Watches out for executions of {@link MavenReport} mojos and record its output.
 * 
 * @author Kohsuke Kawaguchi
 */
public class ReportCollector extends MavenReporter {
    private transient ReportAction action;

    public boolean postExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener, Throwable error) throws InterruptedException, IOException {
        if(!(mojo.mojo instanceof MavenReport))
            return true;    // not a maven report

        MavenReport report = (MavenReport)mojo.mojo;

        String reportPath = report.getReportOutputDirectory().getPath();
        String projectReportPath = pom.getReporting().getOutputDirectory();
        if(!reportPath.startsWith(projectReportPath)) {
            // report is placed outside site. Can't record it.
            listener.getLogger().println("Maven report output goes to "+reportPath+", which is outside project reporting path"+projectReportPath);
            return true;
        }

        if(action==null)
            action = new ReportAction();


        // this is the entry point to the report
        File top = new File(report.getReportOutputDirectory(),report.getOutputName()+".html");
        String relPath = top.getPath().substring(projectReportPath.length());

        action.add(new ReportAction.Entry(relPath,report.getName(Locale.getDefault())));
        
        return true;
    }

    public boolean leaveModule(MavenBuildProxy build, MavenProject pom, BuildListener listener) throws InterruptedException, IOException {
        if(action!=null) {
            // TODO: archive pom.getReporting().getOutputDirectory()
            build.execute(new AddActionTask(action));
        }
        action = null;
        return super.leaveModule(build, pom, listener);
    }

    private static final class AddActionTask implements MavenBuildProxy.BuildCallable<Void,IOException> {
        private final ReportAction action;

        public AddActionTask(ReportAction action) {
            this.action = action;
        }

        public Void call(MavenBuild build) throws IOException, InterruptedException {
            build.addAction(action);
            return null;
        }

        private static final long serialVersionUID = 1L;
    }

    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }

    public static final class DescriptorImpl extends MavenReporterDescriptor {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

        private DescriptorImpl() {
            super(ReportCollector.class);
        }

        public String getDisplayName() {
            return "Record Maven reports";
        }

        public ReportCollector newAutoInstance(MavenModule module) {
            return new ReportCollector();
        }
    }

    private static final long serialVersionUID = 1L;
}
