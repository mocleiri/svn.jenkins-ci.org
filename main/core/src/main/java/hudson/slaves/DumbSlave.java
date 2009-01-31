package hudson.slaves;

import java.util.Collections;
import java.util.List;
import java.io.IOException;

import hudson.model.Slave;
import hudson.model.Descriptor.FormException;

import org.kohsuke.stapler.DataBoundConstructor;

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
			ComputerLauncher launcher, RetentionStrategy retentionStrategy, List<NodeProperty<?>> nodeProperties)
            throws FormException, IOException {
		super(name, description, remoteFS, numExecutors, mode, label, launcher,
				retentionStrategy, nodeProperties);
	}

	@Deprecated
	public DumbSlave(String name, String description, String remoteFS,
			String numExecutors, Mode mode, String label,
			ComputerLauncher launcher, RetentionStrategy retentionStrategy)
            throws FormException, IOException {
		super(name, description, remoteFS, numExecutors, mode, label, launcher,
				retentionStrategy, Collections.EMPTY_LIST);
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
