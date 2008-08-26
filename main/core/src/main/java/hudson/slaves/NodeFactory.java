package hudson.slaves;

import hudson.ExtensionPoint;
import hudson.util.DescriptorList;
import hudson.model.Describable;

/**
 * Creates {@link EphemeralNode}s to dynamically expand/shrink the slaves attached to Hudson.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class NodeFactory implements ExtensionPoint, Describable<NodeFactory> {

    /**
     * All registered {@link NodeFactory} implementations.
     */
    public static final DescriptorList<NodeFactory> LIST = new DescriptorList<NodeFactory>();
}
