package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import hudson.Util;

/**
 * Represents a collection of {@link Job}s.
 *
 * @author Kohsuke Kawaguchi
 */
public class View extends JobCollection {

    private final Hudson owner;

    /**
     * List of job names. This is what gets serialized.
     */
    /*package*/ final Set<String> jobNames = new TreeSet<String>();

    /**
     * Name of this view.
     */
    private String name;

    /**
     * Message displayed in the view page.
     */
    private String description;


    public View(Hudson owner, String name) {
        this.name = name;
        this.owner = owner;
    }

    /**
     * Returns a read-only view of all {@link Job}s in this view.
     *
     * <p>
     * This method returns a separate copy each time to avoid
     * concurrent modification issue.
     */
    public synchronized List<Job> getJobs() {
        Job[] jobs = new Job[jobNames.size()];
        int i=0;
        for (String name : jobNames)
            jobs[i++] = owner.getJob(name);
        return Arrays.asList(jobs);
    }

    public boolean containsJob(Job job) {
        return jobNames.contains(job.getName());
    }

    public String getViewName() {
        return name;
    }

    public String getViewMessage() {
        return description;
    }

    public String getDisplayName() {
        return name;
    }

    public Job doCreateJob(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        if(!Hudson.adminCheck(req,rsp))
            return null;

        Job job = owner.doCreateJob(req, rsp);
        if(job!=null) {
            jobNames.add(job.getName());
            owner.save();
        }
        return job;
    }

    public String getUrl() {
        return "view/"+name+'/';
    }

    /**
     * Accepts submission from the configuration page.
     */
    public synchronized void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        jobNames.clear();
        for (Job job : owner.getJobs()) {
            if(req.getParameter(job.getName())!=null)
                jobNames.add(job.getName());
        }

        description = Util.nullify(req.getParameter("description"));

        owner.save();

        rsp.sendRedirect(".");
    }

    /**
     * Deletes this view.
     */
    public synchronized void doDoDelete( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        owner.deleteView(this);
        rsp.sendRedirect(req.getContextPath()+"/");
    }
}
