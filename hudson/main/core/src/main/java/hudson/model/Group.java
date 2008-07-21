package hudson.model;

import hudson.CopyOnWrite;
import hudson.StructuredForm;
import hudson.XmlFile;
import hudson.model.Descriptor.FormException;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.util.RunList;
import hudson.util.XStream2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import com.thoughtworks.xstream.XStream;

/**
 * Represents a project/group
 *
 * @author Witold Delekta
 */
@ExportedBean
public class Group extends Actionable implements AccessControlled {

	public final static String DEFAULT = "Default";
    private static final Map<String,Group> byName = new HashMap<String,Group>();

    /**
     * Used to load/save user configuration.
     */
    private static final XStream XSTREAM = new XStream2();

    private static final Logger LOGGER = Logger.getLogger(Group.class.getName());

    static {
        XSTREAM.alias("group",Group.class);
    	create(DEFAULT, false);
    }

    private volatile transient String name;
    private volatile String description;
    private String[] users = new String[0];

    /**
     * List of {@link GroupProperty}s configured for this group.
     */
    @CopyOnWrite
    private volatile List<GroupProperty> properties = new ArrayList<GroupProperty>();

    public Group(String name) {
    	this.name = name;
        load();
    }

    /**
     * Loads the other data from disk if it's available.
     */
    private synchronized void load() {

        properties.clear();

        XmlFile config = getConfigFile();
        try {
            if(config.exists()) {
                config.unmarshal(this);
            } else {
            	config.write(this);
            }

            // remove nulls that have failed to load
            for (Iterator<GroupProperty> itr = properties.iterator(); itr.hasNext();) {
                if(itr.next()==null)
                    itr.remove();            
            }

            // allocate default instances if needed.
            // doing so after load makes sure that newly added group properties do get reflected
            for (GroupPropertyDescriptor d : GroupProperties.LIST) {
                if(getProperty(d.clazz)==null) {
                    GroupProperty up = d.newInstance(this);
                    if(up!=null)
                        properties.add(up);
                }
            }

            for (GroupProperty p : properties) {
                p.setGroup(this);
            }
            	
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load "+config,e);
        }
    }

    /**
     * The file we save our configuration.
     */
    protected final XmlFile getConfigFile() {
        return new XmlFile(XSTREAM,new File(Hudson.getInstance().getRootDir(),"groups/"+ name +"/config.xml"));
    }

    /**
     * Gets the group properties configured for this group.
     */
    public Map<Descriptor<GroupProperty>,GroupProperty> getProperties() {
        return Descriptor.toMap(properties);
    }

    /**
     * Updates the group object by adding a property.
     */
    public synchronized void addProperty(GroupProperty p) throws IOException {
    	GroupProperty old = getProperty(p.getClass());
        List<GroupProperty> ps = new ArrayList<GroupProperty>(properties);
        if(old!=null)
            ps.remove(old);
        ps.add(p);
        p.setGroup(this);
        properties = ps;
        save();
    }

    /**
     * List of all {@link GroupProperties} exposed primarily for the remoting API.
     */
    @Exported(name="property",inline=true)
    public List<GroupProperty> getAllProperties() {
        return Collections.unmodifiableList(properties);
    }
    
    /**
     * Gets the specific property, or null.
     */
    public <T extends GroupProperty> T getProperty(Class<T> clazz) {
        for (GroupProperty p : properties) {
            if(clazz.isInstance(p))
                return clazz.cast(p);
        }
        return null;
    }


    @Exported
    public String getName() {
    	return name;
    }

    public String getUrl() {
        return "group/" + name + '/';
    }

    public String getSearchUrl() {
        return "/group/"+name;
    }

    public synchronized boolean hasUser(String userid) {
       	int n = this.users.length;
       	for (String id: this.users) {
       		if (id.equals(userid)) return true;
       	}
       	return false;
    }

    public static Group createAsUser(String name, User user) throws IOException {
    	boolean existed = exists(name);
    	Group group = Group.create(name);
    	if (!existed) {
    		if (user!=null) {
    			group.users = new String[] { user.getId() };
    		}
    		group.save();
    	}
    	return group;
    }

    public static Group create(String name) {
    	return create(name, true);
    }
    public static Group create(String name, boolean secure) {
    	if (name==null || name.length()==0) {
    		name = DEFAULT;
    	}
        synchronized(byName) {
            Group group = byName.get(name);
            if(group==null) {
            	if (secure) {
            		Hudson.getInstance().checkPermission(CREATE);
            	}
                group = new Group(name);
                byName.put(name, group);
            }
            return group;
        }
    }


	public void checkPermission(Permission permission) {
		getACL().checkPermission(permission);
	}
	public boolean hasPermission(Permission permission) {
		return getACL().hasPermission(permission);
	}
	public ACL getACL() {
		return Hudson.getInstance().getAuthorizationStrategy().getACL(this);
	}

	public static boolean exists(String name) {
    	synchronized(byName) {
    		return byName.get(name)!=null;
    	}
    }

    /**
     * Gets all the users.
     */
    public static Collection<Group> getAll() {
        synchronized (byName) {
            return new ArrayList<Group>(byName.values());
        }
    }

    public static int getSize() {
    	return byName.size();
    }

	public String getDisplayName() {
		return name;
	}

