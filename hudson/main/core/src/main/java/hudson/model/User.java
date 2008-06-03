package hudson.model;

import com.thoughtworks.xstream.XStream;
import hudson.CopyOnWrite;
import hudson.FeedAdapter;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.Descriptor.FormException;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.util.RunList;
import hudson.util.XStream2;
import org.acegisecurity.Authentication;
import org.acegisecurity.providers.anonymous.AnonymousAuthenticationToken;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a user.
 *
 * <p>
 * In Hudson, {@link User} objects are created in on-demand basis;
 * for example, when a build is performed, its change log is computed
 * and as a result commits from users who Hudson has never seen may be discovered.
 * When this happens, new {@link User} object is created.
 *
 * <p>
 * If the persisted record for an user exists, the information is loaded at
 * that point, but if there's no such record, a fresh instance is created from
 * thin air (this is where {@link UserPropertyDescriptor#newInstance(User)} is
 * called to provide initial {@link UserProperty} objects.
 *
 * <p>
 * Such newly created {@link User} objects will be simply GC-ed without
 * ever leaving the persisted record, unless {@link User#save()} method
 * is explicitly invoked (perhaps as a result of a browser submitting a
 * configuration.)
 *
 *
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public class User extends AbstractModelObject implements AccessControlled {

    private transient final String id;

    private volatile String fullName;

    private volatile String description;

    /**
     * List of {@link UserProperty}s configured for this project.
     */
    @CopyOnWrite
    private volatile List<UserProperty> properties = new ArrayList<UserProperty>();


    private User(String id) {
        this.id = id;
        this.fullName = id;   // fullName defaults to name
        load();
    }

    /**
     * Loads the other data from disk if it's available.
     */
    private synchronized void load() {
        properties.clear();

        XmlFile config = getConfigFile();
        try {
            if(config.exists())
                config.unmarshal(this);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load "+config,e);
        }

        // remove nulls that have failed to load
        for (Iterator<UserProperty> itr = properties.iterator(); itr.hasNext();) {
            if(itr.next()==null)
                itr.remove();            
        }

        // allocate default instances if needed.
        // doing so after load makes sure that newly added user properties do get reflected
        for (UserPropertyDescriptor d : UserProperties.LIST) {
            if(getProperty(d.clazz)==null) {
                UserProperty up = d.newInstance(this);
                if(up!=null)
                    properties.add(up);
            }
        }

        for (UserProperty p : properties)
            p.setUser(this);
    }

    @Exported
    public String getId() {
        return id;
    }

    public String getUrl() {
        return "user/"+id;
    }

    public String getSearchUrl() {
        return "/user/"+id;
    }

    /**
     * The URL of the user page.
     */
    @Exported(visibility=999)
    public String getAbsoluteUrl() {
        return Stapler.getCurrentRequest().getRootPath()+'/'+getUrl();
    }

    /**
     * Gets the human readable name of this user.
     * This is configurable by the user.
     *
     * @return
     *      never null.
     */
    @Exported(visibility=999)
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the human readable name of thie user.
     */
    public void setFullName(String name) {
        if(Util.fixEmptyAndTrim(name)==null)    name=id;
        this.fullName = name;
    }

    @Exported
    public String getDescription() {
        return description;
    }

    /**
     * Gets the user properties configured for this user.
     */
    public Map<Descriptor<UserProperty>,UserProperty> getProperties() {
        return Descriptor.toMap(properties);
    }

    /**
     * Updates the user object by adding a property.
     */
    public synchronized void addProperty(UserProperty p) throws IOException {
        UserProperty old = getProperty(p.getClass());
        List<UserProperty> ps = new ArrayList<UserProperty>(properties);
        if(old!=null)
            ps.remove(old);
        ps.add(p);
        p.setUser(this);
        properties = ps;
        save();
    }

    /**
     * List of all {@link UserProperties} exposed primarily for the remoting API.
     */
    @Exported(name="property",inline=true)
    public List<UserProperty> getAllProperties() {
        return Collections.unmodifiableList(properties);
    }
    
    /**
     * Gets the specific property, or null.
     */
    public <T extends UserProperty> T getProperty(Class<T> clazz) {
        for (UserProperty p : properties) {
            if(clazz.isInstance(p))
                return clazz.cast(p);
        }
        return null;
    }

    /**
     * Accepts the new description.
     */
    public synchronized void doSubmitDescription( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");

        description = req.getParameter("description");
        save();
        
        rsp.sendRedirect(".");  // go to the top page
    }

    /**
     * Gets the fallback "unknown" user instance.
     * <p>
     * This is used to avoid null {@link User} instance.
     */
    public static User getUnknown() {
        return get("unknown");
    }

    /**
     * Gets the {@link User} object by its id.
     *
     * @param create
     *      If true, this method will never return null for valid input
     *      (by creating a new {@link User} object if none exists.)
     *      If false, this method will return null if {@link User} object
     *      with the given name doesn't exist.
     */
    public static User get(String id, boolean create) {
        if(id==null)
            return null;
        id = id.replace('\\', '_').replace('/', '_');
        
        synchronized(byName) {
            User u = byName.get(id);
            if(u==null && create) {
                u = new User(id);
                byName.put(id,u);
            }
            return u;
        }
    }

    /**
     * Gets the {@link User} object by its id.
     */
    public static User get(String id) {
        return get(id,true);
    }

    /**
     * Gets the {@link User} object representing the currently logged-in user, or null
     * if the current user is anonymous.
     * @since 1.172
     */
    public static User current() {
        Authentication a = Hudson.getAuthentication();
        if(a instanceof AnonymousAuthenticationToken)
            return null;
        return get(a.getPrincipal().toString());
    }

    /**
     * Gets all the users.
     */
    public static Collection<User> getAll() {
        synchronized (byName) {
            return new ArrayList<User>(byName.values());
        }
    }

    /**
     * Reloads the configuration from disk.
     */
    public static void reload() {
        // iterate over an array to be concurrency-safe
        for( User u : byName.values().toArray(new User[0]) )
            u.load();
    }

    /**
     * Returns the user name.
     */
    public String getDisplayName() {
        return getFullName();
    }

    /**
     * Gets the list of {@link Build}s that include changes by this user,
     * by the timestamp order.
     * 
     * TODO: do we need some index for this?
     */
    public List<AbstractBuild> getBuilds() {
        List<AbstractBuild> r = new ArrayList<AbstractBuild>();
        for (AbstractProject<?,?> p : Hudson.getInstance().getAllItems(AbstractProject.class))
            for (AbstractBuild<?,?> b : p.getBuilds())
                if(b.hasParticipant(this))
                    r.add(b);
        Collections.sort(r,Run.ORDER_BY_DATE);
        return r;
    }

    /**
     * Gets all the {@link AbstractProject}s that this user has committed to.
     * @since 1.191
     */
    public Set<AbstractProject<?,?>> getProjects() {
        Set<AbstractProject<?,?>> r = new HashSet<AbstractProject<?,?>>();
        for (AbstractProject<?,?> p : Hudson.getInstance().getAllItems(AbstractProject.class))
            if(p.hasParticipant(this))
                r.add(p);
        return r;
    }

    public String toString() {
        return fullName;
    }

    /**
     * The file we save our configuration.
     */
    protected final XmlFile getConfigFile() {
        return new XmlFile(XSTREAM,new File(Hudson.getInstance().getRootDir(),"users/"+ id +"/config.xml"));
    }

    /**
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        getConfigFile().write(this);
    }

    /**
     * Exposed remote API.
     */
    public Api getApi() {
        return new Api(this);
    }

    /**
     * Accepts submission from the configuration page.
     */
    public void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        checkPermission(Hudson.ADMINISTER);

        req.setCharacterEncoding("UTF-8");

        try {
            fullName = req.getParameter("fullName");
            description = req.getParameter("description");

            List<UserProperty> props = new ArrayList<UserProperty>();
            for (Descriptor<UserProperty> d : UserProperties.LIST) {
                UserProperty p = d.newInstance(req, null);
                p.setUser(this);
                props.add(p);
            }
            this.properties = props;

            save();

            rsp.sendRedirect(".");
        } catch (FormException e) {
            sendError(e,req,rsp);
        }
    }

    public void doRssAll( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        rss(req, rsp, " all builds", RunList.fromRuns(getBuilds()));
    }

    public void doRssFailed( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        rss(req, rsp, " regression builds", RunList.fromRuns(getBuilds()).regressionOnly());
    }

    private void rss(StaplerRequest req, StaplerResponse rsp, String suffix, RunList runs) throws IOException, ServletException {
        RSS.forwardToRss(getDisplayName()+ suffix, getUrl(),
            runs.newBuilds(), FEED_ADAPTER, req, rsp );
    }


    /**
     * Keyed by {@link User#id}. This map is used to ensure
     * singleton-per-id semantics of {@link User} objects.
     */
    private static final Map<String,User> byName = new HashMap<String,User>();

    /**
     * Used to load/save user configuration.
     */
    private static final XStream XSTREAM = new XStream2();

    private static final Logger LOGGER = Logger.getLogger(User.class.getName());

    static {
        XSTREAM.alias("user",User.class);
    }

    /**
     * {@link FeedAdapter} to produce build status summary in the feed.
     */
    public static final FeedAdapter<Run> FEED_ADAPTER = new FeedAdapter<Run>() {
        public String getEntryTitle(Run entry) {
            return entry+" : "+entry.getBuildStatusSummary().message;
        }

        public String getEntryUrl(Run entry) {
            return entry.getUrl();
        }

        public String getEntryID(Run entry) {
            return "tag:"+entry.getParent().getName()+':'+entry.getId();
        }

        public String getEntryDescription(Run entry) {
            // TODO: provide useful details
            return null;
        }

        public Calendar getEntryTimestamp(Run entry) {
            return entry.getTimestamp();
        }
    };


    public ACL getACL() {
    	return Hudson.getInstance().getAuthorizationStrategy().getACL(this);
    }
    
	public void checkPermission(Permission permission) {
		getACL().checkPermission(permission);
	}

	public boolean hasPermission(Permission permission) {
		return getACL().hasPermission(permission);
	}

}
