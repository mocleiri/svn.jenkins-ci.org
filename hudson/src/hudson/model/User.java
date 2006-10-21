package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

import hudson.scm.ChangeLogSet;
import hudson.XmlFile;
import hudson.util.XStream2;
import com.thoughtworks.xstream.XStream;

/**
 * Represents a user.
 * 
 * @author Kohsuke Kawaguchi
 */
public class User extends AbstractModelObject {

    private transient final String name;

    private String description;


    private User(String name) {
        this.name = name;

        // load the other data from disk if it's available
        XmlFile config = getConfigFile();
        try {
            if(config.exists())
            config.unmarshal(this);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load "+config,e);
        }
    }

    public String getUrl() {
        return "user/"+name;
    }


    public String getDescription() {
        return description;
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



    public static User get(String name) {
        if(name==null)
            return null;
        synchronized(byName) {
            User u = byName.get(name);
            if(u==null) {
                u = new User(name);
                byName.put(name,u);
            }
            return u;
        }
    }

    /**
     * Returns the user name.
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * Gets the list of {@link Build}s that include changes by this user,
     * by the timestamp order.
     *
     * <p>
     * The name is somewhat unintuitive to use "builds" for the view name.
     * TODO: do we need some index for this?
     *
     */
    public List<Build> getParticipatingBuilds() {
        List<Build> r = new ArrayList<Build>();
        for (Project p : Hudson.getInstance().getProjects()) {
            for (Build b : p.getBuilds()) {
                for (ChangeLogSet.Entry e : b.getChangeSet()) {
                    if(e.getAuthor()==this) {
                        r.add(b);
                        break;
                    }
                }
            }
        }
        Collections.sort(r,Run.ORDER_BY_DATE);
        return r;
    }


    public String toString() {
        return name;
    }

    /**
     * The file we save our configuration.
     */
    protected final XmlFile getConfigFile() {
        return new XmlFile(XSTREAM,new File(Hudson.getInstance().getRootDir(),"users/"+name+"/config.xml"));
    }

    /**
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        XmlFile config = getConfigFile();
        config.mkdirs();
        config.write(this);
    }

    /**
     * Keyed by {@link User#name}.
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
}
