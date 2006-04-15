package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;

/**
 * Collection of {@link Job}s.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class JobCollection implements ModelObject {

    /**
     * Gets all the jobs in this collection.
     */
    public abstract Collection<Job> getJobs();

    /**
     * Checks if the job is in this collection.
     */
    public abstract boolean containsJob(Job job);

    /**
     * Gets the name of all this collection.
     */
    public abstract String getViewName();

    /**
     * Message displayed in the top page. Can be null. Includes HTML.
     */
    public abstract String getViewMessage();


    /**
     * Creates a job in this collection.
     *
     * @return
     *      null if fails.
     */
    public abstract Job doCreateJob( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException;

    public synchronized void doRssAll( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        RSS.doRssAll(this, getJobs(), req, rsp );
    }
    public synchronized void doRssFailed( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        RSS.doRssFailed(this, getJobs(), req, rsp );
    }

    /**
     * Displays the error in a page.
     */
    protected final void sendError(Exception e, StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        sendError(e.getMessage(),req,rsp);
    }

    protected final void sendError(String message, StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        req.setAttribute("message",message);
        rsp.forward(this,"error",req);
    }

    public static final Comparator<JobCollection> SORTER = new Comparator<JobCollection>() {
        public int compare(JobCollection lhs, JobCollection rhs) {
            return lhs.getViewName().compareTo(rhs.getViewName());
        }
    };

}
