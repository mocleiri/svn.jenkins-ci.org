package hudson.model;

import com.thoughtworks.xstream.XStream;
import hudson.CloseProofOutputStream;
import hudson.EnvVars;
import hudson.ExtensionPoint;
import hudson.FeedAdapter;
import hudson.FilePath;
import hudson.Util;
import static hudson.Util.combine;
import hudson.XmlFile;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.listeners.RunListener;
import hudson.search.SearchIndexBuilder;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.LogRotator;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.IOException2;
import hudson.util.XStream2;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.framework.io.LargeText;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A particular execution of {@link Job}.
 *
 * <p>
 * Custom {@link Run} type is always used in conjunction with
 * a custom {@link Job} type, so there's no separate registration
 * mechanism for custom {@link Run} types.
 *
 * @author Kohsuke Kawaguchi
 * @see RunListener
 */
@ExportedBean
public abstract class Run <JobT extends Job<JobT,RunT>,RunT extends Run<JobT,RunT>>
        extends Actionable implements ExtensionPoint, Comparable<RunT>, AccessControlled {

    protected transient final JobT project;

    /**
     * Build number.
     *
     * <p>
     * In earlier versions &lt; 1.24, this number is not unique nor continuous,
     * but going forward, it will, and this really replaces the build id.
     */
    public /*final*/ int number;

    /**
     * Previous build. Can be null.
     * These two fields are maintained and updated by {@link RunMap}.
     */
    protected volatile transient RunT previousBuild;
    /**
     * Next build. Can be null.
     */
    protected volatile transient RunT nextBuild;

    /**
     * When the build is scheduled.
     */
    protected transient final long timestamp;

    /**
     * The build result.
     * This value may change while the state is in {@link State#BUILDING}.
     */
    protected volatile Result result;

    /**
     * Human-readable description. Can be null.
     */
    protected volatile String description;

    /**
     * The current build state.
     */
    protected volatile transient State state;

    private static enum State {
        /**
         * Build is created/queued but we haven't started building it.
         */
        NOT_STARTED,
        /**
         * Build is in progress.
         */
        BUILDING,
        /**
         * Build is completed now, and the status is determined,
         * but log files are still being updated.
         */
        POST_PRODUCTION,
        /**
         * Build is completed now, and log file is closed.
         */
        COMPLETED
    }

    /**
     * Number of milli-seconds it took to run this build.
     */
    protected long duration;

    /**
     * Keeps this log entries.
     */
    private boolean keepLog;

    protected static final ThreadLocal<SimpleDateFormat> ID_FORMATTER =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                }
            };

    /**
     * Creates a new {@link Run}.
     */
    protected Run(JobT job) throws IOException {
        this(job, new GregorianCalendar());
        this.number = project.assignBuildNumber();
    }

    /**
     * Constructor for creating a {@link Run} object in
     * an arbitrary state.
     */
    protected Run(JobT job, Calendar timestamp) {
        this(job,timestamp.getTimeInMillis());
    }

    protected Run(JobT job, long timestamp) {
        this.project = job;
        this.timestamp = timestamp;
        this.state = State.NOT_STARTED;
    }

    /**
     * Loads a run from a log file.
     */
    protected Run(JobT project, File buildDir) throws IOException {
        this(project, parseTimestampFromBuildDir(buildDir));
        this.state = State.COMPLETED;
        this.result = Result.FAILURE;  // defensive measure. value should be overwritten by unmarshal, but just in case the saved data is inconsistent
        getDataFile().unmarshal(this); // load the rest of the data
    }

    private static long parseTimestampFromBuildDir(File buildDir) throws IOException {
        try {
            return ID_FORMATTER.get().parse(buildDir.getName()).getTime();
        } catch (ParseException e) {
            throw new IOException2("Invalid directory name "+buildDir,e);
        } catch (NumberFormatException e) {
            throw new IOException2("Invalid directory name "+buildDir,e);
        }
    }

    /**
     * Ordering based on build numbers.
     */
    public int compareTo(RunT that) {
        return this.number - that.number;
    }

    /**
     * Returns the build result.
     *
     * <p>
     * When a build is {@link #isBuilding() in progress}, this method
     * may return null or a temporary intermediate result.
     */
    @Exported
    public Result getResult() {
        return result;
    }

    public void setResult(Result r) {
        // state can change only when we are building
        assert state==State.BUILDING;

        StackTraceElement caller = findCaller(Thread.currentThread().getStackTrace(),"setResult");


        // result can only get worse
        if(result==null) {
            result = r;
            LOGGER.fine(toString()+" : result is set to "+r+" by "+caller);
        } else {
            if(r.isWorseThan(result)) {
                LOGGER.fine(toString()+" : result is set to "+r+" by "+caller);
                result = r;
            }
        }
    }

    /**
     * Gets the subset of {@link #getActions()} that consists of {@link BuildBadgeAction}s.
     */
    public List<BuildBadgeAction> getBadgeActions() {
        List<BuildBadgeAction> r = null;
        for (Action a : getActions()) {
            if(a instanceof BuildBadgeAction) {
                if(r==null)
                    r = new ArrayList<BuildBadgeAction>();
                r.add((BuildBadgeAction)a);
            }
        }
        if(isKeepLog()) {
            if(r==null)
                r = new ArrayList<BuildBadgeAction>();
            r.add(new KeepLogBuildBadge());
        }
        if(r==null)     return Collections.emptyList();
        else            return r;
    }

    private StackTraceElement findCaller(StackTraceElement[] stackTrace, String callee) {
        for(int i=0; i<stackTrace.length-1; i++) {
            StackTraceElement e = stackTrace[i];
            if(e.getMethodName().equals(callee))
                return stackTrace[i+1];
        }
        return null; // not found
    }

    /**
     * Returns true if the build is not completed yet.
     * This includes "not started yet" state.
     */
    @Exported
    public boolean isBuilding() {
        return state.compareTo(State.POST_PRODUCTION) < 0;
    }

    /**
     * Returns true if the log file is still being updated.
     */
    public boolean isLogUpdated() {
        return state.compareTo(State.COMPLETED) < 0;
    }

    /**
     * Gets the {@link Executor} building this job, if it's being built.
     * Otherwise null.
     */
    public Executor getExecutor() {
        for( Computer c : Hudson.getInstance().getComputers() ) {
            for (Executor e : c.getExecutors()) {
                if(e.getCurrentExecutable()==this)
                    return e;
            }
        }
        return null;
    }

    /**
     * Returns true if this log file should be kept and not deleted.
     *
     * This is used as a signal to the {@link LogRotator}.
     */
    @Exported
    public final boolean isKeepLog() {
        return getWhyKeepLog()!=null;
    }

    /**
     * If {@link #isKeepLog()} returns true, returns a human readable
     * one-line string that explains why it's being kept.
     */
    public String getWhyKeepLog() {
        if(keepLog)
            return Messages.Run_MarkedExplicitly();
        return null;    // not marked at all
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
    @Exported
    public Calendar getTimestamp() {
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(timestamp);
        return c;
    }

    @Exported
    public String getDescription() {
        return description;
    }

    /**
     * Returns the length-limited description.
     * @return The length-limited description.
     */
    public String getTruncatedDescription() {
        final int maxDescrLength = 100;
        if (description == null || description.length() < maxDescrLength) {
            return description;
        }

        final String ending = "...";

        // limit the description
        String truncDescr = description.substring(
                0, maxDescrLength - ending.length());

        // truncate the description on the space
        int lastSpace = truncDescr.lastIndexOf(" ");
        if (lastSpace != -1) {
            truncDescr = truncDescr.substring(0, lastSpace);
        }

        return truncDescr + ending;
    }

    /**
     * Gets the string that says how long since this build has scheduled.
     *
     * @return
     *      string like "3 minutes" "1 day" etc.
     */
    public String getTimestampString() {
        long duration = new GregorianCalendar().getTimeInMillis()-timestamp;
        return Util.getPastTimeString(duration);
    }

    /**
     * Returns the timestamp formatted in xs:dateTime.
     */
    public String getTimestampString2() {
        return Util.XS_DATETIME_FORMATTER.format(new Date(timestamp));
    }

    /**
     * Gets the string that says how long the build took to run.
     */
    public String getDurationString() {
        if(isBuilding())
            return Util.getTimeSpanString(System.currentTimeMillis()-timestamp)+" and counting";
        return Util.getTimeSpanString(duration);
    }

    /**
     * Gets the millisecond it took to build.
     */
    @Exported
    public long getDuration() {
        return duration;
    }

    /**
     * Gets the icon color for display.
     */
    public BallColor getIconColor() {
        if(!isBuilding()) {
            // already built
            return getResult().color;
        }

        // a new build is in progress
        BallColor baseColor;
        if(previousBuild==null)
            baseColor = BallColor.GREY;
        else
            baseColor = previousBuild.getIconColor();

        return baseColor.anime();
    }

    /**
     * Returns true if the build is still queued and hasn't started yet.
     */
    public boolean hasntStartedYet() {
        return state ==State.NOT_STARTED;
    }

    public String toString() {
        return project.getFullDisplayName()+" #"+number;
    }

    public String getDisplayName() {
        return "#"+number;
    }

    @Exported(visibility=2)
    public int getNumber() {
        return number;
    }

    public RunT getPreviousBuild() {
        return previousBuild;
    }

    /**
     * Returns the last build that didn't fail before this build.
     */
    public RunT getPreviousNotFailedBuild() {
        RunT r=previousBuild;
        while( r!=null && r.getResult()==Result.FAILURE )
            r=r.previousBuild;
        return r;
    }

    /**
     * Returns the last failed build before this build.
     */
    public RunT getPreviousFailedBuild() {
        RunT r=previousBuild;
        while( r!=null && r.getResult()!=Result.FAILURE )
            r=r.previousBuild;
        return r;
    }

    public RunT getNextBuild() {
        return nextBuild;
    }

    // I really messed this up. I'm hoping to fix this some time
    // it shouldn't have trailing '/', and instead it should have leading '/'
    public String getUrl() {
        return project.getUrl()+getNumber()+'/';
    }

    /**
     * Obtains the absolute URL to this build.
     *
     * @deprecated
     *      This method shall <b>NEVER</b> be used during HTML page rendering, as it won't work with
     *      network set up like Apache reverse proxy.
     *      This method is only intended for the remote API clients who cannot resolve relative references
     *      (even this won't work for the same reason, which should be fixed.)
     */
    @Exported(visibility=2,name="url")
    public final String getAbsoluteUrl() {
        return project.getAbsoluteUrl()+getNumber()+'/';
    }

    public final String getSearchUrl() {
        return getNumber()+"/";
    }

    /**
     * Unique ID of this build.
     */
    public String getId() {
        return ID_FORMATTER.get().format(new Date(timestamp));
    }

    /**
     * Root directory of this {@link Run} on the master.
     *
     * Files related to this {@link Run} should be stored below this directory.
     */
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
     * Gets the first {@value #CUTOFF} artifacts (relative to {@link #getArtifactsDir()}.
     */
    public List<Artifact> getArtifacts() {
        ArtifactList r = new ArtifactList();
        addArtifacts(getArtifactsDir(),"",r);
        r.computeDisplayName();
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
            if(r.size()>CUTOFF)
                return;
            File sub = new File(dir, child);
            if (sub.isDirectory()) {
                addArtifacts(sub, path + child + '/', r);
            } else {
                r.add(new Artifact(path + child));
            }
        }
    }

    private static final int CUTOFF = 17;   // 0, 1,... 16, and then "too many"

    public final class ArtifactList extends ArrayList<Artifact> {
        public void computeDisplayName() {
            if(size()>CUTOFF)   return; // we are not going to display file names, so no point in computing this

            int maxDepth = 0;
            int[] len = new int[size()];
            String[][] tokens = new String[size()][];
            for( int i=0; i<tokens.length; i++ ) {
                tokens[i] = get(i).relativePath.split("[\\\\/]+");
                maxDepth = Math.max(maxDepth,tokens[i].length);
                len[i] = 1;
            }

            boolean collision;
            int depth=0;
            do {
                collision = false;
                Map<String,Integer/*index*/> names = new HashMap<String,Integer>();
                for (int i = 0; i < tokens.length; i++) {
                    String[] token = tokens[i];
                    String displayName = combineLast(token,len[i]);
                    Integer j = names.put(displayName, i);
                    if(j!=null) {
                        collision = true;
                        if(j>=0)
                            len[j]++;
                        len[i]++;
                        names.put(displayName,-1);  // occupy this name but don't let len[i] incremented with additional collisions
                    }
                }
            } while(collision && depth++<maxDepth);

            for (int i = 0; i < tokens.length; i++)
                get(i).displayPath = combineLast(tokens[i],len[i]);

//            OUTER:
//            for( int n=1; n<maxLen; n++ ) {
//                // if we just display the last n token, would it be suffice for disambiguation?
//                Set<String> names = new HashSet<String>();
//                for (String[] token : tokens) {
//                    if(!names.add(combineLast(token,n)))
//                        continue OUTER; // collision. Increase n and try again
//                }
//
//                // this n successfully diambiguates
//                for (int i = 0; i < tokens.length; i++) {
//                    String[] token = tokens[i];
//                    get(i).displayPath = combineLast(token,n);
//                }
//                return;
//            }

//            // it's impossible to get here, as that means
//            // we have the same artifacts archived twice, but be defensive
//            for (Artifact a : this)
//                a.displayPath = a.relativePath;
        }

        /**
         * Combines last N token into the "a/b/c" form.
         */
        private String combineLast(String[] token, int n) {
            StringBuffer buf = new StringBuffer();
            for( int i=Math.max(0,token.length-n); i<token.length; i++ ) {
                if(buf.length()>0)  buf.append('/');
                buf.append(token[i]);
            }
            return buf.toString();
        }
    }

    /**
     * A build artifact.
     */
    public class Artifact {
        /**
         * Relative path name from {@link Run#getArtifactsDir()}
         */
        public final String relativePath;

        /**
         * Truncated form of {@link #relativePath} just enough
         * to disambiguate {@link Artifact}s.
         */
        /*package*/ String displayPath;

        /*package for test*/ Artifact(String relativePath) {
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

        public String getDisplayPath() {
            return displayPath;
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

    protected SearchIndexBuilder makeSearchIndex() {
        SearchIndexBuilder builder = super.makeSearchIndex()
                .add("console")
                .add("changes");
        for (Action a : getActions()) {
            if(a.getIconFileName()!=null)
                builder.add(a.getUrlName());
        }
        return builder;
    }

    public Api getApi(final StaplerRequest req) {
        return new Api(this);
    }

    public void checkPermission(Permission p) {
        getACL().checkPermission(p);
    }

    public boolean hasPermission(Permission p) {
        return getACL().hasPermission(p);
    }

    public ACL getACL() {
        // for now, don't maintain ACL per run, and do it at project level
        return getParent().getACL();
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
        
        boolean renamingSucceeded = rootDir.renameTo(tmp);
        Util.deleteRecursive(tmp);

        if(!renamingSucceeded)
            throw new IOException(rootDir+" is in use");

        removeRunFromParent();
    }
    @SuppressWarnings("unchecked") // seems this is too clever for Java's type system?
    private void removeRunFromParent() {
        getParent().removeRun((RunT)this);
    }

    protected static interface Runner {
        /**
         * Performs the main build and returns the status code.
         *
         * @throws Exception
         *      exception will be recorded and the build will be considered a failure.
         */
        Result run( BuildListener listener ) throws Exception, RunnerAbortedException;

        /**
         * Performs the post-build action.
         * <p>
         * This method is called after the status of the build is determined.
         * This is a good opportunity to do notifications based on the result
         * of the build. When this method is called, the build is not really
         * finalized yet, and the build is still considered in progress --- for example,
         * even if the build is successful, this build still won't be picked up
         * by {@link Job#getLastSuccessfulBuild()}.
         */
        void post( BuildListener listener ) throws Exception;

        /**
         * Performs final clean up action.
         * <p>
         * This method is called after {@link #post(BuildListener)},
         * after the build result is fully finalized. This is the point
         * where the build is already considered completed.
         * <p>
         * Among other things, this is often a necessary pre-condition
         * before invoking other builds that depend on this build.
         */
        void cleanUp(BuildListener listener) throws Exception;
    }

    /**
     * Used in {@link Runner#run} to indicates that a fatal error in a build
     * is reported to {@link BuildListener} and the build should be simply aborted
     * without further recording a stack trace.
     */
    public static final class RunnerAbortedException extends RuntimeException {}

    protected final void run(Runner job) {
        if(result!=null)
            return;     // already built.

        BuildListener listener=null;
        PrintStream log = null;

        onStartBuilding();
        try {
            // to set the state to COMPLETE in the end, even if the thread dies abnormally.
            // otherwise the queue state becomes inconsistent

            long start = System.currentTimeMillis();

            try {
                try {
                    log = new PrintStream(new FileOutputStream(getLogFile()));
                    listener = new StreamBuildListener(new CloseProofOutputStream(log));

                    listener.started();

                    RunListener.fireStarted(this,listener);

                    setResult(job.run(listener));

                    LOGGER.info(toString()+" main build action completed: "+result);
                } catch (ThreadDeath t) {
                    throw t;
                } catch( RunnerAbortedException e ) {
                    result = Result.FAILURE;
                } catch( InterruptedException e) {
                    // aborted
                    result = Result.ABORTED;
                    listener.getLogger().println(Messages.Run_BuildAborted());
                    LOGGER.log(Level.INFO,toString()+" aborted",e);
                } catch( Throwable e ) {
                    handleFatalBuildProblem(listener,e);
                    result = Result.FAILURE;
                }

                // even if the main build fails fatally, try to run post build processing
                job.post(listener);

            } catch (ThreadDeath t) {
                throw t;
            } catch( Throwable e ) {
                handleFatalBuildProblem(listener,e);
                result = Result.FAILURE;
            } finally {
                long end = System.currentTimeMillis();
                duration = end-start;

                // advance the state.
                // the significance of doing this is that Hudson
                // will now see this build as completed.
                // things like triggering other builds requires this as pre-condition.
                // see issue #980.
                state = State.POST_PRODUCTION;

                try {
                    job.cleanUp(listener);
                } catch (Exception e) {
                    handleFatalBuildProblem(listener,e);
                    // too late to update the result now
                }

                RunListener.fireCompleted(this,listener);

                if(listener!=null)
                    listener.finished(result);
                if(log!=null)
                    log.close();

                try {
                    save();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                getParent().logRotate();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            onEndBuilding();
        }
    }

    /**
     * Handles a fatal build problem (exception) that occurred during the build.
     */
    private void handleFatalBuildProblem(BuildListener listener, Throwable e) {
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
    }

    /**
     * Called when a job started building.
     */
    protected void onStartBuilding() {
        state = State.BUILDING;
    }

    /**
     * Called when a job finished building normally or abnormally.
     */
    protected void onEndBuilding() {
        state = State.COMPLETED;
        if(result==null) {
            // shouldn't happen, but be defensive until we figure out why
            result = Result.FAILURE;
            LOGGER.warning(toString()+": No build result is set, so marking as failure. This shouldn't happen");
        }
    }

    /**
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        getDataFile().write(this);
    }

    private XmlFile getDataFile() {
        return new XmlFile(XSTREAM,new File(getRootDir(),"build.xml"));
    }

    /**
     * Gets the log of the build as a string.
     *
     * @deprecated Use {@link #getLog(int)} instead as it avoids loading
     * the whole log into memory unnecessarily.
     */
    @Deprecated
    public String getLog() throws IOException {
        return Util.loadFile(getLogFile());
    }

    /**
     * Gets the log of the build as a list of strings (one per log line).
     * The number of lines returned is constrained by the maxLines parameter.
     *
     * @param maxLines The maximum number of log lines to return.  If the log
     * is bigger than this, only the most recent lines are returned.
     * @return A list of log lines.  Will have no more than maxLines elements.
     * @throws IOException If there is a problem reading the log file.
     */
    public List<String> getLog(int maxLines) throws IOException {
        int lineCount = 0;
        List<String> logLines = new LinkedList<String>();
        BufferedReader reader = new BufferedReader(new FileReader(getLogFile()));
        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                logLines.add(line);
                ++lineCount;
                // If we have too many lines, remove the oldest line.  This way we
                // never have to hold the full contents of a huge log file in memory.
                // Adding to and removing from the ends of a linked list are cheap
                // operations.
                if (lineCount > maxLines)
                    logLines.remove(0);
            }
        } finally {
            reader.close();
        }

        // If the log has been truncated, include that information.
        // Use set (replaces the first element) rather than add so that
        // the list doesn't grow beyond the specified maximum number of lines.
        if (lineCount > maxLines)
            logLines.set(0, "[...truncated " + (lineCount - (maxLines - 1)) + " lines...]");

        return logLines;
    }

    public void doBuildStatus( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        // see Hudson.doNocacheImages. this is a work around for a bug in Firefox
        rsp.sendRedirect2(req.getContextPath()+"/nocacheImages/48x48/"+getBuildStatusUrl());
    }

    public String getBuildStatusUrl() {
        return getIconColor().getImage();
    }

    public static class Summary {
        /**
         * Is this build worse or better, compared to the previous build?
         */
        public boolean isWorse;
        public String message;

        public Summary(boolean worse, String message) {
            this.isWorse = worse;
            this.message = message;
        }
    }

    /**
     * Gets an object that computes the single line summary of this build.
     */
    public Summary getBuildStatusSummary() {
        Run prev = getPreviousBuild();

        if(getResult()==Result.SUCCESS) {
            if(prev==null || prev.getResult()== Result.SUCCESS)
                return new Summary(false,"stable");
            else
                return new Summary(false,"back to normal");
        }

        if(getResult()==Result.FAILURE) {
            RunT since = getPreviousNotFailedBuild();
            if(since==null)
                return new Summary(false,"broken for a long time");
            if(since==prev)
                return new Summary(true,"broken since this build");
            return new Summary(false,"broken since "+since.getDisplayName());
        }

        if(getResult()==Result.ABORTED)
            return new Summary(false,"aborted");

        if(getResult()==Result.UNSTABLE) {
            if(((Run)this) instanceof Build) {
                AbstractTestResultAction trN = ((Build)(Run)this).getTestResultAction();
                AbstractTestResultAction trP = prev==null ? null : ((Build) prev).getTestResultAction();
                if(trP==null) {
                    if(trN!=null && trN.getFailCount()>0)
                        return new Summary(false,combine(trN.getFailCount(),"test failure"));
                    else // ???
                        return new Summary(false,"unstable");
                }
                if(trP.getFailCount()==0)
                    return new Summary(true,combine(trN.getFailCount(),"test")+" started to fail");
                if(trP.getFailCount() < trN.getFailCount())
                    return new Summary(true,combine(trN.getFailCount()-trP.getFailCount(),"more test")
                        +" are failing ("+trN.getFailCount()+" total)");
                if(trP.getFailCount() > trN.getFailCount())
                    return new Summary(false,combine(trP.getFailCount()-trN.getFailCount(),"less test")
                        +" are failing ("+trN.getFailCount()+" total)");

                return new Summary(false,combine(trN.getFailCount(),"test")+" are still failing");
            }
        }

        return new Summary(false,"?");
    }

    /**
     * Serves the artifacts.
     */
    public void doArtifact( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException, InterruptedException {
        new DirectoryBrowserSupport(this,project.getDisplayName()+' '+getDisplayName())
            .serveFile(req, rsp, new FilePath(getArtifactsDir()), "package.gif", true);
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

    /**
     * Returns the build time stamp in the body.
     */
    public void doBuildTimestamp( StaplerRequest req, StaplerResponse rsp, @QueryParameter("format") String format) throws IOException {
        rsp.setContentType("text/plain");
        rsp.setCharacterEncoding("US-ASCII");
        rsp.setStatus(HttpServletResponse.SC_OK);
        DateFormat df = format==null ?
                DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT, Locale.ENGLISH) :
                new SimpleDateFormat(format,req.getLocale());
        rsp.getWriter().print(df.format(getTimestamp().getTime()));
    }

    /**
     * Handles incremental log output.
     */
    public void doProgressiveLog( StaplerRequest req, StaplerResponse rsp) throws IOException {
        new LargeText(getLogFile(),!isLogUpdated()).doProgressText(req,rsp);
    }

    public void doToggleLogKeep( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        checkPermission(UPDATE);

        keepLog(!keepLog);
        rsp.forwardToPreviousPage(req);
    }

    /**
     * Marks this build to keep the log.
     */
    public final void keepLog() throws IOException {
        keepLog(true);
    }

    public void keepLog(boolean newValue) throws IOException {
        keepLog = newValue;
        save();
    }

    /**
     * Deletes the build when the button is pressed.
     */
    public void doDoDelete( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        checkPermission(DELETE);

        // We should not simply delete the build if it has been explicitly
        // marked to be preserved, or if the build should not be deleted
        // due to dependencies!
        String why = getWhyKeepLog();
        if (why!=null) {
            sendError(Messages.Run_UnableToDelete(toString(),why),req,rsp);
            return;
        }

        delete();
        rsp.sendRedirect2(req.getContextPath()+'/' + getParent().getUrl());
    }

    public void setDescription(String description) throws IOException {
        checkPermission(UPDATE);
        this.description = description;
        save();
    }
    
    /**
     * Accepts the new description.
     */
    public synchronized void doSubmitDescription( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        setDescription(req.getParameter("description"));
        rsp.sendRedirect(".");  // go to the top page
    }

    /**
     * Returns the map that contains environmental variable overrides for this build.
     *
     * <p>
     * {@link BuildStep}s that invoke external processes should use this.
     * This allows {@link BuildWrapper}s and other project configurations (such as JDK selection)
     * to take effect.
     *
     * <p>
     * On Windows systems, environment variables are case-preserving but
     * comparison/query is case insensitive (IOW, you can set 'Path' to something
     * and you get the same value by doing '%PATH%'.)  So to implement this semantics
     * the map returned from here is a {@link TreeMap} with a special comparator.
     *
     */
    public Map<String,String> getEnvVars() {
        EnvVars env = new EnvVars();
        env.put("BUILD_NUMBER",String.valueOf(number));
        env.put("BUILD_ID",getId());
        env.put("BUILD_TAG","hudson-"+getParent().getName()+"-"+number);
        env.put("JOB_NAME",getParent().getFullName());
        String rootUrl = Hudson.getInstance().getRootUrl();
        if(rootUrl!=null)
            env.put("HUDSON_URL", rootUrl);

        Thread t = Thread.currentThread();
        if (t instanceof Executor) {
            Executor e = (Executor) t;
            env.put("EXECUTOR_NUMBER",String.valueOf(e.getNumber()));
        }

        return env;
    }

    private static final XStream XSTREAM = new XStream2();
    static {
        XSTREAM.alias("build",FreeStyleBuild.class);
        XSTREAM.alias("matrix-build",MatrixBuild.class);
        XSTREAM.alias("matrix-run",MatrixRun.class);
        XSTREAM.registerConverter(Result.conv);
    }

    private static final Logger LOGGER = Logger.getLogger(Run.class.getName());

    /**
     * Sort by date. Newer ones first. 
     */
    public static final Comparator<Run> ORDER_BY_DATE = new Comparator<Run>() {
        public int compare(Run lhs, Run rhs) {
            long lt = lhs.getTimestamp().getTimeInMillis();
            long rt = rhs.getTimestamp().getTimeInMillis();
            if(lt>rt)   return -1;
            if(lt<rt)   return 1;
            return 0;
        }
    };

    /**
     * {@link FeedAdapter} to produce feed from the summary of this build.
     */
    public static final FeedAdapter<Run> FEED_ADAPTER = new FeedAdapter<Run>() {
        public String getEntryTitle(Run entry) {
            return entry+" ("+entry.getResult()+")";
        }

        public String getEntryUrl(Run entry) {
            return entry.getUrl();
        }

        // produces a tag URL as per RFC 4151, required by Atom 1.0
        public String getEntryID(Run entry) {
            return "tag:" + "hudson.dev.java.net,"
                + entry.getTimestamp().get(Calendar.YEAR) + ":"
                + entry.getParent().getName()+':'+entry.getId();
        }

        public String getEntryDescription(Run entry) {
            // TODO: this could provide some useful details
            return null;
        }

        public Calendar getEntryTimestamp(Run entry) {
            return entry.getTimestamp();
        }
    };

    /**
     * {@link BuildBadgeAction} that shows the logs are being kept.
     */
    public final class KeepLogBuildBadge implements BuildBadgeAction {
        public String getIconFileName() { return null; }
        public String getDisplayName() { return null; }
        public String getUrlName() { return null; }
        public String getWhyKeepLog() { return Run.this.getWhyKeepLog(); }
    }

    public static final PermissionGroup PERMISSIONS = new PermissionGroup(Run.class,Messages._Run_Permissions_Title());
    public static final Permission DELETE = new Permission(PERMISSIONS,"Delete", Permission.DELETE);
    public static final Permission UPDATE = new Permission(PERMISSIONS,"Update", Permission.UPDATE);
}
