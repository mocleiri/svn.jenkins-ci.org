package hudson.model;

import com.thoughtworks.xstream.XStream;
import hudson.CloseProofOutputStream;
import hudson.Util;
import hudson.XmlFile;
import hudson.tasks.BuildStep;
import hudson.tasks.LogRotator;
import hudson.util.IOException2;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;

/**
 * A particular execution of {@link Job}.
 *
 * @author Kohsuke Kawaguchi
 */
public class Run <JobT extends Job,RunT extends Run>
        extends Actionable implements ModelObject {

    protected transient final JobT project;

    /**
     * Build number.
     *
     * Note that there's no guarantee that this is unique nor continuous.
     */
    public /*final*/ int number;

    /**
     * Previous build. Can be null.
     */
    protected transient final RunT previousBuild;

    protected transient RunT nextBuild;

    /**
     * When the build is scheduled.
     */
    protected transient final Calendar timestamp;

    /**
     * The build result.
     * null if a build is in progress.
     */
    protected Result result;

    /**
     * False if the build is scheduled but hasn't started yet.
     */
    protected transient boolean building;

    /**
     * Number of milli-seconds it took to run this build.
     */
    protected long duration;

    /**
     * Keeps this log entries.
     */
    private boolean keepLog;

    protected static final SimpleDateFormat ID_FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    protected static final SimpleDateFormat XS_DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {
        XS_DATETIME_FORMATTER.setTimeZone(new SimpleTimeZone(0,"GMT"));
    }

    /**
     * Creates a new {@link Run}.
     */
    protected Run(JobT job) throws IOException {
        this(job,(RunT)job.getLastBuild());
        this.number = project.assignBuildNumber();
        if(previousBuild!=null)
            previousBuild.nextBuild = this;
    }

    private Run(JobT job,RunT prevBuild) {
        this.project = job;
        this.previousBuild = prevBuild;
        this.timestamp = new GregorianCalendar();
    }

    /**
     * Loads a run from a log file.
     */
    Run(JobT project, File buildDir, RunT prevBuild ) throws IOException {
        this(project,prevBuild);
        if(prevBuild!=null)
            prevBuild.nextBuild = this;
        try {
            this.timestamp.setTime(ID_FORMATTER.parse(buildDir.getName()));
        } catch (ParseException e) {
            throw new IOException2("Invalid directory name "+buildDir,e);
        } catch (NumberFormatException e) {
            throw new IOException2("Invalid directory name "+buildDir,e);
        }
        this.building = true;
        getDataFile().unmarshal(this); // load the rest of the data
    }


    public final Result getResult() {
        return result;
    }

    /**
     * Returns true if the build is still in progress.
     */
    public boolean isBuilding() {
        return result==null;
    }

    /**
     * Gets the {@link Executor} building this job, if it's being built.
     * Otherwise null.
     */
    public Executor getExecutor() {
        for( Computer c : Hudson.getInstance().getComputers() ) {
            for (Executor e : c.getExecutors()) {
                if(e.getCurrentBuild()==(Object)this)
                    return e;
            }
        }
        return null;
    }

    /**
     * Returns true if this log file should be kept forever.
     *
     * This is used as a signal to the {@link LogRotator}.
     */
    public boolean isKeepLog() {
        return keepLog;
    }

    /**
     * The project this build is for.
     */
    public JobT getParent() {
        return project;
    }

    /**
     * When the build is scheduled.
     */
    public Calendar getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the string that says how long since this build has scheduled.
     *
     * @return
     *      string like "3 minutes" "1 day" etc.
     */
    public String getTimestampString() {
        long duration = new GregorianCalendar().getTimeInMillis()-timestamp.getTimeInMillis();
        return getTimeSpanString(duration);
    }

    /**
     * Returns the timestamp formatted in xs:dateTime.
     */
    public String getTimestampString2() {
        return XS_DATETIME_FORMATTER.format(timestamp.getTime());
    }

    /**
     * Gets the string that says how long the build took to run.
     */
    public String getDurationString() {
        return getTimeSpanString(duration);
    }

    /**
     * Gets the millisecond it took to build.
     */
    public long getDuration() {
        return duration;
    }

    private static String getTimeSpanString(long duration) {
        duration /= 1000;
        if(duration<60)
            return combine(duration,"second");
        duration /= 60;
        if(duration<60)
            return combine(duration,"minute");
        duration /= 60;
        if(duration<24)
            return combine(duration,"hour");
        duration /= 24;
        if(duration<30)
            return combine(duration,"day");
        duration /= 30;
        if(duration<12)
            return combine(duration,"month");
        duration /= 12;
        return combine(duration,"year");
    }

    private static String combine(long n, String suffix) {
        String s = Long.toString(n)+' '+suffix;
        if(n!=1)
            s += 's';
        return s;
    }

    /**
     * Gets the icon color for display.
     */
    public String getIconColor() {
        if(result!=null) {
            // already built
            if(result==Result.SUCCESS)
                return "blue";
            else
                return "red";
        }

        // a new build is in progress
        String baseColor;
        if(previousBuild==null)
            baseColor = "grey";
        else
            baseColor = previousBuild.getIconColor();

        return baseColor +"_anime";
    }

    /**
     * Returns true if the build is still queued and hasn't started yet.
     */
    public boolean hasntStartedYet() {
        return !building;
    }

    public String toString() {
        return project.getName()+" #"+number;
    }

    public String getDisplayName() {
        return "#"+number;
    }

    public int getNumber() {
        return number;
    }

    public RunT getPreviousBuild() {
        return previousBuild;
    }

    /**
     * Returns the last failed build before this build.
     */
    public RunT getPreviousFailedBuild() {
        Run r=previousBuild;
        while(  r!=null && r.getResult()!=Result.FAILURE )
            r=r.previousBuild;
        return (RunT)r;
    }

    public RunT getNextBuild() {
        return nextBuild;
    }

    // I really messed this up. I'm hoping to fix this some time
    // it shouldn't have trailing '/', and instead it should have leading '/'
    public String getUrl() {
        return project.getUrl()+"build/"+getId()+'/';
    }

    /**
     * Unique ID of this build.
     */
    public String getId() {
        return ID_FORMATTER.format(timestamp.getTime());
    }

    public File getRootDir() {
        File f = new File(project.getBuildDir(),getId());
        f.mkdirs();
        return f;
    }

    /**
     * Gets the directory where the artifacts are archived.
     */
    public File getArtifactsDir() {
        return new File(getRootDir(),"archive");
    }

    /**
     * Gets all the artifacts (relative to {@link #getArtifactsDir()}.
     */
    public List<Artifact> getArtifacts() {
        List<Artifact> r = new ArrayList<Artifact>();
        addArtifacts(getArtifactsDir(),"",r);
        return r;
    }

    /**
     * Returns true if this run has any artifacts.
     *
     * <p>
     * The strange method name is so that we can access it from EL.
     */
    public boolean getHasArtifacts() {
        return !getArtifacts().isEmpty();
    }

    private void addArtifacts( File dir, String path, List<Artifact> r ) {
        String[] children = dir.list();
        if(children==null)  return;
        for (String child : children) {
            File sub = new File(dir, child);
            if (sub.isDirectory()) {
                addArtifacts(sub, path + child + '/', r);
            } else {
                r.add(new Artifact(path + child));
            }
        }
    }

    /**
     * A build artifact.
     */
    public class Artifact {
        /**
         * Relative path name from {@link Run#getArtifactsDir()} 
         */
        private final String relativePath;

        private Artifact(String relativePath) {
            this.relativePath = relativePath;
        }

        /**
         * Gets the artifact file.
         */
        public File getFile() {
            return new File(getArtifactsDir(),relativePath);
        }

        /**
         * Returns just the file name portion, without the path.
         */
        public String getFileName() {
            return getFile().getName();
        }

        public String toString() {
            return relativePath;
        }
    }

    /**
     * Returns the log file.
     */
    public File getLogFile() {
        return new File(getRootDir(),"log");
    }

    /**
     * Deletes this build and its entire log
     *
     * @throws IOException
     *      if we fail to delete.
     */
    public synchronized void delete() throws IOException {
        File rootDir = getRootDir();
        File tmp = new File(rootDir.getParentFile(),'.'+rootDir.getName());

        if(!rootDir.renameTo(tmp))
            throw new IOException(rootDir+" is in use");

        Util.deleteRecursive(tmp);

        getParent().removeRun(this);
    }

    protected static interface Runner {
        Result run( BuildListener listener ) throws Exception;

        void post( BuildListener listener );
    }

    protected final void run(Runner job) {
        if(result!=null)
            return;     // already built.

        building = true;

        long start = System.currentTimeMillis();

        BuildListener listener=null;
        try {
            final PrintStream log = new PrintStream(new FileOutputStream(getLogFile()));
            listener = new BuildListener() {
                final PrintWriter pw = new PrintWriter(new CloseProofOutputStream(log),true);

                public void started() {}

                public PrintStream getLogger() {
                    return log;
                }

                public PrintWriter error(String msg) {
                    pw.println("ERROR: "+msg);
                    return pw;
                }

                public PrintWriter fatalError(String msg) {
                    return error(msg);
                }

                public void finished(Result result) {
                    pw.close();
                    log.close();
                }
            };

            listener.started();

            result = job.run(listener);

            job.post(listener);

        } catch( Exception e ) {
            if(listener!=null) {
                if(e instanceof IOException)
                    Util.displayIOException((IOException)e,listener);

                Writer w = listener.fatalError(e.getMessage());
                if(w!=null) {
                    try {
                        e.printStackTrace(new PrintWriter(w));
                        w.close();
                    } catch (IOException e1) {
                        // ignore
                    }
                }
            }
            result = Result.FAILURE;
        }

        long end = System.currentTimeMillis();
        duration = end-start;

        listener.finished(result);

        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            LogRotator lr = getParent().getLogRotator();
            if(lr!=null)
                lr.perform(getParent());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        getDataFile().write(this);
    }

    private XmlFile getDataFile() {
        XStream xs = new XStream();
        xs.alias("build",Build.class);
        xs.registerConverter(Result.conv);
        return new XmlFile(xs,new File(getRootDir(),"build.xml"));
    }

    /**
     * Gets the log of the build as a string.
     *
     * I know, this isn't terribly efficient!
     */
    public String getLog() throws IOException {
        File logfile = getLogFile();
        if(!logfile.exists())
            return "";

        StringBuffer str = new StringBuffer((int)logfile.length());

        BufferedReader r = new BufferedReader(new FileReader(logfile));
        char[] buf = new char[1024];
        int len;
        while((len=r.read(buf,0,buf.length))>0)
           str.append(buf,0,len);
        r.close();

        return str.toString();
    }

    public void doBuildStatus( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        rsp.sendRedirect(req.getContextPath()+"/images/48x48/"+getBuildStatusUrl());
    }

    public String getBuildStatusUrl() {
        return getIconColor()+".gif";
    }

    public void doArtifact( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        String path = req.getRestOfPath();
        if(path.length()==0) {
            req.getView(this,"artifacts.jsp").forward(req,rsp);
            return;
        }
        if(path.indexOf("..")!=-1 || path.length()<1) {
            // don't serve anything other than files in the artifacts dir
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        File f = new File(getArtifactsDir(),path.substring(1));
        if(!f.exists()) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        FileInputStream in = new FileInputStream(f);
        // serve the file
        rsp.setContentType(req.getServletContext().getMimeType(req.getServletPath()));
        rsp.setContentLength((int)f.length());
        byte[] buf = new byte[1024];
        int len;
        while((len=in.read(buf))>0)
            rsp.getOutputStream().write(buf,0,len);
        in.close();
    }

    /**
     * Returns the build number in the body.
     */
    public void doBuildNumber( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        rsp.setContentType("text/plain");
        rsp.setCharacterEncoding("US-ASCII");
        rsp.setStatus(HttpServletResponse.SC_OK);
        rsp.getWriter().print(number);
    }

    public void doToggleLogKeep( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        keepLog = !keepLog;
        save();
        rsp.forwardToPreviousPage(req);
    }

    /**
     * Returns the map that contains environmental variables for this build.
     *
     * Used by {@link BuildStep}s that invoke external processes.
     */
    public Map<String,String> getEnvVars() {
        Map<String,String> env = new HashMap<String,String>();
        env.put("BUILD_NUMBER",String.valueOf(number));
        env.put("BUILD_ID",getId());
        env.put("BUILD_TAG","hudson-"+getParent().getName()+"-"+number);
        env.put("JOB_NAME",getParent().getName());
        return env;
    }
}
