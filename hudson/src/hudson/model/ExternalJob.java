package hudson.model;

import hudson.Util;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Job that runs outside Hudson.
 *
 * @author Kohsuke Kawaguchi
 */
public class ExternalJob extends Job<ExternalJob,ExternalRun> {

    /**
     * We occasionally update the list of {@link Run}s from a file system.
     * The next scheduled update time.
     */
    private transient long nextUpdate = 0;

    /**
     * Read-only map of all {@link ExternalRun}s. Copy-on-write semantics.
     */
    private transient volatile SortedMap<Integer,ExternalRun> runs = null;

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
    private static final LinkedHashSet<ExternalJob> reloadQueue = new LinkedHashSet<ExternalJob>();
    /*package*/ static final Thread reloadThread = new ReloadThread();
    static {
        reloadThread.start();
    }

    public ExternalJob(Hudson parent,String name) {
        super(parent,name);
        getBuildDir().mkdirs();
    }

    public boolean isBuildable() {
        return false;
    }

    protected SortedMap<Integer,ExternalRun> _getRuns() {
        if(runs==null) {
            synchronized(this) {
                if(runs==null) {
                    reload();   // if none is loaded yet, do so immediately
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

    private void reload() {
        try {
            TreeMap<Integer,ExternalRun> runs = new TreeMap<Integer,ExternalRun>(reverseComparator);

            File[] subdirs = getBuildDir().listFiles(new FileFilter() {
                public boolean accept(File subdir) {
                    return subdir.isDirectory();
                }
            });

            Arrays.sort(subdirs,fileComparator);
            ExternalRun lastBuild = null;

            for( File dir : subdirs ) {
                try {
                    ExternalRun b = new ExternalRun(this,dir,lastBuild);
                    lastBuild = b;
                    runs.put( b.getNumber(), b );
                } catch (IOException e) {
                    logger.log(Level.WARNING,"Unable to load "+dir,e);
                    e.printStackTrace();
                    try {
                        Util.deleteRecursive(dir);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        // but ignore
                    }
                }
            }

            // replace the list with new one atomically.
            this.runs = Collections.unmodifiableSortedMap(runs);

        } finally {
            reloadingInProgress = false;
            nextUpdate = System.currentTimeMillis()+1000;
        }
    }


    /**
     * Creates a new build of this project for immediate execution.
     *
     * Needs to be synchronized so that two {@link #newBuild()} invocations serialize each other.
     */
    public synchronized ExternalRun newBuild() throws IOException {
        ExternalRun run = new ExternalRun(this);
        SortedMap<Integer,ExternalRun> runs = new TreeMap<Integer,ExternalRun>(_getRuns());
        runs.put(run.getNumber(),run);
        this.runs = Collections.unmodifiableSortedMap(runs); // atomically replace the map with new one
        return run;
    }

    /**
     * Used to check if this is an external job and ready to accept a build result.
     */
    public void doAcceptBuildResult( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        rsp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Used to post the build result from a remote machine.
     */
    public void doPostBuildResult( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        ExternalRun run = newBuild();
        run.acceptRemoteSubmission(req.getReader());
        rsp.setStatus(HttpServletResponse.SC_OK);
    }


    private static final Comparator<File> fileComparator = new Comparator<File>() {
        public int compare(File lhs, File rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    };


    /**
     * Thread that reloads the {@link Run}s.
     */
    private static final class ReloadThread extends Thread {
        private ExternalJob getNext() throws InterruptedException {
            synchronized(reloadQueue) {
                while(reloadQueue.isEmpty())
                    reloadQueue.wait();
                ExternalJob job = reloadQueue.iterator().next();
                reloadQueue.remove(job);
                return job;
            }
        }

        public void run() {
            while (true) {
                try {
                    getNext().reload();
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

    private static final Logger logger = Logger.getLogger(ExternalJob.class.getName());

    public Descriptor<Job<ExternalJob,ExternalRun>> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor<Job<ExternalJob,ExternalRun>> DESCRIPTOR = new Descriptor<Job<ExternalJob,ExternalRun>>(ExternalJob.class) {
        public String getDisplayName() {
            return "Monitoring an external job";
        }

        public Job<ExternalJob,ExternalRun> newInstance(StaplerRequest req) throws FormException {
            // TODO
            // return new ExternalJob(Hudson.getInstance(),req.getParameter("name").trim());
            throw new UnsupportedOperationException();
        }
    };
}