    /**
     * Accepts the new description.
     */
    public synchronized void doSubmitDescription( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        checkPermission(CONFIGURE);
        req.setCharacterEncoding("UTF-8");
        description = req.getParameter("description");
        save();
        rsp.sendRedirect(".");  // go to the top page
    }

    /**
     * Accepts submission from the configuration page.
     */
    public synchronized void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        checkPermission(CONFIGURE);
        req.setCharacterEncoding("UTF-8");
        description = req.getParameter("description");
		String[] formUsers = req.getParameterValues("id");
		try {
			Set<String> list = new HashSet<String>();
			if (formUsers!=null) {
				for (String id: formUsers) {
					if (id.length()>0) {
						list.add(id);
					}
				}
			}
			users = list.toArray(new String[0]);

            JSONObject json = StructuredForm.get(req);
	
	        List<GroupProperty> props = new ArrayList<GroupProperty>();
            int i=0;
	        for (Descriptor<GroupProperty> d : GroupProperties.LIST) {
	            GroupProperty p = d.newInstance(req, json.getJSONObject("groupProperty"+(i++)));
	            if (p!=null) {
		            p.setGroup(this);
		            props.add(p);
	            } else {
	            	// leave previous version of value
	            	GroupProperty last = getProperty(d.clazz);
	            	if (last!=null) {
			            props.add(last);
	            	}
	            }
	        }
	
	        this.properties = props;
	    } catch (FormException e) {
	        sendError(e,req,rsp);
	    }
		
        save();
        rsp.sendRedirect(".");  // go to the top page
    }

    public synchronized void save() throws IOException {
        getConfigFile().write(this);
    }

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Api getApi() {
        return new Api(this);
    }

    public static void loadAllGroups() {
    	File dir = new File(Hudson.getInstance().getRootDir(), "groups");
    	if (dir.exists()) {
    		String[] groups = dir.list();
    		for (int i = 0; i<groups.length; i++) {
    			String name = groups[i];
    			if (new File(dir, name).isDirectory()) {
    				// Load group to byName hash
    				create(name, false);
    			}
    		}
    	}
    }

    public List<TopLevelItem> getItems() {

    	List<TopLevelItem> result = new ArrayList<TopLevelItem>();

    	List<TopLevelItem> list = Hudson.getInstance().getItems();
    	for (TopLevelItem it: list) {
    		if (Job.class.isInstance(it)) {
    			Job<?, ?> job = (Job<?, ?>) it;
    			if (name.equals(job.getGroup())) {
    				result.add(it);
    			}
    		}
    	}
    	return result;
	}

    public synchronized List<User> getUsers() {
    	int n = users.length;
    	List<User> result = new ArrayList<User>(n);
    	for (String id: this.users) {
    		User u = User.get(id);
    		result.add(u);
    	}
    	return result;
    }

    public Collection<User> getAllUsers() {
    	return User.getAll();
    }

    @Exported
    public People getPeople() {
    	return new People();
    }

    @ExportedBean
    public class People {
    	@Exported
    	public List<User> users;

    	public People() {
    		synchronized (Group.this) {
    			users = getUsers();
    		}
    	}
        public Group getParent() {
            return Group.this;
        }
        public Api getApi() {
            return new Api(this);
        }
    }

    /**
     * Returns the {@link Action}s associated with all groups.
     *
     * <p>
     * Adding {@link Action} is primarily useful for plugins to contribute
     * an item to the navigation bar of the group page. See existing {@link Action}
     * implementation for it affects the GUI.
     *
     * <p>
     * To register an {@link Action}, write code like
     * {@code Group.getClassActions().add(...)}
     *
     * @return
     *      Live list where the changes can be made. Can be empty but never null.
     */
    public static List<Action> getClassActions() {
        return classActions;
    }

    private static final List<Action> classActions = new CopyOnWriteArrayList<Action>();

    public List<Action> getInstanceActions() {
    	return super.getActions();
    }

    /**
     * @return
     *    Class and instance actions
     */
    public List<Action> getActions() {
    	List<Action> list = new ArrayList<Action>(super.getActions());
    	list.addAll(classActions);
        return list;
    }


    public void doRssAll( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        rss(req, rsp, " all builds", getBuilds());
    }

    public void doRssFailed( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        rss(req, rsp, " failed builds", getBuilds().failureOnly());
    }
    
    public RunList getBuilds() {
    	return new RunList(new GroupView(Hudson.getInstance(), this, null));
    }

    private void rss(StaplerRequest req, StaplerResponse rsp, String suffix, RunList runs) throws IOException, ServletException {
        RSS.forwardToRss(getDisplayName()+ suffix, getUrl(),
            runs.newBuilds(), Run.FEED_ADAPTER, req, rsp );
    }

    public static final PermissionGroup PERMISSIONS = new PermissionGroup(Group.class,Messages._Group_Permissions_Title());
    public static final Permission CREATE = new Permission(PERMISSIONS, "Create", Permission.CREATE);
    // Add jobs
	public static final Permission CONFIGURE = new Permission(PERMISSIONS, "Configure", Permission.CONFIGURE);
	// Add users to the group
	public static final Permission ADDJOB = new Permission(PERMISSIONS, "Add_Job", Permission.CONFIGURE);
}
