package hudson.model;

import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import hudson.model.MultiStageTimeSeries.Picker;

import java.util.List;

/**
 * Utilization statistics for a node or a set of nodes.
 *
 * @author Kohsuke Kawaguchi
 * @see Label#load
 * @see Hudson#overallLoad
 */
public class LoadStatistics {
    /**
     * Number of busy executors and how it changes over time.
     */
    public final MultiStageTimeSeries busyExecutors;

    /**
     * Number of total executors and how it changes over time.
     */
    public final MultiStageTimeSeries totalExecutors;

    /**
     * Number of {@link Queue.BuildableItem}s that can run on any node in this node set but blocked.
     */
    public final MultiStageTimeSeries queueLength;

    protected LoadStatistics(int initialTotalExecutors, int initialBusyExecutors) {
        this.totalExecutors = new MultiStageTimeSeries(initialTotalExecutors,DECAY);
        this.busyExecutors = new MultiStageTimeSeries(initialBusyExecutors,DECAY);
        this.queueLength = new MultiStageTimeSeries(0,DECAY);
    }

    public float getLatestIdleExecutors(Picker picker) {
        return totalExecutors.pick(picker).getLatest() - busyExecutors.pick(picker).getLatest();
    }

    /**
     * Start updating the load average.
     */
    /*package*/ static void register() {
        Trigger.timer.scheduleAtFixedRate(
            new SafeTimerTask() {
                protected void doRun() {
                    Hudson h = Hudson.getInstance();
                    List<Queue.BuildableItem> bis = h.getQueue().getBuildableItems();

                    // update statistics on slaves
                    for( Label l : h.getLabels() ) {
                        l.load.totalExecutors.update(l.getTotalExecutors());
                        l.load.busyExecutors .update(l.getBusyExecutors());

                        int q=0;
                        for (Queue.BuildableItem bi : bis) {
                            if(bi.task.getAssignedLabel()==l)
                                q++;
                        }
                        l.load.queueLength.update(q);
                    }

                    // update statistics of the entire system
                    ComputerSet cs = h.getComputer();
                    h.overallLoad.totalExecutors.update(cs.getTotalExecutors());
                    h.overallLoad.busyExecutors .update(cs.getBusyExecutors());
                    int q=0;
                    for (Queue.BuildableItem bi : bis) {
                        if(bi.task.getAssignedLabel()==null)
                            q++;
                    }
                    h.overallLoad.queueLength.update(q);
                    h.overallLoad.totalQueueLength.update(bis.size());
                }
            }, CLOCK, CLOCK
        );
    }


    /**
     * With 0.90 decay ratio for every 10sec, half reduction is about 1 min.
     */
    public static final float DECAY = Float.parseFloat(System.getProperty(LoadStatistics.class.getName()+".decay","0.9"));
    /**
     * Load statistics clock cycle in milliseconds. Specify a small value for quickly debugging this feature and node provisioning through cloud.
     */
    public static int CLOCK = Integer.getInteger(LoadStatistics.class.getName()+".clock",10*1000);
}
