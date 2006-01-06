package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.GregorianCalendar;
import java.util.Calendar;

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
     * Creates a job in this collection.
     */
    public abstract Job doCreateJob( StaplerRequest req, StaplerResponse rsp ) throws IOException;

    /**
     * RSS feed for all runs.
     */
    public synchronized void doRssAll( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        SortedSet<Run> runs = new TreeSet<Run>(runComparator);
        for( Job j : getJobs() )
            runs.addAll( j.getBuilds() );

        forwardToRss(this,"Hudson all builds",req,rsp,runs);
    }

    /**
     * RSS feed for failed runs.
     */
    public synchronized void doRssFailed( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        SortedSet<Run> runs = new TreeSet<Run>(runComparator);
        for( Job j : getJobs() )
            runs.addAll( j.getBuilds() );

        for (Iterator<Run> i = runs.iterator(); i.hasNext();) {
            if(i.next().getResult()!=Result.FAILURE)
                i.remove();
        }

        forwardToRss(this,"Hudson all failures", req,rsp,runs);
    }

    /**
     * Sends the RSS feed to the client.
     */
    static void forwardToRss( Object it, String title, StaplerRequest req, HttpServletResponse rsp, Collection<Run> runs) throws IOException, ServletException {
        GregorianCalendar threshold = new GregorianCalendar();
        threshold.add(Calendar.DAY_OF_YEAR,-7);

        int count=0;

        for (Iterator<Run> i = runs.iterator(); i.hasNext();) {
            // at least put 10 items
            if(count<10) {
                i.next();
                count++;
                continue;
            }
            // anything older than 7 days will be ignored
            if(i.next().getTimestamp().before(threshold))
                i.remove();
        }

        req.setAttribute("it",it);
        req.setAttribute("title",title);
        req.setAttribute("runs",runs);
        req.getServletContext().getRequestDispatcher("/WEB-INF/rss.jsp").forward(req,rsp);
    }

    public static final Comparator<JobCollection> SORTER = new Comparator<JobCollection>() {
        public int compare(JobCollection lhs, JobCollection rhs) {
            return lhs.getViewName().compareTo(rhs.getViewName());
        }
    };

    private static final Comparator<Run> runComparator = new Comparator<Run>() {
        public int compare(Run lhs, Run rhs) {
            long r = lhs.getTimestamp().getTimeInMillis() - rhs.getTimestamp().getTimeInMillis();
            if(r<0)     return +1;
            if(r>0)     return -1;
            return lhs.getParent().getName().compareTo(rhs.getParent().getName());
        }
    };
}
