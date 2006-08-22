package hudson.plugins.japex;

import com.sun.japex.report.MeanMode;
import hudson.util.ChartUtil;
import hudson.model.Project;
import hudson.model.ModelObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.Collection;

/**
 * Represents a trend report.
 *
 * @author Kohsuke Kawaguchi
 */
public class TrendReport implements ModelObject {

    /*package*/ final HudsonChartGenerator chartGen;

    private final String configName;
    private final Project project;

    TrendReport(Project project, String configName, HudsonChartGenerator chartGen) {
        this.project = project;
        this.configName = configName;
        this.chartGen = chartGen;
    }

    /**
     * This is the configuration file name.
     */
    public String getDisplayName() {
        return configName;
    }

    /**
     * Gets all the test case names.
     */
    public Collection<String> getTestCaseNames() throws IOException {
        return chartGen.getTestNames();
    }

    public Project getProject() {
        return project;
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

        if(chartGen.timestamp!=null && req.checkIfModified(chartGen.timestamp,rsp))
            return; // up to date

        ChartUtil.generateGraph(req,rsp,chartGen.createTrendChart(mean),400,200);
    }
}
