package hudson.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * {@link Job} that monitors activities that happen outside Hudson,
 * which requires occasional batch reload activity to obtain the up-to-date information.
 *
 * <p>
 * This can be used as a base class to derive custom {@link Job} type.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ViewJob<JobT extends ViewJob<JobT,RunT>, RunT extends Run<JobT,RunT>>
    extends Job<JobT,RunT> {

    /**
     * We occasionally update the list of {@link Run}s from a file system.
     * The next scheduled update time.
     */
    private transient long nextUpdate = 0;

    /**
     * Read-only map of all {@link Run}s. Copy-on-write semantics.
     */
    protected transient volatile SortedMap<Integer,RunT> runs = null;

    /**
     * If the reloading of runs are in progress (in another thread,
     * set to true.)
     */
    private transient volatile boolean reloadingInProgress;

    /**
     * {@link ExternalJob}s that need to be reloaded.
     *
     * This is a set, so no {@link ExternalJob}s are scheduled twice, yet
     * it's order is predictable, avoiding starvation.
     */
    private static final LinkedHashSet<ViewJob> reloadQueue = new LinkedHashSet<ViewJob>();
    /*package*/ static final Thread reloadThread = new ReloadThread();
    static {
        reloadThread.start();
    }

    protected ViewJob(Hudson parent, String name) {
        super(parent, name);
    }

    public boolean isBuildable() {
        return false;
    }

    protected SortedMap<Integer,RunT> _getRuns() {
        if(runs==null) {
            synchronized(this) {
                if(runs==null) {
                    _reload();   // if none is loaded yet, do so immediately
                }
            }
        }
        if(nextUpdate<System.currentTimeMillis()) {
            if(!reloadingInProgress) {
                // schedule a new reloading operation.
                // we don't want to block the current thread,
                // so reloading is done asynchronously.
                reloadingInProgress = true;
                synchronized(reloadQueue) {
                    reloadQueue.add(this);
                    reloadQueue.notify();
                }
            }
        }
        return runs;
    }

    public void removeRun(Run run) {
        // reload the info next time
        nextUpdate = 0;
    }

    private void _reload() {
        try {
            // replace the list with new one atomically.
            this.runs = Collections.unmodifiableSortedMap(reload());
        } finally {
            reloadingInProgress = false;
            nextUpdate = System.currentTimeMillis()+1000;
        }
    }

    /**
     * Reloads the list of {@link Run}s. This operation can take a long time.
     */
    protected abstract TreeMap<Integer,RunT> reload();


    /**
     * Thread that reloads the {@link Run}s.
     */
    private static final class ReloadThread extends Thread {
        private ViewJob getNext() throws InterruptedException {
            synchronized(reloadQueue) {
                while(reloadQueue.isEmpty())
                    reloadQueue.wait();
                ViewJob job = reloadQueue.iterator().next();
                reloadQueue.remove(job);
                return job;
            }
        }

        public void run() {
            while (true) {
                try {
                    getNext()._reload();
                } catch (InterruptedException e) {
                    // treat this as a death signal
                    return;
                } catch (Throwable t) {
                    // otherwise ignore any error
                    t.printStackTrace();
                }
            }
        }
    }

    // private static final Logger logger = Logger.getLogger(ViewJob.class.getName());
}
