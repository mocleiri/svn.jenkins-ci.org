package hudson.model;

import hudson.AbortException;
import hudson.FeedAdapter;
import hudson.FilePath;
import hudson.Launcher;
import hudson.StructuredForm;
import hudson.maven.MavenModule;
import hudson.model.Descriptor.FormException;
import hudson.model.Fingerprint.RangeSet;
import hudson.model.RunMap.Constructor;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.scm.SCMS;
import hudson.search.SearchIndexBuilder;
import hudson.security.Permission;
import hudson.tasks.BuildTrigger;
import hudson.triggers.SCMTrigger;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.triggers.Triggers;
import hudson.util.EditDistance;
import hudson.widgets.BuildHistoryWidget;
import hudson.widgets.HistoryWidget;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base implementation of {@link Job}s that build software.
 *
 * For now this is primarily the common part of {@link Project} and {@link MavenModule}.
 *
 * @author Kohsuke Kawaguchi
 * @see AbstractBuild
 */
public abstract class AbstractProject<P extends AbstractProject<P,R>,R extends AbstractBuild<P,R>> extends Job<P,R> implements BuildableItem {

    /**
     * {@link SCM} associated with the project.
     * To allow derived classes to link {@link SCM} config to elsewhere,
     * access to this variable should always go through {@link #getScm()}.
     */
    private SCM scm = new NullSCM();

    /**
     * All the builds keyed by their build number.
     */
    protected transient /*almost final*/ RunMap<R> builds = new RunMap<R>();

    /**
     * The quiet period. Null to delegate to the system default.
     */
    private Integer quietPeriod = null;

    /**
     * If this project is configured to be only built on a certain label,
     * this value will be set to that label.
     *
     * For historical reasons, this is called 'assignedNode'. Also for
     * a historical reason, null to indicate the affinity
     * with the master node.
     *
     * @see #canRoam
     */
    private String assignedNode;

    /**
     * True if this project can be built on any node.
     *
     * <p>
     * This somewhat ugly flag combination is so that we can migrate
     * existing Hudson installations nicely.
     */
    private boolean canRoam;

    /**
     * True to suspend new builds.
     */
    protected boolean disabled;

    /**
     * Identifies {@link JDK} to be used.
     * Null if no explicit configuration is required.
     *
     * <p>
     * Can't store {@link JDK} directly because {@link Hudson} and {@link Project}
     * are saved independently.
     *
     * @see Hudson#getJDK(String)
     */
    private String jdk;

    /**
     * @deprecated
     */
    private transient boolean enableRemoteTrigger;

    private BuildAuthorizationToken authToken = null;

    /**
     * List of all {@link Trigger}s for this project.
     */
    protected List<Trigger<?>> triggers = new Vector<Trigger<?>>();

    /**
     * {@link Action}s contributed from subsidiary objects associated with
     * {@link AbstractProject}, such as from triggers, builders, publishers, etc.
     *
     * We don't want to persist them separately, and these actions
     * come and go as configuration change, so it's kept separate.
     */
    protected transient /*final*/ List<Action> transientActions = new Vector<Action>();

    protected AbstractProject(ItemGroup parent, String name) {
        super(parent,name);

        if(!Hudson.getInstance().getSlaves().isEmpty()) {
            // if a new job is configured with Hudson that already has slave nodes
            // make it roamable by default
            canRoam = true;
        }
    }

    @Override
    public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
        super.onLoad(parent, name);

        this.builds = new RunMap<R>();
        this.builds.load(this,new Constructor<R>() {
            public R create(File dir) throws IOException {
                return loadBuild(dir);
            }
        });

        if(triggers==null)
            // it didn't exist in < 1.28
            triggers = new Vector<Trigger<?>>();
        for (Trigger t : triggers)
            t.start(this,false);

