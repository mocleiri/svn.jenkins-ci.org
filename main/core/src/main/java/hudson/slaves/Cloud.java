package hudson.slaves;

import hudson.ExtensionPoint;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.AbstractModelObject;
import hudson.model.Label;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.util.DescriptorList;

import java.util.Collection;

/**
 * Creates {@link Node}s to dynamically expand/shrink the slaves attached to Hudson.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Cloud extends AbstractModelObject implements ExtensionPoint, Describable<Cloud>, AccessControlled {

    /**
     * Uniquely identifies this {@link Cloud} instance among other instances in {@link Hudson#clouds}.
     */
    public final String name;

    protected Cloud(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return name;
    }

    public String getSearchUrl() {
        return "cloud/"+name;
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
     * Provisions new {@link Node}s from this cloud.
     */
    public abstract Collection<PlannedNode> provision(Label label, int excessWorkload);

    /**
     * Returns true if this cloud is capable of provisioning new nodes for the given label.
     */
    public abstract boolean canProvision(Label label);

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
