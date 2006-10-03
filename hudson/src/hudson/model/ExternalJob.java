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
import java.util.Collections;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Job that runs outside Hudson whose result is submitted to Hudson
 * (either via web interface, or simply by placing files on the file system,
 * for compatibility.)
 *
 * @author Kohsuke Kawaguchi
 */
public class ExternalJob extends ViewJob<ExternalJob,ExternalRun> {
    public ExternalJob(Hudson parent,String name) {
        super(parent,name);
    }

    @Override
    protected TreeMap<Integer,ExternalRun> reload() {
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

        return runs;
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

    private static final Logger logger = Logger.getLogger(ExternalJob.class.getName());

    public JobDescriptor<ExternalJob,ExternalRun> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final JobDescriptor<ExternalJob,ExternalRun> DESCRIPTOR = new JobDescriptor<ExternalJob,ExternalRun>(ExternalJob.class) {
        public String getDisplayName() {
            return "Monitoring an external job";
        }

        public ExternalJob newInstance(String name) {
            return new ExternalJob(Hudson.getInstance(),name);
        }
    };
}
