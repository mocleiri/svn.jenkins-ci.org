package hudson.plugins.japex;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import com.sun.japex.report.MeanMode;

import java.io.IOException;

import hudson.util.ChartUtil;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestCaseGraph {
    private final JapexReportAction owner;
    private final String name;

    public TestCaseGraph(JapexReportAction owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if(ChartUtil.awtProblem) {
            // not available. send out error message
            rsp.sendRedirect2(req.getContextPath()+"/images/headless.png");
            return;
        }

        HudsonChartGenerator gen = owner.createGenerator();

        if(gen.timestamp!=null && req.checkIfModified(gen.timestamp,rsp))
            return; // up to date

        ChartUtil.generateGraph(req,rsp,gen.createTrendChart(name),400,200);
    }

}
