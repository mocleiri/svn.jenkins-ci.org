package hudson.plugins.deploy.glassfish;

import hudson.Extension;
import hudson.plugins.deploy.ContainerAdapterDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * GlassFish 2.x support.
 */
public class GlassFish2xAdapter extends GlassFishAdapter {
	@DataBoundConstructor
	public GlassFish2xAdapter(String home, String password, String userName, Integer adminPort) {
		super(home, password, userName, adminPort);
	}

	@Override
	protected String getContainerId() {
		return "glassfish2x";
	}

    @Extension
	public static final class DescriptorImpl extends ContainerAdapterDescriptor {
        public String getDisplayName() {
            return "GlassFish 2.x";
        }
    }
}
