package hudson.plugins.deploy;

import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.net.URL;

/**
 * Tomcat 5.x.
 *
 * @author Kohsuke Kawaguchi
 */
public class Tomcat4xAdapter extends TomcatAdapter {

    @DataBoundConstructor
    public Tomcat4xAdapter(String userName, String password, URL url) {
        super(userName, password, url);
    }

    public String getContainerId() {
        return "tomcat4x";
    }

    public Descriptor<ContainerAdapter> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor<ContainerAdapter> DESCRIPTOR = new Descriptor<ContainerAdapter>(Tomcat4xAdapter.class) {
        public String getDisplayName() {
            return "Tomcat 4.x";
        }
    };
}
