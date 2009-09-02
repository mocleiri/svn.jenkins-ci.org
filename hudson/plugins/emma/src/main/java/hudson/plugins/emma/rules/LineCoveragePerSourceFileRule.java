package hudson.plugins.emma.rules;

import hudson.model.TaskListener;
import hudson.plugins.emma.Rule;
import hudson.plugins.emma.CoverageReport;
import hudson.plugins.emma.PackageReport;
import hudson.plugins.emma.SourceFileReport;

/**
 * Flags a failure if the line coverage of a source file
 * goes below a certain threashold.
 */
public class LineCoveragePerSourceFileRule extends Rule {
    private final float minPercentage;

    public LineCoveragePerSourceFileRule(float minPercentage) {
        this.minPercentage = minPercentage;
    }

    public void enforce(CoverageReport report, TaskListener listener) {
        for (PackageReport pack : report.getChildren().values()) {
            for (SourceFileReport sfReport : pack.getChildren().values()) {
                float percentage = sfReport.getLineCoverage().getPercentageFloat();

                if (percentage < minPercentage) {
                    listener.getLogger().println(sfReport.getDisplayName() + " failed (below " + minPercentage + "%).");
                    sfReport.setFailed();
                }
            }
        }
    }
}
