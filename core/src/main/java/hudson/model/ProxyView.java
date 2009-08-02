/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.model;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import java.io.IOException;
import java.util.Collection;
import javax.servlet.ServletException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ProxyView extends View {

    private String proxiedViewName;

    @DataBoundConstructor
    public ProxyView(String name) {
        super(name);

        if (Hudson.getInstance().getView(name) != null) {
            // if this is a valid global view name, let's assume the
            // user wants to show it
            proxiedViewName = name;
        }
    }

    public View getProxiedView() {
        if (proxiedViewName == null) {
            // just so we avoid errors just after creation
            return Hudson.getInstance().getPrimaryView();
        } else {
            return Hudson.getInstance().getView(proxiedViewName);
        }
    }

    public String getProxiedViewName() {
        return proxiedViewName;
    }

    public void setProxiedViewName(String proxiedViewName) {
        this.proxiedViewName = proxiedViewName;
    }

    @Override
    public Collection<TopLevelItem> getItems() {
        return getProxiedView().getItems();
    }

    @Override
    public boolean contains(TopLevelItem item) {
        return getProxiedView().contains(item);
    }

    @Override
    public void onJobRenamed(Item item, String oldName, String newName) {
        if (oldName.equals(proxiedViewName)) {
            proxiedViewName = newName;
        }
    }

    @Override
    protected void submit(StaplerRequest req) throws IOException, ServletException, FormException {
        String proxiedViewName = req.getSubmittedForm().getString("proxiedViewName");
        if (Hudson.getInstance().getView(proxiedViewName) == null) {
            throw new FormException("Not an existing global view", "proxiedViewName");
        }
        this.proxiedViewName = proxiedViewName;
    }

    @Override
    public Item doCreateItem(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        return getProxiedView().doCreateItem(req, rsp);
    }

    /**
     * Fails if a global view with the given name does not exist.
     */
    public FormValidation doViewExistsCheck(@QueryParameter String value) {
        checkPermission(View.CREATE);

        String view = Util.fixEmpty(value);
        if(view==null) return FormValidation.ok();

        if(Hudson.getInstance().getView(view)!=null)
            return FormValidation.ok();
        else
            return FormValidation.error(String.format("Global view %s does not exist", value));
    }

    @Extension
    public static class DescriptorImpl extends ViewDescriptor {

        @Override
        public String getDisplayName() {
            return "Include a global view";
        }

    }

}
