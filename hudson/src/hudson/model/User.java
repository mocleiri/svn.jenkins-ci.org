package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import hudson.scm.ChangeLogSet;

/**
 * Represents a user.
 * 
 * @author Kohsuke Kawaguchi
 */
public class User extends AbstractModelObject {

    private final String name;

    private String description ="Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Integer velit quam, elementum in, elementum euismod, sodales eget, quam. Nullam turpis lectus, dapibus eu, consectetuer non, pellentesque id, arcu. Pellentesque viverra erat. Morbi tempus convallis dolor. Cras elementum aliquet turpis. Nulla facilisi. Fusce in ante. Vivamus vulputate metus non turpis. Proin vestibulum magna id est. Integer vulputate, purus in mattis interdum, neque elit rutrum ante, ut viverra enim ligula ut ipsum. Donec vitae lorem. Aliquam a metus in pede laoreet dictum. Etiam at mauris. Proin arcu metus, fringilla quis, pretium aliquam, accumsan quis, risus.";


    private User(String name) {
        this.name = name;
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
        // save();
        
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
     * Gets the list of {@link Build}s that include changes by this user.
     *
     * TODO: do we need some index for this?
     */
    public List<Build> getBuilds() {
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
        return r;
    }


    public String toString() {
        return name;
    }

    /**
     * Keyed by {@link User#name}.
     */
    private static final Map<String,User> byName = new HashMap<String,User>();
}
