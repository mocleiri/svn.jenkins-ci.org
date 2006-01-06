package hudson.model;

import com.thoughtworks.xstream.XStream;
import hudson.XmlFile;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMManager;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Arrays;

/**
 * Root object of the system.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Hudson extends JobCollection {
    private transient final Queue queue = new Queue();

    /**
     * {@link Executor}s in this system. Read-only.
     */
    private transient final List<Executor> executors;

    private int numExecutors = 2;

    /**
     * False to enable anyone to do anything.
     */
    private boolean useSecurity = false;

    /**
     * Root directory of the system.
     */
    public transient final File root;

    /**
     * All {@link Job}s keyed by their names.
     */
    /*package*/ transient final Map<String,Job> jobs = new TreeMap<String,Job>();

    /**
     * The sole instance.
     */
    private static Hudson theInstance;

    /**
     * Set to true if this instance is going to shut down.
     */
    private boolean shuttingDown;

    private List<JDK> jdks;

    /**
     * {@link View}s.
     */
    private List<View> views;   // can't initialize it eagerly for backward compatibility

    public static Hudson getInstance() {
        return theInstance;
    }


    public Hudson(File root) throws IOException {
        this.root = root;
        if(theInstance!=null)
            throw new IllegalStateException("second instance");
        theInstance = this;

        load();

        this.executors = new ArrayList<Executor>(numExecutors);
        for( int i=0; i<numExecutors; i++ )
            executors.add(new Executor(this));

    }

    /**
     * Gets the snapshot of all the jobs.
     */
    public synchronized Collection<Job> getJobs() {
        return new ArrayList<Job>(jobs.values());
    }

    /**
     * Every job belongs to us.
     *
     * @deprecated
     *      why are you calling a method that always return true?
     */
    public boolean containsJob(Job job) {
        return true;
    }

    public synchronized JobCollection getView(String name) {
        if(views!=null) {
            for (View v : views) {
                if(v.getViewName().equals(name))
                    return v;
            }
        }
        if(this.getViewName().equals(name))
            return this;
        else
            return null;
    }

    /**
     * Gets the read-only list of all {@link JobCollection}s.
     */
    public synchronized JobCollection[] getViews() {
        if(views==null)
            views = new ArrayList<View>();
        JobCollection[] r = new JobCollection[views.size()+1];
        views.toArray(r);
        // sort Views and put "all" at the very beginning
        r[r.length-1] = r[0];
        Arrays.sort(r,1,r.length,JobCollection.SORTER);
        r[0] = this;
        return r;
    }

    public synchronized void deleteView(View view) throws IOException {
        if(views!=null) {
            views.remove(view);
            save();
        }
    }

    public String getViewName() {
        return "All";
    }

    /**
     * Gets the list of all {@link Executor}s.
     */
    public List<Executor> getExecutors() {
        return executors;
    }

    public Queue getQueue() {
        return queue;
    }

    public String getDisplayName() {
        return "Hudson";
    }

    public List<JDK> getJDKs() {
        if(jdks==null)
            jdks = new ArrayList<JDK>();
        return jdks;
    }

    /**
     * Gets the JDK installation of the given name, or returns null.
     */
    public JDK getJDK(String name) {
        for (JDK j : getJDKs()) {
            if(j.getName().equals(name))
                return j;
        }
        return null;
    }

    /**
     * Dummy method that returns "".
     * used from JSPs.
     */
    public String getUrl() {
        return "";
    }

    public File getRootDir() {
        return root;
    }

    public boolean isUseSecurity() {
        return useSecurity;
    }

    public void setUseSecurity(boolean useSecurity) {
        this.useSecurity = useSecurity;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * Gets the job of the given name.
     *
     * @return null
     *      if such a project doesn't exist.
     */
    public synchronized Job getJob(String name) {
        return jobs.get(name);
    }

    /**
     * Creates a new job.
     *
     * @throws IllegalArgumentException
     *      if the project of the given name already exists.
     */
    public synchronized Job createProject( Class type, String name ) throws IOException {
        if(jobs.containsKey(name))
            throw new IllegalArgumentException();
        if(!Job.class.isAssignableFrom(type))
            throw new IllegalArgumentException();

        Job job;
        try {
            job = (Job)type.getConstructor(Hudson.class,String.class).newInstance(this,name);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        job.save();
        jobs.put(name,job);
        return job;
    }

    /**
     * The file we save our configuration.
     */
    private XmlFile getConfigFile() {
        XStream xs = new XStream();
        xs.alias("hudson",Hudson.class);
        return new XmlFile(xs, new File(root,"config.xml"));
    }

    /**
     * Returns the number of {@link Executor}s.
     *
     * This may be different from <code>getExecutors().size()</code>
     * because it takes time to adjust the number of executors.
     */
    public int getNumExecutors() {
        return numExecutors;
    }

    private void load() throws IOException {
        XmlFile cfg = getConfigFile();
        if(cfg.exists())
            cfg.unmarshal(this);

        File projectsDir = new File(root,"jobs");
        if(!projectsDir.isDirectory() && !projectsDir.mkdirs()) {
            if(projectsDir.exists())
                throw new IOException(projectsDir+" is not a directory");
            throw new IOException("Unable to create "+projectsDir+"\nPermission issue? Please create this directory manually.");
        }
        File[] subdirs = projectsDir.listFiles(new FileFilter() {
            public boolean accept(File child) {
                return child.isDirectory();
            }
        });
        jobs.clear();
        for (File subdir : subdirs) {
            try {
                Job p = Job.load(this,subdir);
                jobs.put(p.getName(), p);
            } catch (IOException e) {
                e.printStackTrace(); // TODO: logging
            }
        }
    }

    /**
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        getConfigFile().write(this);
    }


    /**
     * Called to shut down the system.
     */
    public void cleanUp() {
        shuttingDown = true;
        if(executors!=null) {
            for( Executor e : executors )
                e.interrupt();
        }
        ExternalJob.reloadThread.interrupt();
    }



//
//
// actions
//
//
    /**
     * Accepts submission from the configuration page.
     */
    public synchronized void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        useSecurity = req.getParameter("use_security")!=null;

        numExecutors = Integer.parseInt(req.getParameter("numExecutors"));

        {// update JDK installations
            jdks.clear();
            String[] names = req.getParameterValues("jdk_name");
            String[] homes = req.getParameterValues("jdk_home");
            if(names!=null && homes!=null) {
                int len = Math.min(names.length,homes.length);
                for(int i=0;i<len;i++) {
                    jdks.add(new JDK(names[i],homes[i]));
                }
            }
        }

        for( Executor e : executors )
            if(e.getCurrentBuild()==null)
                e.interrupt();

        while(executors.size()<numExecutors)
            executors.add(new Executor(this));

        boolean result = true;

        for( BuildStepDescriptor d : BuildStep.BUILDERS )
            result &= d.configure(req);

        for( BuildStepDescriptor d : BuildStep.PUBLISHERS )
            result &= d.configure(req);

        for( SCMDescriptor scmd : SCMManager.getSupportedSCMs() )
            result &= scmd.configure(req);

        save();
        if(result)
            rsp.sendRedirect(".");  // go to the top page
        else
            rsp.sendRedirect("configure"); // back to config
    }

    public synchronized Job doCreateJob( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        String name = req.getParameter("name");
        String className = req.getParameter("type");

        if(name==null || getJob(name)!=null || className==null) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        try {
            Class type = Class.forName(className);

            // redirect to the project config screen
            Job project = createProject(type, name);
            rsp.sendRedirect(req.getContextPath()+'/'+project.getUrl()+"configure");
            return project;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
    }

    public synchronized void doCreateView( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        String name = req.getParameter("name");

        if(name==null) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        View v = new View(this, name);
        if(views==null)
            views = new Vector<View>();
        views.add(v);
        save();

        // redirect to the config screen
        rsp.sendRedirect("./"+v.getUrl()+"configure");
    }

    /**
     * Called once the user logs in. Just forward to the top page.
     */
    public synchronized void doLoginEntry( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        rsp.sendRedirect(req.getContextPath());
    }

    /**
     * Called once the user logs in. Just forward to the top page.
     */
    public synchronized void doLogout( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        HttpSession session = req.getSession(false);
        if(session!=null)
            session.invalidate();
        rsp.sendRedirect(req.getContextPath());
    }

    /**
     * Reloads the configuration.
     */
    public synchronized void doReload( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        load();
        rsp.sendRedirect(req.getContextPath());
    }



    public static boolean isWindows() {
        return File.pathSeparatorChar==';';
    }
}
