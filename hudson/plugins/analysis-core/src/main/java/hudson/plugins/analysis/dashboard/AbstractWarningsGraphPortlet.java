package hudson.plugins.analysis.dashboard;

import java.util.List;

import com.google.common.collect.Lists;

import hudson.model.Job;

import hudson.plugins.analysis.core.ResultAction;
import hudson.plugins.analysis.core.AbstractProjectAction;
import hudson.plugins.analysis.graph.BuildResultGraph;
import hudson.plugins.analysis.graph.GraphConfiguration;
import hudson.plugins.analysis.graph.NewVersusFixedGraph;
import hudson.plugins.analysis.graph.NullGraph;
import hudson.plugins.analysis.graph.PriorityGraph;

import hudson.util.Graph;

/**
 * A portlet that shows a trend graph of the warnings in the selected jobs.
 *
 * @author Ulli Hafner
 */
public abstract class AbstractWarningsGraphPortlet extends AbstractPortlet {
    private final GraphConfiguration configuration = GraphConfiguration.createDefault();

    /**
     * Creates a new instance of {@link AbstractWarningsGraphPortlet}.
     *
     * @param name
     *            the name of the portlet
     */
    public AbstractWarningsGraphPortlet(final String name, final String width, final String height, final String dayCount, final BuildResultGraph graphType) {
        super(name);
    }

    /**
     * Returns the trend graph for specified jobs.
     *
     * @return the trend graph
     */
    public Graph getWarningsGraph() {
        List<ResultAction<?>> results = getActions();
        if (!results.isEmpty()) {
            return new PriorityGraph().getGraph(-1, configuration, getPluginName(), results);
        }
        return new NullGraph().getGraph(-1, configuration, getPluginName(), results);
    }

    /**
     * Returns the actions that should be used as base for the graph.
     *
     * @return the actions that should be used as base for the graph
     */
    private List<ResultAction<?>> getActions() {
        List<ResultAction<?>> results = Lists.newArrayList();
        for (Job<?, ?> job : getDashboard().getJobs()) {
            AbstractProjectAction<?> action = job.getAction(getAction());
            if (action != null && action.hasValidResults()) {
                results.add(action.getLastAction());
            }
        }
        return results;
    }

    /**
     * Returns the list of available graphs.
     *
     * @return the list of available graphs
     */
    public List<? extends BuildResultGraph> getAvailableGraphs() {
        List<BuildResultGraph> availableGraphs = Lists.newArrayList();

        availableGraphs.add(new PriorityGraph());
        availableGraphs.add(new NewVersusFixedGraph());

        return availableGraphs;
    }

    /**
     * Returns the height.
     *
     * @return the height
     */
    public int getHeight() {
        return configuration.getHeight();
    }

    /**
     * Returns the width.
     *
     * @return the width
     */
    public int getWidth() {
        return configuration.getWidth();
    }

    /**
     * Returns the number of days to consider.
     *
     * @return the number of days to consider
     */
    public int getDayCount() {
        return configuration.getDayCount();
    }

    /**
     * Returns the type of the graph.
     *
     * @return the type
     */
    public BuildResultGraph getGraphType() {
        return configuration.getGraphType();
    }

    /**
     * Checks whether a meaningful graph is available.
     *
     * @return <code>true</code>, if there is such a graph
     */
    public boolean hasMeaningfulGraph() {
        return !getActions().isEmpty();
    }
}

