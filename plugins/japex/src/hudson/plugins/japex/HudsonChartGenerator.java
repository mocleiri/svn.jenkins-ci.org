package hudson.plugins.japex;

import com.sun.japex.report.ChartGenerator;
import com.sun.japex.report.TestSuiteReport;

import java.util.List;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Collection;

import hudson.model.Build;

/**
 * @author Kohsuke Kawaguchi
 */
final class HudsonChartGenerator extends ChartGenerator {
    /**
     * Gets the last build # of the file in this generator.
     */
    final int buildNumber;

    final Calendar timestamp;

    private final Set<String> testNames = new LinkedHashSet<String>();

    public HudsonChartGenerator(List<? extends TestSuiteReport> reports, Build b) {
        super(reports);
        this.buildNumber = b==null ? 0 : b.getNumber();
        timestamp = b==null ? null : b.getTimestamp();

        // Populate set of test cases across all reports
        for (TestSuiteReport report : reports) {
            for (TestSuiteReport.Driver driver : report.getDrivers()) {
                for (TestSuiteReport.TestCase test : driver.getTestCases()) {
                    testNames.add(test.getName());
                }
            }
        }
    }

    public Collection<String> getTestNames() {
        return testNames;
    }

}
