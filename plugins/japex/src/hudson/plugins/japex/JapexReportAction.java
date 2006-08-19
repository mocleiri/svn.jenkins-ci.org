package hudson.plugins.japex;

import com.sun.japex.report.ChartGenerator;
import com.sun.japex.report.TestSuiteReport;
import com.sun.japex.report.MeanMode;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.Project;
import hudson.util.ChartUtil;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

/**
 * Project action to display trend reports.
 *
 * @author Kohsuke Kawaguchi
 */
public class JapexReportAction implements Action {
    private final Project project;

    public JapexReportAction(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public String getDisplayName() {
        return "Japex Trend Report";
    }

    public String getIconFileName() {
        return "graph.gif";
    }

    public String getUrlName() {
        return "japex";
    }

    /**
     * Cached {@link ChartGenerator}.
     */
    private WeakReference<HudsonChartGenerator> chartGen = null;

    /**
     * Creates the {@link HudsonChartGenerator} (or reuse the last one.)
     */
    /*package*/ synchronized HudsonChartGenerator createGenerator() throws IOException {
        Build lb = project.getLastBuild();

        if(chartGen!=null) {
            HudsonChartGenerator gen = chartGen.get();
            if(gen!=null && lb!=null && gen.buildNumber==lb.getNumber())
                return gen; // reuse the cached instance
        }

        List<TestSuiteReport> reports = new ArrayList<TestSuiteReport>();
        for (Build build : project.getBuilds()) {
            File f = JapexPublisher.getJapexReport(build);
            if(f.exists())
                try {
                    reports.add(new TestSuiteReport(f));
                } catch (SAXException e) {
                    IOException x = new IOException("Failed to parse " + f);
                    x.initCause(e);
                    throw x;
                } catch (RuntimeException e) {
                    // Japex sometimes intentionally send RuntimeException
                    IOException x = new IOException("Failed to parse " + f);
                    x.initCause(e);
                    throw x;
                }
        }

        HudsonChartGenerator gen = new HudsonChartGenerator(reports,lb);
        chartGen = new WeakReference<HudsonChartGenerator>(gen);

        return gen;
    }

    /**
     * Gets all the test case names.
     */
    public Collection<String> getTestCaseNames() throws IOException {
        return createGenerator().getTestNames();
    }

//
//
// Web methods
//
//

    /**
     * Gets to the object that represents individual test caase result.
     */
    public TestCaseGraph getTestCaseGraph(String name) {
        return new TestCaseGraph(this,name);
    }

    public void doArithmeticMeanGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
        doMeanGraph(req,rsp, MeanMode.ARITHMETIC);
    }

    public void doGeometricMeanGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
        doMeanGraph(req,rsp, MeanMode.GEOMETRIC);
    }

    public void doHarmonicMeanGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
        doMeanGraph(req,rsp, MeanMode.HARMONIC);
    }

    private void doMeanGraph(StaplerRequest req, StaplerResponse rsp, MeanMode mean) throws IOException {
        if(ChartUtil.awtProblem) {
            // not available. send out error message
            rsp.sendRedirect2(req.getContextPath()+"/images/headless.png");
            return;
        }

        HudsonChartGenerator gen = createGenerator();

        if(gen.timestamp!=null && req.checkIfModified(gen.timestamp,rsp))
            return; // up to date

        ChartUtil.generateGraph(req,rsp,gen.createTrendChart(mean),400,200);
    }
}
