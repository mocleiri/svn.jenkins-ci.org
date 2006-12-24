package hudson.model;

import java.util.List;

/**
 * List of all installed {@link Slave}s.
 * @author Kohsuke Kawaguchi
 */
public class Slaves {
    /**
     * List of all installed job types.
     */
    public static final List<SlaveDescriptor> LIST = Descriptor.toList(
        LegacySlave.DESCRIPTOR,
        AgentSlave.DESCRIPTOR
    );
}
