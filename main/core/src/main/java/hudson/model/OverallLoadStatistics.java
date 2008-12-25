package hudson.model;

/**
 * {@link LoadStatistics} for the entire system (the master and all the slaves combined.)
 *
 * <p>
 * {@link #computeQueueLength()} and {@link #queueLength} counts those tasks
 * that are unassigned to any node, whereas {@link #totalQueueLength}
 * tracks the queue length including tasks that are assigned to a specific node.
 *
 * @author Kohsuke Kawaguchi
 * @see Hudson#overallLoad
 */
public class OverallLoadStatistics extends LoadStatistics {
    /**
     * Number of total {@link Queue.BuildableItem}s that represents blocked builds.
     */
    public final MultiStageTimeSeries totalQueueLength = new MultiStageTimeSeries(0,DECAY);

    private final Hudson hudson = Hudson.getInstance();

    /*package*/ OverallLoadStatistics() {
        super(0,0);
    }

    @Override
    public int computeIdleExecutors() {
        return hudson.getComputer().getIdleExecutors();
    }

    @Override
    public int computeQueueLength() {
        return hudson.getQueue().countBuildableItemsFor(null);
    }
}
