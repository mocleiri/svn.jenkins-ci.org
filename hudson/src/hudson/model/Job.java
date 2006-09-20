package hudson.model;

import com.thoughtworks.xstream.XStream;
import hudson.Util;
import hudson.XmlFile;
import hudson.tasks.BuildTrigger;
import hudson.tasks.LogRotator;
import hudson.util.IOException2;
import hudson.util.TextFile;
import hudson.util.XStream2;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;

/**
 * A job is an runnable entity under the monitoring of Hudson.
 *
 * <p>
 * Every time it "runs", it will be recorded as a {@link Run} object.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Job<JobT extends Job<JobT,RunT>, RunT extends Run<JobT,RunT>>
        extends DirectoryHolder {
    /**
     * Project name.
     */
    protected /*final*/ transient String name;

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
        doSetName(name);
        this.root.mkdirs();
    }

    /**
     * Called when a {@link Job} is loaded from disk.
     */
    protected void onLoad(Hudson root, String name) throws IOException {
        this.parent = root;
        doSetName(name);

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

    /**
     * Just update {@link #name} and {@link #root}, since they are linked.
     */
    private void doSetName(String name) {
        this.name = name;
        this.root = new File(new File(parent.root,"jobs"),name);
    }

    public File getRootDir() {
        return root;
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

    public boolean isInQueue() {
        return false;
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

    /**
     * Renames a job.
     */
    public void renameTo(String newName) throws IOException {
        // always synchronize from bigger objects first
        synchronized(parent) {
            synchronized(this) {
                // sanity check
                if(newName==null)
                    throw new IllegalArgumentException("New name is not given");
                if(parent.getJob(newName)!=null)
                    throw new IllegalArgumentException("Job "+newName+" already exists");

                // noop?
                if(this.name.equals(newName))
                    return;


                String oldName = this.name;
                File oldRoot = this.root;

                doSetName(newName);
                File newRoot = this.root;

                {// rename data files
                    boolean interrupted=false;
                    boolean renamed = false;

                    // try to rename the job directory.
                    // this may fail on Windows due to some other processes accessing a file.
                    // so retry few times before we fall back to copy.
                    for( int retry=0; retry<5; retry++ ) {
                        if(oldRoot.renameTo(newRoot)) {
                            renamed = true;
                            break; // succeeded
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            // process the interruption later
                            interrupted = true;
                        }
                    }

                    if(interrupted)
                        Thread.currentThread().interrupt();

                    if(!renamed) {
                        // failed to rename. it must be that some lengthy process is going on
                        // to prevent a rename operation. So do a copy. Ideally we'd like to
                        // later delete the old copy, but we can't reliably do so, as before the VM
                        // shuts down there might be a new job created under the old name.
                        Copy cp = new Copy();
                        cp.setProject(new org.apache.tools.ant.Project());
                        cp.setTodir(newRoot);
                        FileSet src = new FileSet();
                        src.setDir(getRootDir());
                        cp.addFileset(src);
                        cp.setOverwrite(true);
                        cp.setPreserveLastModified(true);
                        cp.setFailOnError(false);   // keep going even if there's an error
                        cp.execute();

                        // try to delete as much as possible
                        try {
                            Util.deleteRecursive(oldRoot);
                        } catch (IOException e) {
                            // but ignore the error, since we expect that
                            e.printStackTrace();
                        }
                    }
                }

                parent.onRenamed(this,oldName,newName);

                // update BuildTrigger of other projects that point to this object.
                // can't we generalize this?
                for( Project p : parent.getProjects() ) {
                    BuildTrigger t = (BuildTrigger) p.getPublishers().get(BuildTrigger.DESCRIPTOR);
                    if(t!=null) {
                        if(t.onJobRenamed(oldName,newName))
                            p.save();
                    }
                }
            }
        }
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
    public List<RunT> getBuilds() {
        return new ArrayList<RunT>(_getRuns().values());
    }

    /**
     * @deprecated
     *      This is only used to support backward compatibility with
     *      old URLs.
     */
    public RunT getBuild(String id) {
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
    public RunT getBuildByNumber(int n) {
        return _getRuns().get(n);
    }

    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        try {
            // try to interpret the token as build number
            return _getRuns().get(Integer.valueOf(token));
        } catch (NumberFormatException e) {
            return super.getDynamic(token,req,rsp);
        }
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
        return "job/"+name+'/';
    }

    /**
     * Gets all the runs.
     *
     * The resulting map must be immutable (by employing copy-on-write semantics.)
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
    public RunT getLastBuild() {
        SortedMap<Integer,? extends RunT> runs = _getRuns();

        if(runs.isEmpty())    return null;
        return runs.get(runs.firstKey());
    }

    /**
     * Returns the oldest build in the record.
     */
    public RunT getFirstBuild() {
        SortedMap<Integer,? extends RunT> runs = _getRuns();

        if(runs.isEmpty())    return null;
        return runs.get(runs.lastKey());
    }

    /**
     * Returns the last successful build, if any. Otherwise null.
     */
    public RunT getLastSuccessfulBuild() {
        RunT r = getLastBuild();
        // temporary hack till we figure out what's causing this bug
        while(r!=null && (r.isBuilding() || r.getResult()==null || r.getResult().isWorseThan(Result.UNSTABLE)))
            r=r.getPreviousBuild();
        return r;
    }

    /**
     * Returns the last stable build, if any. Otherwise null.
     */
    public RunT getLastStableBuild() {
        RunT r = getLastBuild();
        while(r!=null && (r.isBuilding() || r.getResult().isWorseThan(Result.SUCCESS)))
            r=r.getPreviousBuild();
        return r;
    }

    /**
     * Returns the last failed build, if any. Otherwise null.
     */
    public RunT getLastFailedBuild() {
        RunT r = getLastBuild();
        while(r!=null && (r.isBuilding() || r.getResult()!=Result.FAILURE))
            r=r.getPreviousBuild();
        return r;
    }

    /**
     * Used as the color of the status ball for the project.
     */
    public String getIconColor() {
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
    public synchronized void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        req.setCharacterEncoding("UTF-8");

        description = req.getParameter("description");

        if(req.getParameter("logrotate")!=null)
            logRotator = LogRotator.DESCRIPTOR.newInstance(req);
        else
            logRotator = null;

        save();

        String newName = req.getParameter("name");
        if(newName!=null && !newName.equals(name)) {
            rsp.sendRedirect("rename?newName="+newName);
        } else {
            rsp.sendRedirect(".");
        }
    }

    /**
     * Returns the image that shows the current buildCommand status.
     */
    public void doBuildStatus( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        rsp.sendRedirect2(req.getContextPath()+"/nocacheImages/48x48/"+getBuildStatusUrl());
    }

    public String getBuildStatusUrl() {
        return getIconColor()+".gif";
    }

    /**
     * Deletes this job.
     */
    public synchronized void doDoDelete( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        Util.deleteRecursive(root);
        getParent().deleteJob(this);
        rsp.sendRedirect2(req.getContextPath()+"/");
    }

    /**
     * Renames this job.
     */
    public /*not synchronized. see renameTo()*/ void doDoRename( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        String newName = req.getParameter("newName");

        renameTo(newName);
        rsp.sendRedirect2(req.getContextPath()+'/'+getUrl()); // send to the new job page
    }

    /**
     * RSS feed for all runs.
     */
    public synchronized void doRssAll( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        List<RunT> runs = getBuilds();
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

    private static final XStream XSTREAM = new XStream2();

    static {
        XSTREAM.alias("project",Project.class);
    }
}
