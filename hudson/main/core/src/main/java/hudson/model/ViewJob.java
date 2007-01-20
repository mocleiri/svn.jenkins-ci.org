package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.SortedMap;

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
     * All {@link Run}s. Copy-on-write semantics.
     */
    protected transient /*almost final*/ RunMap<RunT> runs = new RunMap<RunT>();

    private transient boolean notLoaded = true;

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
        super(name);
    }

    public boolean isBuildable() {
        return false;
    }


    public void onLoad(String name) throws IOException {
        super.onLoad(name);
        notLoaded = true;
    }

    protected SortedMap<Integer,RunT> _getRuns() {
        if(notLoaded || runs==null) {
            // if none is loaded yet, do so immediately.
            synchronized(this) {
                if(runs==null)
                    runs = new RunMap<RunT>();
                if(notLoaded) {
                    notLoaded = false;
                    _reload();   
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

    public void removeRun(RunT run) {
        // reload the info next time
        nextUpdate = 0;
    }

    private void _reload() {
        try {
            reload();
        } finally {
            reloadingInProgress = false;
            nextUpdate = System.currentTimeMillis()+1000*60;
        }
    }

    /**
     * Reloads the list of {@link Run}s. This operation can take a long time.
     *
     * <p>
     * The loaded {@link Run}s should be set to {@link #runs}.
     */
    protected abstract void reload();

    public void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        super.doConfigSubmit(req,rsp);
        // make sure to reload to reflect this config change.
        nextUpdate = 0;
    }


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
