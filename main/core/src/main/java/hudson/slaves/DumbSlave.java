package hudson.slaves;

import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;

import hudson.Functions;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.model.Descriptor.FormException;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Default {@link Slave} implementation for computers that do not belong to a
 * higher level structure, like grid or cloud.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class DumbSlave extends Slave {
	@DataBoundConstructor
	public DumbSlave(String name, String description, String remoteFS,
			String numExecutors, Mode mode, String label,
			ComputerLauncher launcher, RetentionStrategy retentionStrategy)
			throws FormException {
		super(name, description, remoteFS, numExecutors, mode, label, launcher,
				retentionStrategy);
	}

	public DescriptorImpl getDescriptor() {
		return DescriptorImpl.INSTANCE;
	}

	public static final class DescriptorImpl extends NodeDescriptor {
		public static final DescriptorImpl INSTANCE = new DescriptorImpl();

		public String getDisplayName() {
			return Messages.DumbSlave_displayName();
		}

	}

	static {
		NodeDescriptor.ALL.add(DescriptorImpl.INSTANCE);
	}
}
