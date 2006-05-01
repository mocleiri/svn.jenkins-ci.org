package hudson.scheduler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Scheduled tasks.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Task {
    private static Timer timer = new Timer();

    private final TimerTask timerTask = new TimerTask() {
        public void run() {
            try {
                runnable.run();
            } finally {
                // schedule the next invocation
                schedule();
            }
        }
    };

    /**
     * Generates the next schedule.
     */
    private final Scheduler scheduler;

    /**
     * Task to execute.
     */
    private final Runnable runnable;

    public Task(Scheduler scheduler, Runnable runnable) {
        this.scheduler = scheduler;
        this.runnable = runnable;
        schedule();
    }

    void schedule() {
        timer.schedule(timerTask,scheduler.next());
    }

    public void cancel() {
        timerTask.cancel();
    }
}
