/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Tom Huybrechts
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.model;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.security.ACL;
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
public class MyViewsProperty extends UserProperty implements ViewGroup, Action {

    private static final Logger log = Logger.getLogger(MyViewsProperty.class.getName());

    private String primaryViewName;
    private CopyOnWriteArrayList<View> views = new CopyOnWriteArrayList<View>();

    @DataBoundConstructor
    public MyViewsProperty(String primaryViewName) {
        this.primaryViewName = primaryViewName;
    }

    private MyViewsProperty() {
		views.add(new AllView(hudson.model.Messages.Hudson_ViewName(), this));
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
        return user.getUrl() + "/my-views/";
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
     * An error is returned if exists==true but the view does not exist.
     * An error is also returned if exists==false but the view does exist.
     **/
    public FormValidation doViewExistsCheck(@QueryParameter String value, @QueryParameter boolean exists) {
        checkPermission(View.CREATE);

        String view = Util.fixEmpty(value);
        if (view == null) return FormValidation.ok();
        if (exists) {
        	return (getView(view)!=null) ?
            		FormValidation.ok() :
            		FormValidation.error(String.format("A view with name \"%s\" does not exist", view));
        } else {
        	return (getView(view)==null) ?
        			FormValidation.ok() :
        				FormValidation.error(String.format("A view with name \"%s\" already exists", view));
        }
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
        return "My Views";
    }

    public String getIconFileName() {
        return "user.gif";
    }

    public String getUrlName() {
        return "my-views";
    }

    @Extension
    public static class DescriptorImpl extends UserPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "My Views";
        }

        @Override
        public UserProperty newInstance(User user) {
            return new MyViewsProperty();
        }
    }
    
    @Extension
    public static class GlobalAction implements RootAction {

		public String getDisplayName() {
			return "My Views";
		}

		public String getIconFileName() {
			// do not show when not logged in
			if (User.current() == null ) {
				return null;
			} 
			
			return "user.gif";
		}

		public String getUrlName() {
			return "/me/my-views";
		}
		
		public HttpResponse doIndex() {
			return new HttpRedirect("/me/my-views");
		}
    	
    }

}
