package hudson.slaves;

import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Queue;
import hudson.triggers.SafeTimerTask;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Periodically checks the slaves and try to reconnect dead slaves.
 *
 * @author Kohsuke Kawaguchi
 */
public class SlaveReconnectionWork extends SafeTimerTask {
    /**
     * Use weak hash map to avoid leaking {@link Computer}.
     */
    private final Map<Computer,Long> nextCheck = new WeakHashMap<Computer,Long>();

    protected void doRun() {
        final Queue queue = Hudson.getInstance().getQueue();
        
        for (Computer c : Hudson.getInstance().getComputers()) {
            if (!nextCheck.containsKey(c) || System.currentTimeMillis() > nextCheck.get(c)) {
                boolean hasJob = !c.isIdle();
                
                // TODO get only the items from the queue that can apply to this slave
                SlaveAvailabilityStrategy.State state = new SlaveAvailabilityStrategy.State(queue.getItems().length > 0, hasJob);
                // at the moment I don't trust strategies to wait more than 60 minutes
                // strategies need to wait at least one minute
                final long waitInMins = Math.min(1, Math.max(60, c.getAvailabilityStrategy().check(c, state)));
                nextCheck.put(c, System.currentTimeMillis() + 60 * 1000 * waitInMins);
            }
        }
    }
}