        if(transientActions==null)
            transientActions = new Vector<Action>();    // happens when loaded from disk
        updateTransientActions();
    }

    /**
     * If this project is configured to be always built on this node,
     * return that {@link Node}. Otherwise null.
     */
    public Label getAssignedLabel() {
        if(canRoam)
            return null;

        if(assignedNode==null)
            return Hudson.getInstance().getSelfLabel();
        return Hudson.getInstance().getLabel(assignedNode);
    }

    /**
     * Get the term used in the UI to represent this kind of {@link AbstractProject}.
     * Must start with a capital letter.
     */
    @Override
    public String getPronoun() {
        return Messages.AbstractProject_Pronoun();
    }

    /**
     * Gets the directory where the module is checked out.
     *
     * @return
     *      null if the workspace is on a slave that's not connected.
     */
    public abstract FilePath getWorkspace();

    /**
     * Returns the root directory of the checked-out module.
     * <p>
     * This is usually where <tt>pom.xml</tt>, <tt>build.xml</tt>
     * and so on exists.
     */
    public FilePath getModuleRoot() {
        return getScm().getModuleRoot(getWorkspace());
    }

    /**
     * Returns the root directories of all checked-out modules.
     * <p>
     * Some SCMs support checking out multiple modules into the same workspace.
     * In these cases, the returned array will have a length greater than one.
     * @return The roots of all modules checked out from the SCM.
     */
    public FilePath[] getModuleRoots() {
        return getScm().getModuleRoots(getWorkspace());
    }

    public int getQuietPeriod() {
        return quietPeriod!=null ? quietPeriod : Hudson.getInstance().getQuietPeriod();
    }

    // ugly name because of EL
    public boolean getHasCustomQuietPeriod() {
        return quietPeriod!=null;
    }

    public final boolean isBuildable() {
        return !isDisabled();
    }

    /**
     * Used in <tt>sidepanel.jelly</tt> to decide whether to display
     * the config/delete/build links.
     */
    public boolean isConfigurable() {
        return true;
    }

    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Marks the build as disabled.
     */
    public void makeDisabled(boolean b) throws IOException {
        if(disabled==b)     return; // noop
        this.disabled = b;
        save();
    }

    @Override
    public BallColor getIconColor() {
        if(isDisabled())
            return BallColor.DISABLED;
        else
            return super.getIconColor();
    }

    protected void updateTransientActions() {
        synchronized(transientActions) {
            transientActions.clear();

            for (JobProperty<? super P> p : properties) {
                Action a = p.getJobAction((P)this);
                if(a!=null)
                    transientActions.add(a);
            }
        }
    }

    public List<ProminentProjectAction> getProminentActions() {
        List<Action> a = getActions();
        List<ProminentProjectAction> pa = new Vector<ProminentProjectAction>();
        for (Action action : a) {
            if(action instanceof ProminentProjectAction)
                pa.add((ProminentProjectAction) action);
        }
        return pa;
    }

    @Override
    public void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        super.doConfigSubmit(req,rsp);

        Set<AbstractProject> upstream = Collections.emptySet();
        if(req.getParameter("pseudoUpstreamTrigger")!=null) {
            upstream = new HashSet<AbstractProject>(Items.fromNameList(req.getParameter("upstreamProjects"),AbstractProject.class));
        }

        // dependency setting might have been changed by the user, so rebuild.
        Hudson.getInstance().rebuildDependencyGraph();

        // reflect the submission of the pseudo 'upstream build trriger'.
        // this needs to be done after we release the lock on 'this',
        // or otherwise we could dead-lock

        for (Project p : Hudson.getInstance().getProjects()) {
            boolean isUpstream = upstream.contains(p);
            synchronized(p) {
                List<AbstractProject> newChildProjects = new ArrayList<AbstractProject>(p.getDownstreamProjects());

                if(isUpstream) {
                    if(!newChildProjects.contains(this))
                        newChildProjects.add(this);
                } else {
                    newChildProjects.remove(this);
                }

                if(newChildProjects.isEmpty()) {
                    p.removePublisher(BuildTrigger.DESCRIPTOR);
                } else {
                    BuildTrigger existing = (BuildTrigger)p.getPublisher(BuildTrigger.DESCRIPTOR);
                    if(existing!=null && existing.hasSame(newChildProjects))
                        continue;   // no need to touch
                    p.addPublisher(new BuildTrigger(newChildProjects,
                        existing==null?Result.SUCCESS:existing.getThreshold()));
                }
            }
        }

        // notify the queue as the project might be now tied to different node
        Hudson.getInstance().getQueue().scheduleMaintenance();

        // this is to reflect the upstream build adjustments done above
        Hudson.getInstance().rebuildDependencyGraph();
    }

    /**
     * Schedules a build of this project.
     *
     * @return
     *      true if the project is actually added to the queue.
     *      false if the queue contained it and therefore the add()
     *      was noop
     */
    public boolean scheduleBuild() {
        if(isDisabled())    return false;
        return Hudson.getInstance().getQueue().add(this);
    }

    /**
     * Schedules a polling of this project.
     */
    public boolean schedulePolling() {
        if(isDisabled())    return false;
        SCMTrigger scmt = getTrigger(SCMTrigger.class);
        if(scmt==null)      return false;
        scmt.run();
        return true;
    }

    /**
     * Returns true if the build is in the queue.
     */
    @Override
    public boolean isInQueue() {
        return Hudson.getInstance().getQueue().contains(this);
    }

    @Override
    public Queue.Item getQueueItem() {
        return Hudson.getInstance().getQueue().getItem(this);
    }

    /**
     * Returns true if a build of this project is in progress.
     */
    public boolean isBuilding() {
        R b = getLastBuild();
        return b!=null && b.isBuilding();
    }

    /**
     * Gets the JDK that this project is configured with, or null.
     */
    public JDK getJDK() {
        return Hudson.getInstance().getJDK(jdk);
    }

    /**
     * Overwrites the JDK setting.
     */
    public synchronized void setJDK(JDK jdk) throws IOException {
        this.jdk = jdk.getName();
        save();
    }

    public BuildAuthorizationToken getAuthToken() {
        return authToken;
    }

    public SortedMap<Integer, ? extends R> _getRuns() {
        return builds.getView();
    }

    public void removeRun(R run) {
        this.builds.remove(run);
    }

    /**
     * Determines Class&lt;R>.
     */
    protected abstract Class<R> getBuildClass();

    /**
     * Creates a new build of this project for immediate execution.
     */
    protected R newBuild() throws IOException {
        try {
            R lastBuild = getBuildClass().getConstructor(getClass()).newInstance(this);
            builds.put(lastBuild);
            return lastBuild;
        } catch (InstantiationException e) {
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        } catch (InvocationTargetException e) {
            throw handleInvocationTargetException(e);
        } catch (NoSuchMethodException e) {
            throw new Error(e);
        }
    }

    private IOException handleInvocationTargetException(InvocationTargetException e) {
        Throwable t = e.getTargetException();
        if(t instanceof Error)  throw (Error)t;
        if(t instanceof RuntimeException)   throw (RuntimeException)t;
        if(t instanceof IOException)    return (IOException)t;
        throw new Error(t);
    }

    /**
     * Loads an existing build record from disk.
     */
    protected R loadBuild(File dir) throws IOException {
        try {
            return getBuildClass().getConstructor(getClass(),File.class).newInstance(this,dir);
        } catch (InstantiationException e) {
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        } catch (InvocationTargetException e) {
            throw handleInvocationTargetException(e);
        } catch (NoSuchMethodException e) {
            throw new Error(e);
        }
    }

    public synchronized List<Action> getActions() {
        // add all the transient actions, too
        List<Action> actions = new Vector<Action>(super.getActions());
        actions.addAll(transientActions);
        return actions;
    }

    /**
     * Gets the {@link Node} where this project was last built on.
     *
     * @return
     *      null if no information is available (for example,
     *      if no build was done yet.)
     */
    public Node getLastBuiltOn() {
        // where was it built on?
        AbstractBuild b = getLastBuild();
        if(b==null)
            return null;
        else
            return b.getBuiltOn();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A project must be blocked if its own previous build is in progress,
     * but derived classes can also check other conditions.
     */
    public boolean isBuildBlocked() {
        return isBuilding();
    }

    public String getWhyBlocked() {
        AbstractBuild<?, ?> build = getLastBuild();
        Executor e = build.getExecutor();
        String eta="";
        if(e!=null)
            eta = Messages.AbstractProject_ETA(e.getEstimatedRemainingTime());
        int lbn = build.getNumber();
        return Messages.AbstractProject_BuildInProgress(lbn,eta);
    }

    public final long getEstimatedDuration() {
        AbstractBuild b = getLastSuccessfulBuild();
        if(b==null)     return -1;

        long duration = b.getDuration();
        if(duration==0) return -1;

        return duration;
    }

    public R createExecutable() throws IOException {
        return newBuild();
    }

    public void checkAbortPermission() {
        checkPermission(AbstractProject.ABORT);
    }

    public boolean hasAbortPermission() {
        return hasPermission(AbstractProject.ABORT);
    }

    /**
     * Gets the {@link Resource} that represents the workspace of this project.
     */
    public Resource getWorkspaceResource() {
        return new Resource(getFullDisplayName()+" workspace");
    }

    /**
     * List of necessary resources to perform the build of this project.
     */
    public ResourceList getResourceList() {
        final Set<ResourceActivity> resourceActivities = getResourceActivities();
        final List<ResourceList> resourceLists = new ArrayList<ResourceList>(1 + resourceActivities.size());
        for (ResourceActivity activity : resourceActivities) {
            if (activity != this && activity != null) {
                // defensive infinite recursion and null check
                resourceLists.add(activity.getResourceList());
            }
        }
        resourceLists.add(new ResourceList().w(getWorkspaceResource()));
        return ResourceList.union(resourceLists);
    }

    /**
     * Set of child resource activities of the build of this project (override in child projects).
     * @return The set of child resource activities of the build of this project.
     */
    protected Set<ResourceActivity> getResourceActivities() {
        return Collections.emptySet();
    }

    public boolean checkout(AbstractBuild build, Launcher launcher, BuildListener listener, File changelogFile) throws IOException {
        SCM scm = getScm();
        if(scm==null)
            return true;    // no SCM

        try {
            FilePath workspace = getWorkspace();
            workspace.mkdirs();

            return scm.checkout(build, launcher, workspace, listener, changelogFile);
        } catch (InterruptedException e) {
            listener.getLogger().println(Messages.AbstractProject_ScmAborted());
            LOGGER.log(Level.INFO,build.toString()+" aborted",e);
            return false;
        }
    }

    /**
     * Checks if there's any update in SCM, and returns true if any is found.
     *
     * <p>
     * The caller is responsible for coordinating the mutual exclusion between
     * a build and polling, as both touches the workspace.
     */
    public boolean pollSCMChanges( TaskListener listener ) {
        SCM scm = getScm();
        if(scm==null) {
            listener.getLogger().println(Messages.AbstractProject_NoSCM());
            return false;
        }
        if(isDisabled()) {
            listener.getLogger().println(Messages.AbstractProject_Disabled());
            return false;
        }

        try {
            FilePath workspace = getWorkspace();
            if(workspace==null) {
                // workspace offline. build now, or nothing will ever be built
                listener.getLogger().println(Messages.AbstractProject_WorkspaceOffline());
                return true;
            }
            if(!workspace.exists()) {
                // no workspace. build now, or nothing will ever be built
                listener.getLogger().println(Messages.AbstractProject_NoWorkspace());
                return true;
            }

            return scm.pollChanges(this, workspace.createLauncher(listener), workspace, listener );
        } catch (AbortException e) {
            listener.fatalError(Messages.AbstractProject_Aborted());
            return false;
        } catch (IOException e) {
            e.printStackTrace(listener.fatalError(e.getMessage()));
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace(listener.fatalError(Messages.AbstractProject_PollingABorted()));
            return false;
        }
    }

    public SCM getScm() {
        return scm;
    }

    public void setScm(SCM scm) {
        this.scm = scm;
    }

    /**
     * Adds a new {@link Trigger} to this {@link Project} if not active yet.
     */
    public void addTrigger(Trigger<?> trigger) throws IOException {
        addToList(trigger,triggers);
    }

    public void removeTrigger(TriggerDescriptor trigger) throws IOException {
        removeFromList(trigger,triggers);
    }

    protected final synchronized <T extends Describable<T>>
    void addToList( T item, List<T> collection ) throws IOException {
        for( int i=0; i<collection.size(); i++ ) {
            if(collection.get(i).getDescriptor()==item.getDescriptor()) {
                // replace
                collection.set(i,item);
                save();
                return;
            }
        }
        // add
        collection.add(item);
        save();
    }

    protected final synchronized <T extends Describable<T>>
    void removeFromList(Descriptor<T> item, List<T> collection) throws IOException {
        for( int i=0; i< collection.size(); i++ ) {
            if(collection.get(i).getDescriptor()==item) {
                // found it
                collection.remove(i);
                save();
                return;
            }
        }
    }

    public synchronized Map<TriggerDescriptor,Trigger> getTriggers() {
        return (Map)Descriptor.toMap(triggers);
    }

    /**
     * Gets the specific trigger, or null if the propert is not configured for this job.
     */
    public <T extends Trigger> T getTrigger(Class<T> clazz) {
        for (Trigger p : triggers) {
            if(clazz.isInstance(p))
                return clazz.cast(p);
        }
        return null;
    }

//
//
// fingerprint related
//
//
    /**
     * True if the builds of this project produces {@link Fingerprint} records.
     */
    public abstract boolean isFingerprintConfigured();

    /**
     * Gets the other {@link AbstractProject}s that should be built
     * when a build of this project is completed.
     */
    @Exported
    public final List<AbstractProject> getDownstreamProjects() {
        return Hudson.getInstance().getDependencyGraph().getDownstream(this);
    }

    @Exported
    public final List<AbstractProject> getUpstreamProjects() {
        return Hudson.getInstance().getDependencyGraph().getUpstream(this);
    }

    /**
     * Gets all the upstream projects including transitive upstream projects.
     *
     * @since 1.138
     */
    public final Set<AbstractProject> getTransitiveUpstreamProjects() {
        return Hudson.getInstance().getDependencyGraph().getTransitiveUpstream(this);
    }

    /**
     * Gets all the downstream projects including transitive downstream projects.
     *
     * @since 1.138
     */
    public final Set<AbstractProject> getTransitiveDownstreamProjects() {
        return Hudson.getInstance().getDependencyGraph().getTransitiveDownstream(this);
    }

    /**
     * Gets the dependency relationship map between this project (as the source)
     * and that project (as the sink.)
     *
     * @return
     *      can be empty but not null. build number of this project to the build
     *      numbers of that project.
     */
    public SortedMap<Integer, RangeSet> getRelationship(AbstractProject that) {
        TreeMap<Integer,RangeSet> r = new TreeMap<Integer,RangeSet>(REVERSE_INTEGER_COMPARATOR);

        checkAndRecord(that, r, this.getBuilds());
        // checkAndRecord(that, r, that.getBuilds());

        return r;
    }

    /**
     * Helper method for getDownstreamRelationship.
     *
     * For each given build, find the build number range of the given project and put that into the map.
     */
    private void checkAndRecord(AbstractProject that, TreeMap<Integer, RangeSet> r, Collection<R> builds) {
        for (R build : builds) {
            RangeSet rs = build.getDownstreamRelationship(that);
            if(rs==null || rs.isEmpty())
                continue;

            int n = build.getNumber();

            RangeSet value = r.get(n);
            if(value==null)
                r.put(n,rs);
            else
                value.add(rs);
        }
    }

    /**
     * Builds the dependency graph.
     * @see DependencyGraph
     */
    protected abstract void buildDependencyGraph(DependencyGraph graph);

    protected SearchIndexBuilder makeSearchIndex() {
        SearchIndexBuilder sib = super.makeSearchIndex();
        if(isBuildable() && Hudson.isAdmin())
            sib.add("build","build");
        return sib;
    }

    @Override
    protected HistoryWidget createHistoryWidget() {
        return new BuildHistoryWidget<R>(this,getBuilds(),HISTORY_ADAPTER);
    }

//
//
// actions
//
//
    /**
     * Schedules a new build command.
     */
    public void doBuild( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        BuildAuthorizationToken.checkPermission(this, authToken, req, rsp);
        scheduleBuild();
        rsp.forwardToPreviousPage(req);
    }

    /**
     * Schedules a new SCM polling command.
     */
    public void doPolling( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        BuildAuthorizationToken.checkPermission(this, authToken, req, rsp);
        schedulePolling();
        rsp.forwardToPreviousPage(req);
    }

    /**
     * Cancels a scheduled build.
     */
    public void doCancelQueue( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        checkPermission(BUILD);

        Hudson.getInstance().getQueue().cancel(this);
        rsp.forwardToPreviousPage(req);
    }

    @Override
    protected void submit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
        super.submit(req,rsp);

        disabled = req.getParameter("disable")!=null;

        jdk = req.getParameter("jdk");
        if(req.getParameter("hasCustomQuietPeriod")!=null) {
            quietPeriod = Integer.parseInt(req.getParameter("quiet_period"));
        } else {
            quietPeriod = null;
        }

        if(req.getParameter("hasSlaveAffinity")!=null) {
            canRoam = false;
            assignedNode = req.getParameter("slave");
            if(assignedNode !=null) {
                if(Hudson.getInstance().getLabel(assignedNode).isEmpty())
                    assignedNode = null;   // no such label
            }
        } else {
            canRoam = true;
            assignedNode = null;
        }

        authToken = BuildAuthorizationToken.create(req);

        setScm(SCMS.parseSCM(req));

        for (Trigger t : triggers)
            t.stop();
        triggers = buildDescribable(req, Triggers.getApplicableTriggers(this), "trigger");
        for (Trigger t : triggers)
            t.start(this,true);

        updateTransientActions();
    }

    protected final <T extends Describable<T>> List<T> buildDescribable(StaplerRequest req, List<? extends Descriptor<T>> descriptors, String prefix)
        throws FormException {

        JSONObject data = StructuredForm.get(req);
        List<T> r = new Vector<T>();
        for( int i=0; i< descriptors.size(); i++ ) {
            String name = prefix + i;
            if(req.getParameter(name)!=null) {
                T instance = descriptors.get(i).newInstance(req,data.getJSONObject(name));
                r.add(instance);
            }
        }
        return r;
    }

    /**
     * Serves the workspace files.
     */
    public void doWs( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException, InterruptedException {
        FilePath ws = getWorkspace();
        if(!ws.exists()) {
            // if there's no workspace, report a nice error message
            req.getView(this,"noWorkspace.jelly").forward(req,rsp);
        } else {
            new DirectoryBrowserSupport(this,getDisplayName()+" workspace").serveFile(req, rsp, ws, "folder.gif", true);
        }
    }

    /**
     * RSS feed for changes in this project.
     */
    public void doRssChangelog(  StaplerRequest req, StaplerResponse rsp  ) throws IOException, ServletException {
        class FeedItem {
            ChangeLogSet.Entry e;
            int idx;

            public FeedItem(Entry e, int idx) {
                this.e = e;
                this.idx = idx;
            }

            AbstractBuild<?,?> getBuild() {
                return e.getParent().build;
            }
        }

        List<FeedItem> entries = new ArrayList<FeedItem>();

        for(R r=getLastBuild(); r!=null; r=r.getPreviousBuild()) {
            int idx=0;
            for( ChangeLogSet.Entry e : r.getChangeSet())
                entries.add(new FeedItem(e,idx++));
        }

        RSS.forwardToRss(
            getDisplayName()+' '+getScm().getDescriptor().getDisplayName()+" changes",
            getUrl()+"changes",
            entries, new FeedAdapter<FeedItem>() {
                public String getEntryTitle(FeedItem item) {
                    return "#"+item.getBuild().number+' '+item.e.getMsg()+" ("+item.e.getAuthor()+")";
                }

                public String getEntryUrl(FeedItem item) {
                    return item.getBuild().getUrl()+"changes#detail"+item.idx;
                }

                public String getEntryID(FeedItem item) {
                    return getEntryUrl(item);
                }

                public String getEntryDescription(FeedItem item) {
                    StringBuilder buf = new StringBuilder();
                    for(String path : item.e.getAffectedPaths())
                        buf.append(path).append('\n');
                    return buf.toString();
                }

                public Calendar getEntryTimestamp(FeedItem item) {
                    return item.getBuild().getTimestamp();
                }
            },
            req, rsp );
    }

    /**
     * Finds a {@link AbstractProject} that has the name closest to the given name.
     */
    public static AbstractProject findNearest(String name) {
        List<AbstractProject> projects = Hudson.getInstance().getAllItems(AbstractProject.class);
        String[] names = new String[projects.size()];
        for( int i=0; i<projects.size(); i++ )
            names[i] = projects.get(i).getName();

        String nearest = EditDistance.findNearest(name, names);
        return (AbstractProject)Hudson.getInstance().getItem(nearest);
    }

    private static final Comparator<Integer> REVERSE_INTEGER_COMPARATOR = new Comparator<Integer>() {
        public int compare(Integer o1, Integer o2) {
            return o2-o1;
        }
    };

    private static final Logger LOGGER = Logger.getLogger(AbstractProject.class.getName());

    public static final Permission BUILD = new Permission(PERMISSIONS, "Build", Permission.UPDATE);
    /**
     * Permission to abort a build. For now, let's make it the same as {@link #BUILD}
     */
    public static final Permission ABORT = BUILD;
}
