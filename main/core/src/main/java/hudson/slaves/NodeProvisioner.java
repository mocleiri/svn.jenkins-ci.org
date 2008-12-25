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

        /*
            Here we determine how many additional slaves we need to keep up with the load (if at all),
            which involves a simple math.

            Broadly speaking, first we check that all the executors are fully utilized before attempting
            to start any new slave (this also helps to ignore the temporary gap between different numbers,
            as changes in them are not necessarily synchronized --- for example, there's a time lag between
            when a slave launches (thus bringing the planned capacity down) and the time when its executors
            pick up builds (thus bringing the queue length down.)

            Once we confirm that, we compare the # of buildable items against the additional slaves
            that are being brought online. If we have more jobs than our executors can handle, we'll launch a new slave.

            So this computation involves three stats:

              1. # of idle executors
              2. # of jobs that are starving for executors
              3. # of additional slaves being provisioned (planned capacities.)

            To ignore a temporary surge/drop, we make conservative estimates on each one of them. That is,
            we take the current snapshot value, and we take the current exponential moving average (EMA) value,
            and use the max/min.

            This is another measure to be robust against temporary surge/drop in those indicators, and helps
            us avoid over-reacting to stats.

            If we only use the snapshot value or EMA value, tests confirmed that the gap creates phantom
            excessive loads and Hudson ends up firing excessive capacities. In a static system, over the time
            EMA and the snapshot value becomes the same, so this makes sure that in a long run this conservative
            estimate won't create a starvation.
         */

        float idle = Math.min(stat.getLatestIdleExecutors(picker),hudson.getComputer().getIdleExecutors());
        if(idle<MARGIN) {
            // make sure the system is fully utilized before attempting any new launch.

            // this is the amount of work left to be done
            float qlen = Math.min(stat.queueLength.getLatest(picker), hudson.getQueue().getBuildableItems().size());

            // ... and this is the additional executors we've already provisioned.
            plannedCapacity = Math.max(plannedCapacitiesEMA.getLatest(picker),plannedCapacity);

            float excessWorkload = qlen - plannedCapacity;
            if(excessWorkload>1-MARGIN) {// and there's more work to do...
                LOGGER.info("Excess workload "+excessWorkload+" detected. (planned capacity="+plannedCapacity+",Qlen="+qlen+")");
                for( Cloud c : hudson.clouds ) {
                    if(excessWorkload<0)    break;  // enough slaves allocated

                    // provisioning a new node should be conservative --- for example if exceeWorkload is 1.4,
                    // we don't want to allocate two nodes but just one.
                    // OTOH, because of the exponential decay, even when we need one slave, excess workload is always
                    // something like 0.95, in which case we want to allocate one node.
                    // so the threshold here is 1-MARGIN, and hence floor(excessWorkload+MARGIN) is needed to handle this.

                    Collection<PlannedNode> additionalCapacities = c.provision((int)Math.round(Math.floor(excessWorkload+MARGIN)));
                    for (PlannedNode ac : additionalCapacities) {
                        LOGGER.info("Provisioned "+ac.displayName+" from "+c.name+" with "+ac.numExecutors+" executors. Remaining excess workload:"+excessWorkload);
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
