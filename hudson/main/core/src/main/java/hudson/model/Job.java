package hudson.model;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import hudson.ExtensionPoint;
import hudson.StructuredForm;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.Descriptor.FormException;
import hudson.model.listeners.ItemListener;
import hudson.search.QuickSilver;
import hudson.search.SearchIndex;
import hudson.search.SearchIndexBuilder;
import hudson.search.SearchItem;
import hudson.search.SearchItems;
import hudson.tasks.LogRotator;
import hudson.util.AtomicFileWriter;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.CopyOnWriteList;
import hudson.util.DataSetBuilder;
import hudson.util.IOException2;
import hudson.util.RunList;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.StackedAreaRenderer2;
import hudson.util.TextFile;
import hudson.widgets.HistoryWidget;
import hudson.widgets.Widget;
import hudson.widgets.HistoryWidget.Adapter;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.json.JSONObject;

import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.export.Exported;

/**
 * A job is an runnable entity under the monitoring of Hudson.
 *
 * <p>
 * Every time it "runs", it will be recorded as a {@link Run} object.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Job<JobT extends Job<JobT,RunT>, RunT extends Run<JobT,RunT>>
        extends AbstractItem implements ExtensionPoint {

    /**
     * Next build number.
     * Kept in a separate file because this is the only information
     * that gets updated often. This allows the rest of the configuration
     * to be in the VCS.
     * <p>
     * In 1.28 and earlier, this field was stored in the project configuration file,
     * so even though this is marked as transient, don't move it around.
     */
    protected transient int nextBuildNumber = 1;

    private LogRotator logRotator;
    
    /**
     * Not all plugins are good at calculating their health report quickly.
     * These fields are used to cache the health reports to speed up rendering the main page.
     */
    private transient Integer cachedBuildHealthReportsBuildNumber = null;
    private transient List<HealthReport> cachedBuildHealthReports = null;

    private boolean keepDependencies;

    /**
     * List of {@link UserProperty}s configured for this project.
     */
    protected CopyOnWriteList<JobProperty<? super JobT>> properties = new CopyOnWriteList<JobProperty<? super JobT>>();

    protected Job(ItemGroup parent,String name) {
        super(parent,name);
    }

    public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
        super.onLoad(parent, name);

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

        if(properties==null) // didn't exist < 1.72
            properties = new CopyOnWriteList<JobProperty<? super JobT>>();

        for (JobProperty p : properties)
            p.setOwner(this);
    }

    @Override
    public void onCopiedFrom(Item src) {
        super.onCopiedFrom(src);
        this.nextBuildNumber = 1;     // reset the next build number
    }

    private TextFile getNextBuildNumberFile() {
        return new TextFile(new File(this.getRootDir(),"nextBuildNumber"));
    }

    protected void saveNextBuildNumber() throws IOException {
        getNextBuildNumberFile().write(String.valueOf(nextBuildNumber)+'\n');
    }

    @Exported
    public boolean isInQueue() {
        return false;
    }

    /**
     * If this job is in the build queue, return its item.
     */
    @Exported
    public Queue.Item getQueueItem() {
        return null;
    }

    /**
     * Get the term used in the UI to represent this kind of {@link AbstractProject}.
     * Must start with a capital letter.
     */
    public String getPronoun() {
        return Messages.Job_Pronoun();
    }

    /**
     * Returns whether the name of this job can be changed by user.
     */
    public boolean isNameEditable() {
        return true;
    }

    /**
     * If true, it will keep all the build logs of dependency components.
     */
    @Exported
    public boolean isKeepDependencies() {
        return keepDependencies;
    }

    /**
     * Allocates a new buildCommand number.
     */
    public synchronized int assignBuildNumber() throws IOException {
        int r = nextBuildNumber++;
        saveNextBuildNumber();
        return r;
    }

    /**
     * Peeks the next build number.
     */
    @Exported
    public int getNextBuildNumber() {
        return nextBuildNumber;
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

    /**
     * Perform log rotation.
     */
    public void logRotate() throws IOException {
        LogRotator lr = getLogRotator();
        if(lr!=null)
            lr.perform(this);
    }

    /**
     * True if this instance supports log rotation configuration.
     */
    public boolean supportsLogRotator() {
        return true;
    }

    protected SearchIndexBuilder makeSearchIndex() {
        return super.makeSearchIndex()
            .add(new SearchIndex() {
                public void find(String token, List<SearchItem> result) {
                    try {
                        if(token.startsWith("#"))   token=token.substring(1);   // ignore leading '#'
                        int n = Integer.parseInt(token);
                        Run b = getBuildByNumber(n);
                        if(b==null) return; // no such build
                        result.add(SearchItems.create("#"+n,""+n,b));
                    } catch (NumberFormatException e) {
                        // not a number.
                    }
                }

                public void suggest(String token, List<SearchItem> result) {
                    find(token,result);
                }
            }).add("configure","config","configure");
    }

    public Collection<? extends Job> getAllJobs() {
        return Collections.<Job>singleton(this);
    }

    /**
     * Adds {@link JobProperty}.
     * @since 1.188
     */
    public void addProperty(JobProperty<? super JobT> jobProp) throws IOException {
        properties.add(jobProp);
        save();
    }
    /**
     * Gets all the job properties configured for this job.
     */
    @SuppressWarnings("unchecked")
    public Map<JobPropertyDescriptor,JobProperty<? super JobT>> getProperties() {
        return Descriptor.toMap((Iterable)properties);
    }

    /**
     * Gets the specific property, or null if the propert is not configured for this job.
     */
    public <T extends JobProperty> T getProperty(Class<T> clazz) {
        for (JobProperty p : properties) {
            if(clazz.isInstance(p))
                return clazz.cast(p);
        }
        return null;
    }

    public List<Widget> getWidgets() {
        ArrayList<Widget> r = new ArrayList<Widget>();
        r.add(createHistoryWidget());
        return r;
    }

    protected HistoryWidget createHistoryWidget() {
        return new HistoryWidget<Job,RunT>(this,getBuilds(),HISTORY_ADAPTER);
    }

    protected static final HistoryWidget.Adapter<Run> HISTORY_ADAPTER = new Adapter<Run>() {
        public int compare(Run record, String key) {
            return record.getNumber()-Integer.parseInt(key);
        }

        public String getKey(Run record) {
            return String.valueOf(record.getNumber());
        }

        public boolean isBuilding(Run record) {
            return record.isBuilding();
        }

        public String getNextKey(String key) {
            return String.valueOf(Integer.parseInt(key)+1);
        }
    };

    /**
     * Renames a job.
     *
     * <p>
     * This method is defined on {@link Job} but really only applicable
     * for {@link Job}s that are top-level items.
     */
    public void renameTo(String newName) throws IOException {
        // always synchronize from bigger objects first
        final Hudson parent = Hudson.getInstance();
        assert this instanceof TopLevelItem;
        synchronized(parent) {
            synchronized(this) {
                // sanity check
                if(newName==null)
                    throw new IllegalArgumentException("New name is not given");
                if(parent.getItem(newName)!=null)
                    throw new IllegalArgumentException("Job "+newName+" already exists");

                // noop?
                if(this.name.equals(newName))
                    return;


                String oldName = this.name;
                File oldRoot = this.getRootDir();

                doSetName(newName);
                File newRoot = this.getRootDir();

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

                parent.onRenamed((TopLevelItem)this,oldName,newName);

                for (ItemListener l : Hudson.getInstance().getJobListeners())
                    l.onRenamed(this,oldName,newName);
            }
        }
    }

    /**
     * Returns true if we should display "build now" icon
     */
    @Exported
    public abstract boolean isBuildable();

    /**
     * Gets all the builds.
     *
     * @return
     *      never null. The first entry is the latest build.
     */
    public List<RunT> getBuilds() {
        return new ArrayList<RunT>(_getRuns().values());
    }

    /**
     * Gets all the builds in a map.
     */
    public SortedMap<Integer,RunT> getBuildsAsMap() {
        return Collections.unmodifiableSortedMap(_getRuns());
    }

    /**
     * @deprecated
     *      This is only used to support backward compatibility with
     *      old URLs.
     */
    @Deprecated
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
     * @return
     *      null if no such build exists.
     * @see Run#getNumber()
     */
    public RunT getBuildByNumber(int n) {
        return _getRuns().get(n);
    }

    /**
     * Gets the youngest build #m that satisfies <tt>n&lt;=m</tt>.
     *
     * This is useful when you'd like to fetch a build but the exact build might be already
     * gone (deleted, rotated, etc.)
     */
    public final RunT getNearestBuild(int n) {
        SortedMap<Integer, ? extends RunT> m = _getRuns().headMap(n-1); // the map should include n, so n-1
        if(m.isEmpty()) return null;
        return m.get(m.lastKey());
    }

    /**
     * Gets the latest build #m that satisfies <tt>m&lt;=n</tt>.
     *
     * This is useful when you'd like to fetch a build but the exact build might be already
     * gone (deleted, rotated, etc.)
     */
    public final RunT getNearestOldBuild(int n) {
        SortedMap<Integer, ? extends RunT> m = _getRuns().tailMap(n);
        if(m.isEmpty()) return null;
        return m.get(m.firstKey());
    }

    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        try {
            // try to interpret the token as build number
            return _getRuns().get(Integer.valueOf(token));
        } catch (NumberFormatException e) {
            // try to map that to widgets
            for (Widget w : getWidgets()) {
                if(w.getUrlName().equals(token))
                    return w;
            }
            
            return super.getDynamic(token,req,rsp);
        }
    }

    /**
     * Directory for storing {@link Run} records.
     * <p>
     * Some {@link Job}s may not have backing data store for {@link Run}s,
     * but those {@link Job}s that use file system for storing data
     * should use this directory for consistency.
     *
     * @see RunMap
     */
    protected File getBuildDir() {
        return new File(getRootDir(),"builds");
    }

    /**
     * Gets all the runs.
     *
     * The resulting map must be immutable (by employing copy-on-write semantics.)
     * The map is descending order, with newest builds at the top.
     */
    protected abstract SortedMap<Integer,? extends RunT> _getRuns();

    /**
     * Called from {@link Run} to remove it from this job.
     *
     * The files are deleted already. So all the callee needs to do
     * is to remove a reference from this {@link Job}.
     */
    protected abstract void removeRun(RunT run);

    /**
     * Returns the last build.
     */
    @Exported @QuickSilver
    public RunT getLastBuild() {
        SortedMap<Integer,? extends RunT> runs = _getRuns();

        if(runs.isEmpty())    return null;
        return runs.get(runs.firstKey());
    }

    /**
     * Returns the oldest build in the record.
     */
    @Exported @QuickSilver
    public RunT getFirstBuild() {
        SortedMap<Integer,? extends RunT> runs = _getRuns();

        if(runs.isEmpty())    return null;
        return runs.get(runs.lastKey());
    }

    /**
     * Returns the last successful build, if any. Otherwise null.
     * A stable build would include either {@link Result#SUCCESS} or {@link Result#UNSTABLE}.
     * @see #getLastStableBuild()
     */
    @Exported @QuickSilver
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
    @Exported @QuickSilver
    public RunT getLastStableBuild() {
        RunT r = getLastBuild();
        while(r!=null && (r.isBuilding() || r.getResult().isWorseThan(Result.SUCCESS)))
            r=r.getPreviousBuild();
        return r;
    }

    /**
     * Returns the last failed build, if any. Otherwise null.
     */
    @Exported @QuickSilver
    public RunT getLastFailedBuild() {
        RunT r = getLastBuild();
        while(r!=null && (r.isBuilding() || r.getResult()!=Result.FAILURE))
            r=r.getPreviousBuild();
        return r;
    }

    /**
     * Used as the color of the status ball for the project.
     */
    @Exported(visibility=2,name="color")
    public BallColor getIconColor() {
        RunT lastBuild = getLastBuild();
        while(lastBuild!=null && lastBuild.hasntStartedYet())
            lastBuild = lastBuild.getPreviousBuild();

        if(lastBuild!=null)
            return lastBuild.getIconColor();
        else
            return BallColor.GREY;
    }

    /**
     * Get the current health report for a job.
     * @return
     *     the health report.  Never returns null
     */
    public HealthReport getBuildHealth() {
        List<HealthReport> reports = getBuildHealthReports();
        return reports.isEmpty() ? new HealthReport() : reports.get(0);
    }

    @Exported(name="healthReport")
    public List<HealthReport> getBuildHealthReports() {
        List<HealthReport> reports = new ArrayList<HealthReport>();
        RunT lastBuild = getLastBuild();

        if (lastBuild != null && lastBuild.isBuilding()) {
            // show the previous build's report until the current one is finished building.
            lastBuild = lastBuild.getPreviousBuild();
        }

        // check the cache
        if (cachedBuildHealthReportsBuildNumber != null && 
                cachedBuildHealthReports != null &&
                lastBuild != null &&
                cachedBuildHealthReportsBuildNumber.intValue() == lastBuild.getNumber()) {
            reports.addAll(cachedBuildHealthReports);
        } else if (lastBuild != null) {
            for (HealthReportingAction healthReportingAction : lastBuild.getActions(HealthReportingAction.class)) {
                final HealthReport report = healthReportingAction.getBuildHealth();
                if (report != null) {
                    if (report.isAggregateReport()) {
                        reports.addAll(report.getAggregatedReports());
                    } else {
                        reports.add(report);
                    }
                }
            }
            final HealthReport report = getBuildStabilityHealthReport();
            if (report != null) {
                if (report.isAggregateReport()) {
                    reports.addAll(report.getAggregatedReports());
                } else {
                    reports.add(report);
                }
            }

            Collections.sort(reports);
    
            // store the cache
            cachedBuildHealthReportsBuildNumber = lastBuild.getNumber();
            cachedBuildHealthReports = new ArrayList<HealthReport>(reports);
        }

        return reports;
    }

    private HealthReport getBuildStabilityHealthReport() {
        // we can give a simple view of build health from the last five builds
        int failCount = 0;
        int totalCount = 0;
        RunT i = getLastBuild();
        while (totalCount < 5 && i != null) {
            switch (i.getIconColor()) {
                case BLUE:
                case YELLOW:
                    //failCount stays the same
                    totalCount++;
                    break;
                case RED:
                    failCount++;
                    totalCount++;
                    break;

                default:
                    // do nothing as these are inconclusive statuses
                    break;
            }
            i = i.getPreviousBuild();
        }
        if (totalCount > 0) {
            int score = (int) ((100.0 * (totalCount - failCount)) / totalCount);
            if (score < 100 && score > 0) {
                // HACK
                // force e.g. 4/5 to be in the 60-79 range
                score--;
            }

            String description;
            if (failCount == 0) {
                description = Messages.Job_NoRecentBuildFailed();
            } else if (totalCount == failCount) {
                // this should catch the case where totalCount == 1
                // as failCount must be between 0 and totalCount
                // and we can't get here if failCount == 0
                description = Messages.Job_AllRecentBuildFailed();
            } else {
                description = Messages.Job_NOfMFailed(failCount,totalCount);
            }
            return new HealthReport(score, Messages.Job_BuildStability(description));
        }
        return null;
    }

