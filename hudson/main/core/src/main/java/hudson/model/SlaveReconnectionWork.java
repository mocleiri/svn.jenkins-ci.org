package hudson.model;

import hudson.model.Slave.ComputerImpl;
import static hudson.model.SlaveAvailabilityStrategy.*;
import hudson.triggers.SafeTimerTask;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Periodically checks the slaves and try to reconnect dead slaves.
 *
 * @author Kohsuke Kawaguchi
 */
public class SlaveReconnectionWork extends SafeTimerTask {
    protected void doRun() {
        // use a weak hashmap
        Map<Slave, Long> nextCheck = new WeakHashMap<Slave, Long>();
        for (Slave s : Hudson.getInstance().getSlaves()) {
            if (!nextCheck.containsKey(s) || System.currentTimeMillis() > nextCheck.get(s)) {
                final Queue queue = Hudson.getInstance().getQueue();
                boolean hasJob = false;
                for (Executor exec: s.getComputer().getExecutors()) {
                    if (!exec.isIdle()) {
                        hasJob = true;
                        break;
                    }
                }
                // TODO get only the items from the queue that can apply to this slave
                State state = new State(queue.getItems().length > 0, hasJob);
                // at the moment I don't trust strategies to wait more than 60 minutes
                // strategies need to wait at least one minute
                final long waitInMins = Math.min(1, Math.max(60, s.getAvailabilityStrategy().check(s, state)));
                nextCheck.put(s, System.currentTimeMillis() + 60 * 1000 * waitInMins);
            }
        }
    }
}
