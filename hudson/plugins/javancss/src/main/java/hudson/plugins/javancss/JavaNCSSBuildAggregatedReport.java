package hudson.plugins.javancss;

import hudson.maven.*;
import hudson.model.Action;
import hudson.model.HealthReport;
import hudson.plugins.javancss.parser.Statistic;

import java.util.*;

/**
 * TODO javadoc.
 *
 * @author Stephen Connolly
 * @since 08-Jan-2008 21:12:12
 */
public class JavaNCSSBuildAggregatedReport extends AbstractBuildReport<MavenModuleSetBuild> implements MavenAggregatedReport {
    private HealthReport buildHealth = null;

    public JavaNCSSBuildAggregatedReport(MavenModuleSetBuild build, Map<MavenModule, List<MavenBuild>> moduleBuilds) {
        super(new ArrayList<Statistic>());
        setBuild(build);
        Map<MavenModule, List<MavenBuild>> processedModuleBuilds = new HashMap<MavenModule, List<MavenBuild>>();
        for (Map.Entry<MavenModule, List<MavenBuild>> childList : moduleBuilds.entrySet()) {
            if (!processedModuleBuilds.containsKey(childList.getKey())) {
                List<MavenBuild> processedChildren = new ArrayList<MavenBuild>();
                for (MavenBuild child : childList.getValue()) {
                    if (!processedChildren.contains(child)) {
                        update(processedModuleBuilds, child);
                        processedChildren.add(child);
                    }
                }
                processedModuleBuilds.put(childList.getKey(), processedChildren);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void update(Map<MavenModule, List<MavenBuild>> moduleBuilds, MavenBuild newBuild) {
        JavaNCSSBuildIndividualReport report = newBuild.getAction(JavaNCSSBuildIndividualReport.class);
        if (report != null) {
            Collection<Statistic> u = Statistic.merge(report.getResults(), getResults());
            getResults().clear();
            getResults().addAll(u);
            getTotals().add(report.getTotals());
            buildHealth = HealthReport.min(buildHealth, report.getBuildHealth());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Class<? extends AggregatableAction> getIndividualActionType() {
        return JavaNCSSBuildIndividualReport.class;
    }

    /**
     * {@inheritDoc}
     */
    public Action getProjectAction(MavenModuleSet moduleSet) {
        for (MavenModuleSetBuild build : moduleSet.getBuilds()) {
            if (build.getAction(JavaNCSSBuildAggregatedReport.class) != null) {
                return new JavaNCSSProjectAggregatedReport(moduleSet);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public HealthReport getBuildHealth() {
        return buildHealth;
    }

}