//
//
// actions
//
//
    /**
     * Accepts submission from the configuration page.
     */
    public synchronized void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        checkPermission(CONFIGURE);

        req.setCharacterEncoding("UTF-8");

        description = req.getParameter("description");

        if (req.getParameter("logrotate") != null)
            logRotator = LogRotator.DESCRIPTOR.newInstance(req);
        else
            logRotator = null;

        keepDependencies = req.getParameter("keepDependencies") != null;

        try {
            properties.clear();

            JSONObject json = StructuredForm.get(req);

            int i=0;
            for (JobPropertyDescriptor d : JobPropertyDescriptor.getPropertyDescriptors(Job.this.getClass())) {
                String name = "jobProperty"+(i++);
                JobProperty prop = d.newInstance(req,json.getJSONObject(name));
                if (prop != null) {
                    prop.setOwner(this);
                    properties.add(prop);
                }
            }

            submit(req,rsp);

            save();

            String newName = req.getParameter("name");
            if(newName!=null && !newName.equals(name)) {
                // check this error early to avoid HTTP response splitting.
                try {
                    Hudson.checkGoodName(newName);
                } catch (ParseException e) {
                    sendError(e,req,rsp);
                    return;
                }
                rsp.sendRedirect("rename?newName="+newName);
            } else {
                rsp.sendRedirect(".");
            }
        } catch (FormException e) {
            sendError(e,req,rsp);
        }
    }

    /**
     * Accepts <tt>config.xml</tt> submission, as well as serve it.
     */
    @WebMethod(name="config.xml")
    public void doConfigDotXml(StaplerRequest req, StaplerResponse rsp) throws IOException {
        checkPermission(CONFIGURE);

        if(req.getMethod().equals("GET")) {
            // read
            rsp.setContentType("application/xml;charset=UTF-8");
            getConfigFile().writeRawTo(rsp.getWriter());
            return;
        }
        if(req.getMethod().equals("POST")) {
            // submission
            XmlFile configXmlFile = getConfigFile();
            AtomicFileWriter out = new AtomicFileWriter(configXmlFile.getFile());

            try {
                // this allows us to use UTF-8 for storing data,
                // plus it checks any well-formedness issue in the submitted data
                Transformer t = TransformerFactory.newInstance().newTransformer();
                t.transform(new StreamSource(req.getReader()),new StreamResult(out));
                out.close();
            } catch (TransformerException e) {
                throw new IOException2("Failed to persist configuration.xml",e);
            }

            // try to reflect the changes by reloading
            new XmlFile(Items.XSTREAM, out.getTemporaryFile()).unmarshal(this);
            onLoad(getParent(),getName());

            // if everything went well, commit this new version
            out.commit();
            return;
        }

        // huh?
        rsp.sendError(SC_BAD_REQUEST);
    }

    /**
     * Derived class can override this to perform additional config submission work.
     */
    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
    }

    /**
     * Returns the image that shows the current buildCommand status.
     */
    public void doBuildStatus( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        rsp.sendRedirect2(req.getContextPath()+"/nocacheImages/48x48/"+getBuildStatusUrl());
    }

    public String getBuildStatusUrl() {
        return getIconColor().getImage();
    }

    /**
     * Returns the graph that shows how long each build took.
     */
    public void doBuildTimeGraph( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        if(getLastBuild()==null) {
            rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if(req.checkIfModified(getLastBuild().getTimestamp(),rsp))
            return;
        ChartUtil.generateGraph(req,rsp, createBuildTimeTrendChart(),500,400);
    }

    /**
     * Returns the clickable map for the build time graph.
     * Loaded lazily by AJAX.
     */
    public void doBuildTimeGraphMap( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        if(getLastBuild()==null) {
            rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if(req.checkIfModified(getLastBuild().getTimestamp(),rsp))
            return;
        ChartUtil.generateClickableMap(req,rsp, createBuildTimeTrendChart(),500,400);
    }

    private JFreeChart createBuildTimeTrendChart() {
        class ChartLabel implements Comparable<ChartLabel> {
            final Run run;

            public ChartLabel(Run r) {
                this.run = r;
            }

            public int compareTo(ChartLabel that) {
                return this.run.number-that.run.number;
            }

            public boolean equals(Object o) {
                ChartLabel that = (ChartLabel) o;
                return run ==that.run;
            }

            public Color getColor() {
                // TODO: consider gradation. See http://www.javadrive.jp/java2d/shape/index9.html
                Result r = run.getResult();
                if(r ==Result.FAILURE || r== Result.ABORTED)
                    return ColorPalette.RED;
                else
                    return ColorPalette.BLUE;
            }

            public int hashCode() {
                return run.hashCode();
            }

            public String toString() {
                String l = run.getDisplayName();
                if(run instanceof Build) {
                    String s = ((Build)run).getBuiltOnStr();
                    if(s!=null)
                        l += ' '+s;
                }
                return l;
            }

        }

        DataSetBuilder<String,ChartLabel> data = new DataSetBuilder<String, ChartLabel>();
        for( Run r : getBuilds() ) {
            if(r.isBuilding())  continue;
            data.add( ((double)r.getDuration())/(1000*60), "min", new ChartLabel(r));
        }

        final CategoryDataset dataset = data.build();

        final JFreeChart chart = ChartFactory.createStackedAreaChart(
            null,                   // chart title
            null,                   // unused
            Messages.Job_minutes(),   // range axis label
            dataset,                  // data
            PlotOrientation.VERTICAL, // orientation
            false,                     // include legend
            true,                     // tooltips
            false                     // urls
        );

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();

        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setForegroundAlpha(0.8f);
//        plot.setDomainGridlinesVisible(true);
//        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        ChartUtil.adjustChebyshev(dataset,rangeAxis);
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        StackedAreaRenderer ar = new StackedAreaRenderer2() {
            @Override
            public Paint getItemPaint(int row, int column) {
                ChartLabel key = (ChartLabel) dataset.getColumnKey(column);
                return key.getColor();
            }

            @Override
            public String generateURL(CategoryDataset dataset, int row, int column) {
                ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
                return String.valueOf(label.run.number);
            }

            @Override
            public String generateToolTip(CategoryDataset dataset, int row, int column) {
                ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
                return label.run.getDisplayName() + " : " + label.run.getDurationString();
            }
        };
        plot.setRenderer(ar);

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(0,0,0,5.0));

        return chart;
    }

    /**
     * Renames this job.
     */
    public /*not synchronized. see renameTo()*/ void doDoRename( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        // rename is essentially delete followed by a create
        checkPermission(CREATE);
        checkPermission(DELETE);

        String newName = req.getParameter("newName");
        try {
            Hudson.checkGoodName(newName);
        } catch (ParseException e) {
            sendError(e,req,rsp);
            return;
        }

        renameTo(newName);
        // send to the new job page
        // note we can't use getUrl() because that would pick up old name in the Ancestor.getUrl()
        rsp.sendRedirect2(req.getContextPath()+'/'+getParent().getUrl()+getShortUrl());
    }

    public void doRssAll( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        rss(req, rsp, " all builds", new RunList(this));
    }
    public void doRssFailed( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        rss(req, rsp, " failed builds", new RunList(this).failureOnly());
    }

    private void rss(StaplerRequest req, StaplerResponse rsp, String suffix, RunList runs) throws IOException, ServletException {
        RSS.forwardToRss(getDisplayName()+ suffix, getUrl(),
            runs.newBuilds(), Run.FEED_ADAPTER, req, rsp );
    }
}
