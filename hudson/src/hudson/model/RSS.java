package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * RSS related code.
 *
 * @author Kohsuke Kawaguchi
 */
final class RSS {
    /**
     * RSS feed for all runs.
     */
    public static void doRssAll( Object it, Collection<? extends Job> jobs, StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        SortedSet<Run> runs = new TreeSet<Run>(runComparator);
        for( Job j : jobs )
            runs.addAll( j.getBuilds() );

        forwardToRss(it,"Hudson all builds",req,rsp,runs);
    }

    /**
     * RSS feed for failed runs.
     */
    public static void doRssFailed( Object it, Collection<? extends Job> jobs, StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        SortedSet<Run> runs = new TreeSet<Run>(runComparator);
        for( Job j : jobs )
            runs.addAll( j.getBuilds() );

        for (Iterator<Run> i = runs.iterator(); i.hasNext();) {
            if(i.next().getResult()!=Result.FAILURE)
                i.remove();
        }

        forwardToRss(it,"Hudson all failures", req,rsp,runs);
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

    private static final Comparator<Run> runComparator = new Comparator<Run>() {
        public int compare(Run lhs, Run rhs) {
            long r = lhs.getTimestamp().getTimeInMillis() - rhs.getTimestamp().getTimeInMillis();
            if(r<0)     return +1;
            if(r>0)     return -1;
            return lhs.getParent().getName().compareTo(rhs.getParent().getName());
        }
    };
}
