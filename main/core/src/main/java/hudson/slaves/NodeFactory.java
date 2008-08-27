package hudson.slaves;

import hudson.ExtensionPoint;
import hudson.security.AccessControlled;
import hudson.security.ACL;
import hudson.security.Permission;
import hudson.util.DescriptorList;
import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.model.Descriptor;

/**
 * Creates {@link EphemeralNode}s to dynamically expand/shrink the slaves attached to Hudson.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class NodeFactory implements ExtensionPoint, Describable<NodeFactory>, AccessControlled {

    /**
     * Uniquely identifies this {@link NodeFactory} instance among other instances in {@link Hudson#nodeFactories}.
     */
    public final String name;

    protected NodeFactory(String name) {
        this.name = name;
    }

    public ACL getACL() {
        return Hudson.getInstance().getAuthorizationStrategy().getACL(this);
    }

    public final void checkPermission(Permission permission) {
        getACL().checkPermission(permission);
    }

    public final boolean hasPermission(Permission permission) {
        return getACL().hasPermission(permission);
    }

    /**
     * All registered {@link NodeFactory} implementations.
     */
    public static final DescriptorList<NodeFactory> ALL = new DescriptorList<NodeFactory>();

    /**
     * Permission constant to control mutation operations on {@link NodeFactory}.
     *
     * This includes provisioning a new node, as well as removing it.
     */
    public static final Permission PROVISION = Hudson.ADMINISTER;
}
