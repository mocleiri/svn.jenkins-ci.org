package hudson.slaves;

import hudson.model.LoadStatistics;
import hudson.model.Node;
import hudson.model.Hudson;
import hudson.model.MultiStageTimeSeries;
import static hudson.model.LoadStatistics.DECAY;
import hudson.model.MultiStageTimeSeries.Picker;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;

import java.util.concurrent.Future;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;

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

    /**
     * Exponential moving average of the "planned ePcapacity" over time, which is the number of
     * additional executors being brought up.
     *
     * This is used to filter out high-frequency components from the planned capacity, so that
     * the comparison with other low-frequency only variables won't leave spikes. 
     */
    private final MultiStageTimeSeries plannedCapacitiesEMA = new MultiStageTimeSeries(0,DECAY);

    private NodeProvisioner() {}

    @Override
    protected void doRun() {
        // TODO: picker should be selectable
        MultiStageTimeSeries.Picker picker = Picker.SEC10;

        // clean up the cancelled launch activity, then count the # of executors that we are about to bring up.
        float plannedCapacity = 0;
        for (PlannedNode f : pendingLaunches) {
            if(f.future.isDone()) {
                try {
                    f.future.get();
                } catch (InterruptedException e) {
                    throw new AssertionError(e); // since we confirmed that the future is already done
                } catch (ExecutionException e) {
                    LOGGER.log(Level.WARNING, "Provisioned slave failed to launch",e.getCause());
                }
                pendingLaunches.remove(f);
                continue;
            }
            plannedCapacity += f.numExecutors;
        }
        plannedCapacitiesEMA.update(plannedCapacity);

        Hudson hudson = Hudson.getInstance();

        // TODO: do this for each label separately.
        LoadStatistics stat = hudson.overallLoad;

        float idle = stat.getLatestIdleExecutors(picker);
        if(idle<MARGIN) {// the system is fully utilized
            // make a conservative estimate of queue length
            // stat.queueLength is the exponential moving average, so when there's a temporal queue length surge,
            // it increases slowly, which helps us avoid over-reaction.
            // OTOH, when the Q length goes down because of new capacity, the moving average decreases only slowly,
            // while planned capacity goes down immediately, so this creates phantom excessive workload.
            //
            // so we look at the current Q length as well and take a smaller value, to be conservative.
            // in a long run when nothing changes, the moving average and the snapshot value is identical,
            // so this ensures that we won't have something in Q forever waiting for an extra capacity.
            float qlen = Math.min(stat.queueLength.getLatest(picker), hudson.getQueue().getBuildableItems().size());

            // when we subtract plannedCapacity from qlen below, also make a conservative estimate
            // by mixing in  the EMA of planned capacity. This prevents a temporary glitch from allocating
            // more slaves.
            plannedCapacity = Math.max(plannedCapacitiesEMA.getLatest(picker),plannedCapacity);

            float excessWorkload = qlen - plannedCapacity;
            if(excessWorkload>1-MARGIN) {// and there's more work to do than we are currently bringing up
                LOGGER.fine("Excess workload "+excessWorkload+" detected. (planned capacity="+plannedCapacity+",Qlen="+qlen+")");
                for( Cloud c : hudson.clouds ) {
                    if(excessWorkload<0)    break;  // enough slaves allocated

                    // provisioning a new node should be conservative --- for example if exceeWorkload is 1.4,
                    // we don't want to allocate two nodes but just one.
                    // OTOH, because of the exponential decay, even when we need one slave, excess workload is always
                    // something like 0.95, in which case we want to allocate one node.
                    // so the threshold here is 1-MARGIN, and hence floor(excessWorkload+MARGIN) is needed to handle this.

                    Collection<PlannedNode> additionalCapacities = c.provision((int)Math.round(Math.floor(excessWorkload+MARGIN)));
                    for (PlannedNode ac : additionalCapacities) {
                        LOGGER.info(ac.displayName+" provisioned from "+c.name+" with "+ac.numExecutors+" (remaining excess workload:"+excessWorkload+")");
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
                LoadStatistics.CLOCK*10,
                LoadStatistics.CLOCK);
    }

    private static final float MARGIN = 0.1f;
    private static final Logger LOGGER = Logger.getLogger(NodeProvisioner.class.getName());
}
