package hudson.model;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.util.FormValidation;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.acegisecurity.AccessDeniedException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * A UserProperty that remembers user-private views.
 *
 * @author Tom Huybrechts
 */
public class PrivateViewsProperty extends UserProperty implements ViewGroup, Action, AccessControlled {

    private static final Logger log = Logger.getLogger(PrivateViewsProperty.class.getName());

    private String primaryViewName;
    private CopyOnWriteArrayList<View> views = new CopyOnWriteArrayList<View>();

    @DataBoundConstructor
    public PrivateViewsProperty(String primaryViewName) {
        this.primaryViewName = primaryViewName;
    }

    private PrivateViewsProperty() {
        views.add(new MyView("My Jobs", this));
        primaryViewName = views.get(0).getViewName();
    }

    public String getPrimaryViewName() {
        return primaryViewName;
    }

    public void setPrimaryViewName(String primaryViewName) {
        this.primaryViewName = primaryViewName;
    }

    public User getUser() {
        return user;
    }

    ///// ViewGroup methods /////
    public String getUrl() {
        return user.getUrl() + "/private-views/";
    }

    public void save() throws IOException {
        user.save();
    }

    public Collection<View> getViews() {
        List<View> copy = new ArrayList<View>(views);
        Collections.sort(copy, View.SORTER);
        return copy;
    }

    public View getView(String name) {
        for (View v : views) {
            if (v.getViewName().equals(name)) {
                return v;
            }
        }
        return null;
    }

    public void deleteView(View view) throws IOException {
        if (views.size() <= 1) {
            throw new IllegalStateException();
        }
        views.remove(view);
        if (view.getViewName().equals(primaryViewName)) {
            primaryViewName = views.get(0).getViewName();
        }
        save();
    }

    public void onViewRenamed(View view, String oldName, String newName) {
        if (primaryViewName.equals(oldName)) {
            primaryViewName = newName;
            try {
                save();
            } catch (IOException ex) {
                log.log(Level.SEVERE, "error while saving user " + user.getId(), ex);
            }
        }
    }

    public void addView(View view) throws IOException {
        views.add(view);
        save();
    }

    public View getPrimaryView() {
        if (primaryViewName != null) {
            return getView(primaryViewName);
        } else {
            return getViews().iterator().next();
        }
    }

    public HttpResponse doIndex() {
        return new HttpRedirect("view/" + getPrimaryView().getViewName());
    }

    public synchronized void doCreateView(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException, ParseException, FormException {
        checkPermission(View.CREATE);
        addView(View.create(req, rsp, this));
    }

    /**
     * Checks if a private view with the given name exists.
     */
    public FormValidation doViewExistsCheck(@QueryParameter String value) {
        checkPermission(View.CREATE);

        String view = Util.fixEmpty(value);
        if(view==null) return FormValidation.ok();

        if(getView(view)==null)
            return FormValidation.ok();
        else
            return FormValidation.error(Messages.Hudson_ViewAlreadyExists(view));
    }

    public ACL getACL() {
        return user.getACL();
    }

    public void checkPermission(Permission permission) throws AccessDeniedException {
        getACL().checkPermission(permission);
    }

    public boolean hasPermission(Permission permission) {
        return getACL().hasPermission(permission);
    }

    ///// Action methods /////
    public String getDisplayName() {
        return "Private Views";
    }

    public String getIconFileName() {
        return "notepad.gif";
    }

    public String getUrlName() {
        return "private-views";
    }

    @Extension
    public static class DescriptorImpl extends UserPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Private Views";
        }

        @Override
        public UserProperty newInstance(User user) {
            return new PrivateViewsProperty();
        }
    }

}
