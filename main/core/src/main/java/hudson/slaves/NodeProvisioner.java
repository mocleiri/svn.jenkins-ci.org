package hudson.slaves;

import hudson.model.LoadStatistics;
import hudson.model.Node;
import hudson.model.Hudson;
import hudson.model.MultiStageTimeSeries;
import hudson.model.MultiStageTimeSeries.Picker;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;

import java.util.concurrent.Future;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Uses the {@link LoadStatistics} and determines when we need to allocate
 * a new {@link Node} through {@link Cloud}.
 *
 * @author Kohsuke Kawaguchi
 */
public class NodeProvisioner extends SafeTimerTask {
    /**
     * The node addition activity in progress.
     */
    public static final class PlannedNode {
        public final String displayName;
        public final Future<Node> future;
        public final int numExecutors;

        public PlannedNode(String displayName, Future<Node> future, int numExecutors) {
            if(displayName==null || future==null || numExecutors<1)  throw new IllegalArgumentException();
            this.displayName = displayName;
            this.future = future;
            this.numExecutors = numExecutors;
        }
    }

    private List<PlannedNode> pendingLaunches = new CopyOnWriteArrayList<PlannedNode>();


    private NodeProvisioner() {}

    @Override
    protected void doRun() {
        // TODO: picker should be selectable
        MultiStageTimeSeries.Picker picker = Picker.SEC10;

        // clean up the cancelled launch activity, then count the # of executors that we are about to bring up.
        int plannedCapacity = 0;
        for (Iterator<PlannedNode> itr = pendingLaunches.iterator(); itr.hasNext();) {
            PlannedNode f =  itr.next();
            if(f.future.isDone()) {
                itr.remove();
                continue;
            }
            plannedCapacity += f.numExecutors;
        }

        Hudson hudson = Hudson.getInstance();

        // TODO: do this for each label separately.
        LoadStatistics stat = hudson.overallLoad;

        float idle = stat.getLatestIdleExecutors(picker);
        if(idle<MARGIN) {// the system is fully utilized
            float excessWorkload = stat.queueLength.getLatest(picker) - plannedCapacity;
            if(excessWorkload>1-MARGIN) {// and there's more work to do than we are currently bringing up
                LOGGER.fine("Excess workload "+excessWorkload+" detected.");
                for( Cloud c : hudson.clouds ) {
                    if(excessWorkload<0)    break;  // enough slaves allocated
                    Collection<PlannedNode> additionalCapacities = c.provision(excessWorkload);
                    for (PlannedNode ac : additionalCapacities) {
                        LOGGER.info(ac.displayName+" provisioned from "+c.name+" with "+ac.numExecutors+" (remaining excess:"+excessWorkload+")");
                        excessWorkload -= ac.numExecutors;
                    }
                    pendingLaunches.addAll(additionalCapacities);
                }
            }
        }
    }

    public static void launch() {
        Trigger.timer.scheduleAtFixedRate(new NodeProvisioner(),
                // give some initial warm up time so that statically connected slaves
                // can be brought online before we start allocating more. 
                LoadStatistics.CLOCK*1000*10,
                LoadStatistics.CLOCK*1000);
    }

    private static final float MARGIN = 0.1f;
    private static final Logger LOGGER = Logger.getLogger(NodeProvisioner.class.getName());
}
