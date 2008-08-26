package hudson.slaves;

import hudson.model.Node;

/**
 * {@link Node}s that are created by {@link NodeFactory} and hence not persisted as configuration by itself.
 *
 * @author Kohsuke Kawaguchi
 */
public interface EphemeralNode extends Node {
    /**
     * Gets the {@link NodeFactory} that created this {@link EphemeralNode}.
     *
     * @return
     *      never null.
     */
    public NodeFactory getFactory();
}
