package hudson.slaves;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.util.DescriptorList;

/**
 * Creates {@link Node}s to dynamically expand/shrink the slaves attached to Hudson.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Cloud implements ExtensionPoint, Describable<Cloud>, AccessControlled {

    /**
     * Uniquely identifies this {@link Cloud} instance among other instances in {@link Hudson#clouds}.
     */
    public final String name;

    protected Cloud(String name) {
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
     * All registered {@link Cloud} implementations.
     */
    public static final DescriptorList<Cloud> ALL = new DescriptorList<Cloud>();

    /**
     * Permission constant to control mutation operations on {@link Cloud}.
     *
     * This includes provisioning a new node, as well as removing it.
     */
    public static final Permission PROVISION = Hudson.ADMINISTER;
}
