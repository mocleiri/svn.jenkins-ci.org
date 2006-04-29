package hudson.model;

import hudson.FilePath;
import hudson.Launcher;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.scm.SCMManager;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

/**
 * Buildable software project.
 *
 * @author Kohsuke Kawaguchi
 */
public class Project extends Job<Project,Build> {

    /**
     * All the builds keyed by their build number.
     */
    private transient SortedMap<Integer,Build> builds =
        Collections.synchronizedSortedMap(new TreeMap<Integer,Build>(reverseComparator));

    private SCM scm = new NullSCM();

    // TODO
    // private transient List<Trigger> triggers = new Vector<Trigger>();

    /**
     * List of {@link BuildStep}s.
     */
    private List<BuildStep> builders = new Vector<BuildStep>();

    private List<BuildStep> publishers = new Vector<BuildStep>();

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
     * The quiet period. Null to delegate to the system default.
     */
    private Integer quietPeriod = null;

    /**
     * If this project is configured to be only built on a certain node,
     * this value will be set to that node. Null to indicate the affinity
     * with the master node.
     *
     * see #canRoam
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
     * Creates a new project.
     */
    public Project(Hudson parent,String name) {
        super(parent,name);
        getBuildDir().mkdirs();

        if(!parent.getSlaves().isEmpty()) {
            // if a new job is configured with Hudson that already has slave nodes
            // make it roamable by default
            canRoam = true;
        }
    }

    /**
     * If this project is configured to be always built on this node,
     * return that {@link Node}. Otherwise null.
     */
    public Node getAssignedNode() {
        if(canRoam)
            return null;

        if(assignedNode ==null)
            return Hudson.getInstance();
        return getParent().getSlave(assignedNode);
    }

    public JDK getJDK() {
        return getParent().getJDK(jdk);
    }

    public int getQuietPeriod() {
        return quietPeriod!=null ? quietPeriod : getParent().getQuietPeriod();
    }

    // ugly name because of EL
    public boolean getHasCustomQuietPeriod() {
        return quietPeriod!=null;
    }


    protected void onLoad(Hudson root, String name) throws IOException {
        super.onLoad(root, name);
        builds = new TreeMap<Integer,Build>(reverseComparator);

        // load builds
        File buildDir = getBuildDir();
        buildDir.mkdirs();
        String[] builds = buildDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir,name).isDirectory();
            }
        });
        Arrays.sort(builds);

        for( String build : builds ) {
            File d = new File(buildDir,build);
            if(new File(d,"build.xml").exists()) {
                // if the build result file isn't in the directory, ignore it.
                try {
                    Build b = new Build(this,d,getLastBuild());
                    this.builds.put( b.getNumber(), b );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isBuildable() {
        return true;
    }

    public SCM getScm() {
        return scm;
    }

    public void setScm(SCM scm) {
        this.scm = scm;
    }

    public synchronized Map<BuildStepDescriptor,BuildStep> getBuilders() {
        Map<BuildStepDescriptor,BuildStep> m = new LinkedHashMap<BuildStepDescriptor,BuildStep>();
        for (BuildStep b : builders) {
            m.put(b.getDescriptor(),b);
        }
        return m;
    }

    public synchronized Map<BuildStepDescriptor,BuildStep> getPublishers() {
        Map<BuildStepDescriptor,BuildStep> m = new LinkedHashMap<BuildStepDescriptor,BuildStep>();
        for (BuildStep b : publishers) {
            m.put(b.getDescriptor(),b);
        }
        return m;
    }

    public SortedMap<Integer, ? extends Build> _getRuns() {
        return builds;
    }

    public synchronized void removeRun(Run run) {
        builds.remove(run.getNumber());
    }

    /**
     * Creates a new build of this project for immediate execution.
     */
    public synchronized Build newBuild() throws IOException {
        Build lastBuild = new Build(this);
        builds.put(lastBuild.getNumber(),lastBuild);
        return lastBuild;
    }

    public boolean checkout(Build build, Launcher launcher, BuildListener listener) throws IOException {
        if(scm==null)
            return true;    // no SCM

        FilePath workspace = getWorkspace();
        workspace.mkdirs();

        return scm.checkout(build, launcher, workspace, listener);
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
        Build b = getLastBuild();
        if(b==null)
            return null;
        else
            return b.getBuiltOn();
    }

    /**
     * Gets the directory where the module is checked out.
     */
    public FilePath getWorkspace() {
        Node node = getLastBuiltOn();

        if(node==null)
            node = getParent();

        if(node instanceof Slave)
            return ((Slave)node).getFilePath().child("workspace").child(getName());
        else
            return new FilePath(new File(root,"workspace"));
    }

    /**
     * Gets the directory where the javadoc will be published.
     */
    public File getJavadocDir() {
        return new File(root,"javadoc");
    }

    /**
     * Returns true if this project has a published javadoc.
     *
     * <p>
     * This ugly name is because of EL.
     */
    public boolean getHasJavadoc() {
        return getJavadocDir().exists();
    }

    /**
     * Returns the root directory of the checked-out module.
     *
     * @return
     *      When running remotely, this returns a remote fs directory.
     */
    public FilePath getModuleRoot() {
        return getScm().getModuleRoot(getWorkspace());
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
        getParent().getQueue().add(this);
        rsp.forwardToPreviousPage(req);
    }

    /**
     * Cancels a scheduled build.
     */
    public void doCancelQueue( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        getParent().getQueue().cancel(this);
        rsp.forwardToPreviousPage(req);
    }

    /**
     * Accepts submission from the configuration page.
     */
    public synchronized void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        int scmidx = Integer.parseInt(req.getParameter("scm"));
        scm = SCMManager.getSupportedSCMs()[scmidx].newInstance(req);

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
                if(Hudson.getInstance().getSlave(assignedNode)==null) {
                    assignedNode = null;   // no such slave
                }
            }
        } else {
            canRoam = true;
            assignedNode = null;
        }

        builders.clear();
        for( int i=0; i<BuildStep.BUILDERS.length; i++ ) {
            if(req.getParameter("builder"+i)!=null) {
                BuildStep b = BuildStep.BUILDERS[i].newInstance(req);
                builders.add(b);
            }
        }

        publishers.clear();
        for( int i=0; i<BuildStep.PUBLISHERS.length; i++ ) {
            if(req.getParameter("publisher"+i)!=null) {
                BuildStep b = BuildStep.PUBLISHERS[i].newInstance(req);
                publishers.add(b);
            }
        }

        super.doConfigSubmit(req,rsp);
    }

    /**
     * Serves the workspace files.
     */
    public synchronized void doWs( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        serveFile(req, rsp, getWorkspace().getLocal(), "folder.gif", true);
    }

    /**
     * Serves the javadoc.
     */
    public synchronized void doJavadoc( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        serveFile(req, rsp, getJavadocDir(), "help.gif", false);
    }

    /**
     * @deprecated
     *      left for legacy config file compatibility
     */
    private transient String slave;
}
