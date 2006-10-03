package hudson.plugins.jprt;

import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.ViewJob;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.FileFilter;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.ParseException;

/**
 * {@link Job} that monitors a remote JPRT system.
 *
 * @author Kohsuke Kawaguchi
 */
public class JPRTJob extends ViewJob<JPRTJob,JPRTRun> {

    /**
     * Path to the JPRT archive root directory.
     */
    private volatile File archiveRoot;

    public JPRTJob(Hudson parent, String name) {
        super(parent, name);
    }

    protected TreeMap<Integer,JPRTRun> reload() {
        // TODO: what about the queue and on-going builds?

        TreeMap<Integer,JPRTRun> runs = new TreeMap<Integer,JPRTRun>();

        File[] dirs = archiveRoot.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() && f.getName().length()>18;
            }
        });
        if(dirs==null)     return runs;

        JPRTRun last = null;
        for (File dir : dirs) {
            try {
                last = new JPRTRun(this,last,dir);
                runs.put( last.getNumber(), last );
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to load "+dir,e);
            }
        }

        return runs;
    }

    /**
     * Accepts submission from the configuration page.
     */
    @Override
    public void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        archiveRoot = new File(req.getParameter("archiveRoot"));
        if(!archiveRoot.isDirectory()) {
            sendError(archiveRoot+" is not a directory",req,rsp);
            return;
        }

        super.doConfigSubmit(req,rsp);
    }


    public Descriptor<Job<JPRTJob,JPRTRun>> getDescriptor() {
        return DESCRIPTOR;
    }

    static final Descriptor<Job<JPRTJob,JPRTRun>> DESCRIPTOR = new Descriptor<Job<JPRTJob,JPRTRun>>(JPRTJob.class) {
        public String getDisplayName() {
            return "Monitor a JPRT system";
        }

        public Job<JPRTJob,JPRTRun> newInstance(StaplerRequest req) throws FormException {
            // TODO
            throw new UnsupportedOperationException();
        }
    };

    static {
        Job.XSTREAM.alias("jprt",JPRTJob.class);
    }

    private static final Logger logger = Logger.getLogger(JPRTJob.class.getName());
}
