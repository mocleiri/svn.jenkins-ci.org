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
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.logging.Level;

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

    private transient SortedMap<Integer,ExternalRun> runs = null;

    /**
     * If the reloading of runs are in progress (in another thread,
     * set to true.)
     */
    private transient boolean reloadingInProgress;

    /**
     * {@link ExternalJob}s that need to be reloaded.
     */
    private static final List<ExternalJob> reloadQueue = new LinkedList<ExternalJob>();
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

    protected synchronized SortedMap<Integer,ExternalRun> _getRuns() {
        if(runs==null) {
            reload();   // if none is loaded yet, do so immediately
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
            this.runs = runs;

        } finally {
            reloadingInProgress = false;
            nextUpdate = System.currentTimeMillis()+1000;
        }
    }


    /**
     * Creates a new build of this project for immediate execution.
     */
    public synchronized ExternalRun newBuild() throws IOException {
        ExternalRun run = new ExternalRun(this);
        _getRuns().put(run.getNumber(),run);
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
                return reloadQueue.remove(0);
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
}
