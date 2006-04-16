package hudson.model;

import com.thoughtworks.xstream.XStream;
import hudson.XmlFile;
import hudson.Util;
import hudson.Launcher;
import hudson.scm.CVSSCM;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMManager;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import org.apache.tools.ant.taskdefs.Copy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Map.Entry;
import java.text.ParseException;

/**
 * Root object of the system.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Hudson extends JobCollection implements Node {
    private transient final Queue queue = new Queue();

    /**
     * {@link Computer}s in this Hudson system. Read-only.
     */
    private transient final Map<Node,Computer> computers = new HashMap<Node,Computer>();

    /**
     * Number of executors of the master node.
     */
    private int numExecutors = 2;

    /**
     * False to enable anyone to do anything.
     */
    private boolean useSecurity = false;

    /**
     * Message displayed in the top page.
     */
    private String systemMessage;

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
    private transient boolean shuttingDown;

    private List<JDK> jdks;

    /**
     * Set of installed cluster nodes.
     */
    private List<Slave> slaves;

    /**
     * Quiet period.
     *
     * This is {@link Integer} so that we can initialize it to '5' for upgrading users.
     */
    /*package*/ Integer quietPeriod;

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

        updateComputerList();
    }

    /**
     * If you are calling it o Hudson something is wrong.
     *
     * @deprecated
     */
    public String getNodeName() {
        return "";
    }

    public String getViewMessage() {
        return systemMessage;
    }

    /**
     * Synonym to {@link #getViewMessage()}.
     */
    public String getSystemMessage() {
        return systemMessage;
    }

    public String getDescription() {
        return "the master Hudson node";
    }

    public Launcher createLauncher(BuildListener listener) {
        return new Launcher(listener);
    }

    /**
     * Updates {@link #computers} by using {@link #getSlaves()}.
     *
     * <p>
     * This method tries to reuse existing {@link Computer} objects
     * so that we won't upset {@link Executor}s running in it.
     */
    private void updateComputerList() {
        synchronized(computers) {
            Map<String,Computer> byName = new HashMap<String,Computer>();
            for (Computer c : computers.values())
                byName.put(c.getNode().getNodeName(),c);

            Set<Computer> old = new HashSet<Computer>(computers.values());
            Set<Computer> used = new HashSet<Computer>();

            updateComputer(this, byName, used);
            for (Slave s : getSlaves())
                updateComputer(s, byName, used);

            // find out what computers are removed, and kill off all executors.
            // when all executors exit, it will be removed from the computers map.
            // so don't remove too quickly
            old.removeAll(used);
            for (Computer c : old) {
                c.kill();
            }
        }
    }

    private void updateComputer(Node n, Map<String,Computer> byNameMap, Set<Computer> used) {
        Computer c;
        c = byNameMap.get(n.getNodeName());
        if(c==null) {
            if(n.getNumExecutors()>0)
                computers.put(n,c=new Computer(n));
        } else {
            c.setNode(n);
        }
        used.add(c);
    }

    /*package*/ void removeComputer(Computer computer) {
        synchronized(computers) {
            Iterator<Entry<Node,Computer>> itr=computers.entrySet().iterator();
            while(itr.hasNext()) {
                if(itr.next().getValue()==computer) {
                    itr.remove();
                    return;
                }
            }
        }
        throw new IllegalStateException("Trying to remove unknown computer");
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
     * Gets the read-only list of all {@link Computer}s.
     */
    public Computer[] getComputers() {
        synchronized(computers) {
            Computer[] r = computers.values().toArray(new Computer[computers.size()]);
            Arrays.sort(r,new Comparator<Computer>() {
                public int compare(Computer lhs, Computer rhs) {
                    if(lhs.getNode()==Hudson.this)  return -1;
                    if(rhs.getNode()==Hudson.this)  return 1;
                    return lhs.getNode().getNodeName().compareTo(rhs.getNode().getNodeName());
                }
            });
            return r;
        }
    }

    public Computer getComputer(String name) {
        synchronized(computers) {
            for (Computer c : computers.values()) {
                if(c.getNode().getNodeName().equals(name))
                    return c;
            }
        }
        return null;
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
     * Gets the slave node of the give name, hooked under this Hudson.
     */
    public Slave getSlave(String name) {
        for (Slave s : getSlaves()) {
            if(s.getNodeName().equals(name))
                return s;
        }
        return null;
    }

    public List<Slave> getSlaves() {
        if(slaves ==null)
            slaves = new ArrayList<Slave>();
        return slaves;
    }

    /**
     * Gets the system default quiet period.
     */
    public int getQuietPeriod() {
        return quietPeriod!=null ? quietPeriod : 5;
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
     * Called in response to {@link Job#doDoDelete(StaplerRequest, StaplerResponse)}
     */
    /*package*/ void deleteJob(Job job) throws IOException {
        jobs.remove(job.getName());
        if(views!=null) {
            for (View v : views) {
                synchronized(v) {
                    v.jobNames.remove(job.getName());
                }
            }
            save();
        }
    }

    /**
     * The file we save our configuration.
     */
    private XmlFile getConfigFile() {
        XStream xs = new XStream();
        xs.alias("hudson",Hudson.class);
        return new XmlFile(xs, new File(root,"config.xml"));
    }

    public int getNumExecutors() {
        return numExecutors;
    }

    public Mode getMode() {
        return Mode.NORMAL;
    }

    private synchronized void load() throws IOException {
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
        synchronized(computers) {
            for( Computer c : computers.values() )
                c.interrupt();
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
        if(!Hudson.adminCheck(req,rsp))
            return;

        useSecurity = req.getParameter("use_security")!=null;

        numExecutors = Integer.parseInt(req.getParameter("numExecutors"));
        quietPeriod = Integer.parseInt(req.getParameter("quiet_period"));

        systemMessage = Util.nullify(req.getParameter("system_message"));

        {// update slave list
            slaves.clear();
            String[] names = req.getParameterValues("slave_name");
            String[] descriptions = req.getParameterValues("slave_description");
            String[] executors = req.getParameterValues("slave_executors");
            String[] cmds = req.getParameterValues("slave_command");
            String[] rfs = req.getParameterValues("slave_remoteFS");
            String[] lfs = req.getParameterValues("slave_localFS");
            String[] mode = req.getParameterValues("slave_mode");
            if(names!=null && descriptions!=null && executors!=null && cmds!=null && rfs!=null && lfs!=null && mode!=null) {
                int len = Util.min(names.length,descriptions.length,executors.length,cmds.length,rfs.length, lfs.length, mode.length);
                for(int i=0;i<len;i++) {
                    int n = 2;
                    try {
                        n = Integer.parseInt(executors[i].trim());
                    } catch(NumberFormatException e) {
                        // ignore
                    }
                    slaves.add(new Slave(names[i],descriptions[i],cmds[i],rfs[i],new File(lfs[i]),n,Node.Mode.valueOf(mode[i])));
                }
            }

            updateComputerList();
        }

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

    public synchronized Job doCreateJob( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        if(!Hudson.adminCheck(req,rsp))
            return null;

        String name = req.getParameter("name").trim();
        String className = req.getParameter("type");
        String mode = req.getParameter("mode");

        try {
            checkGoodName(name);
        } catch (ParseException e) {
            sendError(e,req,rsp);
            return null;
        }

        if(getJob(name)!=null) {
            sendError("A job already exists with the name '"+name+"'",req,rsp);
            return null;
        }

        if(mode==null) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        Job result;

        if(mode.equals("newJob")) {
            if(className==null) {
                // request forged?
                rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }
            try {
                Class type = Class.forName(className);

                // redirect to the project config screen
                result = createProject(type, name);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }
        } else {
            Job src = getJob(req.getParameter("from"));
            if(src==null) {
                rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }

            result = createProject(src.getClass(),name);

            // copy config
            Copy cp = new Copy();
            cp.setProject(new org.apache.tools.ant.Project());
            cp.setTofile(result.getConfigFile());
            cp.setFile(src.getConfigFile());
            cp.setOverwrite(true);
            cp.execute();

            // reload from the new config
            result = Job.load(this,result.root);
            jobs.put(name,result);
        }

        rsp.sendRedirect(req.getContextPath()+'/'+result.getUrl()+"configure");
        return result;
    }

    public synchronized void doCreateView( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        String name = req.getParameter("name");

        try {
            checkGoodName(name);
        } catch (ParseException e) {
            sendError(e, req, rsp);
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
     * Check if the given name is suitable as a name
     * for job, view, etc.
     *
     * @throws ParseException
     *      if the given name is not good
     */
    public static void checkGoodName(String name) throws ParseException {
        if(name==null || name.length()==0)
            throw new ParseException("No name is specified",0);

        for( int i=0; i<name.length(); i++ ) {
            char ch = name.charAt(i);
            if(Character.isISOControl(ch))
                throw new ParseException("No control code is allowed",i);
            if("?*()/\\%!@#$^&|<>".indexOf(ch)!=-1)
                throw new ParseException("'"+ch+"' is an unsafe character",i);
        }

        // looks good
    }

    /**
     * Called once the user logs in. Just forward to the top page.
     */
    public synchronized void doLoginEntry( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        rsp.sendRedirect(req.getContextPath()+"/");
    }

    /**
     * Called once the user logs in. Just forward to the top page.
     */
    public synchronized void doLogout( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        HttpSession session = req.getSession(false);
        if(session!=null)
            session.invalidate();
        rsp.sendRedirect(req.getContextPath()+"/");
    }

    /**
     * Reloads the configuration.
     */
    public synchronized void doReload( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        load();
        rsp.sendRedirect(req.getContextPath()+"/");
    }

    /**
     * Serves static resources without the "Last-Modified" header to work around
     * a bug in Firefox.
     *
     * @see https://bugzilla.mozilla.org/show_bug.cgi?id=89419
     */
    public void doNocacheImages( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        String path = req.getRestOfPath();

        if(path.length()==0)
            path = "/";

        if(path.indexOf("..")!=-1 || path.length()<1) {
            // don't serve anything other than files in the artifacts dir
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        File f = new File(req.getServletContext().getRealPath("/images"),path.substring(1));
        if(!f.exists()) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if(f.isDirectory()) {
            // listing not allowed
            rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        FileInputStream in = new FileInputStream(f);
        // serve the file
        String contentType = req.getServletContext().getMimeType(f.getPath());
        rsp.setContentType(contentType);
        rsp.setContentLength((int)f.length());
        byte[] buf = new byte[1024];
        int len;
        while((len=in.read(buf))>0)
            rsp.getOutputStream().write(buf,0,len);
        in.close();
    }



    public static boolean isWindows() {
        return File.pathSeparatorChar==';';
    }


    /**
     * Returns all {@code CVSROOT} strings used in the current Hudson installation.
     *
     * <p>
     * Ideally this shouldn't be defined in here
     * but EL doesn't provide a convenient way of invoking a static function,
     * so I'm putting it here for now.
     */
    public Set<String> getAllCvsRoots() {
        Set<String> r = new TreeSet<String>();
        for( Job j : getJobs() ) {
            if (j instanceof Project) {
                Project p = (Project) j;
                SCM scm = p.getScm();
                if (scm instanceof CVSSCM) {
                    CVSSCM cvsscm = (CVSSCM) scm;
                    r.add(cvsscm.getCvsRoot());
                }
            }
        }

        return r;
    }

    public static boolean adminCheck(StaplerRequest req,StaplerResponse rsp) throws IOException {
        if(!getInstance().isUseSecurity())
            return true;

        if(req.isUserInRole("admin"))
            return true;

        rsp.sendError(StaplerResponse.SC_FORBIDDEN);
        return false;
    }
}
