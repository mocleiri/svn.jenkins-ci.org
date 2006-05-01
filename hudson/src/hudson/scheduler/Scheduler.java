package hudson.scheduler;

import java.util.Date;
import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi
 */
public interface Scheduler {
    /**
     * Gets the next {@link Date} when a task needs to execute.
     *
     * @return
     *      Must return a date in the future.
     */
    Date next();
}
