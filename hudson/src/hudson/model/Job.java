package hudson.model;

import com.thoughtworks.xstream.XStream;
import hudson.Util;
import hudson.XmlFile;
import hudson.util.TextFile;
import hudson.util.IOException2;
import hudson.tasks.LogRotator;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.regex.Pattern;

/**
 * A job is an runnable entity under the monitoring of Hudson.
 *
 * <p>
 * Every time it "runs", it will be recorded as a {@link Run} object.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Job<JobT extends Job<JobT,RunT>, RunT extends Run<JobT,RunT>>
        extends DirectoryHolder implements ModelObject {
    /**
     * Project name.
     */
    protected transient String name;

    /**
     * Project description. Can be HTML.
     */
    protected String description;

    /**
     * Root directory for this job.
     */
    protected transient File root;

    /**
     * Next bulid number.
     * Kept in a separate file because this is the only information
     * that gets updated often. This allows the rest of the configuration
     * to be in the VCS.
     */
    protected transient int nextBuildNumber = 1;
    private transient Hudson parent;

    private LogRotator logRotator;

    protected Job(Hudson parent,String name) {
        this.parent = parent;
        this.name = name;
        this.root = new File(new File(parent.root,"jobs"),name);
        this.root.mkdirs();
    }

    /**
     * Called when a {@link Job} is loaded from disk.
     */
    protected void onLoad(Hudson root, String name) throws IOException {
        this.parent = root;
        this.name = name;
        this.root = new File(new File(parent.root,"jobs"),name);

        TextFile f = getNextBuildNumberFile();
        if(f.exists()) {
            // starting 1.28, we store nextBuildNumber in a separate file.
            // but old Hudson didn't do it, so if the file doesn't exist,
            // assume that nextBuildNumber was read from config.xml
            try {
                this.nextBuildNumber = Integer.parseInt(f.readTrim());
            } catch (NumberFormatException e) {
                throw new IOException2(f+" doesn't contain a number",e);
            }
        } else {
            // this must be the old Hudson. create this file now.
            saveNextBuildNumber();
            save(); // and delete it from the config.xml
        }
    }

    private TextFile getNextBuildNumberFile() {
        return new TextFile(new File(this.root,"nextBuildNumber"));
    }

    private void saveNextBuildNumber() throws IOException {
        getNextBuildNumberFile().write(String.valueOf(nextBuildNumber)+'\n');
    }

    public final Hudson getParent() {
        return parent;
    }

    /**
     * Allocates a new buildCommand number.
     */
    public int assignBuildNumber() throws IOException {
        int r = nextBuildNumber++;
        saveNextBuildNumber();
        return r;
    }

    public int getNextBuildNumber() {
        return nextBuildNumber;
    }

    /**
     * Gets the project description HTML.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the project description HTML.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the log rotator for this job, or null if none.
     */
    public LogRotator getLogRotator() {
        return logRotator;
    }

    public void setLogRotator(LogRotator logRotator) {
        this.logRotator = logRotator;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns true if we should display "build now" icon
     */
    public abstract boolean isBuildable();

    /**
     * Gets all the builds.
     *
     * @return
     *      never null. The first entry is the latest buildCommand.
     */
    public synchronized Collection<? extends RunT> getBuilds() {
        return _getRuns().values();
    }

    /**
     * @deprecated
     *      This is only used to support backward compatibility with
     *      old URLs.
     */
    public synchronized RunT getBuild(String id) {
        for (RunT r : _getRuns().values()) {
            if(r.getId().equals(id))
                return r;
        }
        return null;
    }

    /**
     * @param n
     *      The build number.
     * @see Run#getNumber()
     */
    public synchronized RunT getBuildByNumber(int n) {
        return _getRuns().get(n);
    }

    public synchronized RunT getDynamic(String buildNumber, StaplerRequest req, StaplerResponse rsp) {
        return _getRuns().get(Integer.valueOf(buildNumber));
    }

    /**
     * The file we save our configuration.
     */
    protected static XmlFile getConfigFile(File dir) {
        return new XmlFile(XSTREAM,new File(dir,"config.xml"));
    }

    File getConfigFile() {
        return new File(root,"config.xml");
    }

    File getBuildDir() {
        return new File(root,"builds");
    }

    /**
     * Returns the URL of this project.
     */
    public String getUrl() {
        return "job/"+WHITESPACE_REPLACER.matcher(name).replaceAll("%20")+'/';
    }

    private static final Pattern WHITESPACE_REPLACER = Pattern.compile(" ", Pattern.LITERAL);


    /**
     * Gets all the runs.
     */
    protected abstract SortedMap<Integer,? extends RunT> _getRuns();

    /**
     * Called from {@link Run} to remove it from this job.
     *
     * The files are deleted already. So all the callee needs to do
     * is to remove a reference from this {@link Job}.
     */
    protected abstract void removeRun(Run run);

    /**
     * Returns the last build.
     */
    public synchronized RunT getLastBuild() {
        SortedMap<Integer,? extends RunT> runs = _getRuns();

        if(runs.isEmpty())    return null;
        return runs.get(runs.firstKey());
    }

    /**
     * Returns the last successful build, if any. Otherwise null.
     */
    public synchronized RunT getLastSuccessfulBuild() {
        RunT r = getLastBuild();
        while(r!=null && (r.isBuilding() || r.getResult().isWorseThan(Result.UNSTABLE)))
            r=r.getPreviousBuild();
        return r;
    }

    /**
     * Returns the last stable build, if any. Otherwise null.
     */
    public synchronized RunT getLastStableBuild() {
        RunT r = getLastBuild();
        while(r!=null && (r.isBuilding() || r.getResult().isWorseThan(Result.SUCCESS)))
            r=r.getPreviousBuild();
        return r;
    }

    /**
     * Returns the last failed build, if any. Otherwise null.
     */
    public synchronized RunT getLastFailedBuild() {
        RunT r = getLastBuild();
        while(r!=null && (r.isBuilding() || r.getResult()!=Result.FAILURE))
            r=r.getPreviousBuild();
        return r;
    }

    public synchronized String getIconColor() {
        RunT lastBuild = getLastBuild();
        while(lastBuild!=null && lastBuild.hasntStartedYet())
            lastBuild = lastBuild.getPreviousBuild();

        if(lastBuild!=null)
            return lastBuild.getIconColor();
        else
            return "grey";
    }


    /**
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        getConfigFile(root).write(this);
    }

    /**
     * Loads a project from a config file.
     */
    static Job load(Hudson root, File dir) throws IOException {
        Job job = (Job)getConfigFile(dir).read();
        job.onLoad(root,dir.getName());
        return job;
    }

    protected static final Comparator<Comparable> reverseComparator = new Comparator<Comparable>() {
        public int compare(Comparable o1, Comparable o2) {
            return -o1.compareTo(o2);
        }
    };



//
//
// actions
//
//
    /**
     * Accepts submission from the configuration page.
     */
    public synchronized void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        description = req.getParameter("description");

        if(req.getParameter("logrotate")!=null)
            logRotator = LogRotator.DESCRIPTOR.newInstance(req);
        else
            logRotator = null;

        save();
        rsp.sendRedirect(".");
    }

    /**
     * Returns the image that shows the current buildCommand status.
     */
    public void doBuildStatus( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        rsp.sendRedirect(req.getContextPath()+"/nocacheImages/48x48/"+getBuildStatusUrl());
    }

    public String getBuildStatusUrl() {
        return getIconColor()+".gif";
    }

    /**
     * Deletes this project.
     */
    public synchronized void doDoDelete( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        Util.deleteRecursive(root);
        getParent().deleteJob(this);
        rsp.sendRedirect(req.getContextPath()+"/");
    }

    /**
     * RSS feed for all runs.
     */
    public synchronized void doRssAll( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        List<Run> runs = new ArrayList<Run>(getBuilds());
        RSS.forwardToRss(this,getDisplayName()+" all builds",req,rsp,runs);
    }

    /**
     * RSS feed for failed runs.
     */
    public synchronized void doRssFailed( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        List<Run> runs = new ArrayList<Run>();
        for( Run r=getLastFailedBuild(); r!=null; r=r.getPreviousFailedBuild() )
            runs.add(r);
        RSS.forwardToRss(this,getDisplayName()+" all failures",req,rsp,runs);
    }

    private static final XStream XSTREAM = new XStream();

    static {
        XSTREAM.alias("project",Project.class);
    }
}
