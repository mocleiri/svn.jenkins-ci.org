package hudson.model;

import hudson.ExtensionPoint;
import hudson.Plugin;

import org.kohsuke.stapler.export.ExportedBean;

/**
 * Extensible property of {@link Group}.
 *
 * <p>
 * {@link Plugin}s can extend this to define custom properties
 * for {@link Group}s. {@link GroupProperty}s show up in the user
 * configuration screen, and they are persisted with the group object.
 *
 * <p>
 * Configuration screen should be defined in <tt>config.jelly</tt>.
 * Within this page, the {@link GroupProperty} instance is available
 * as <tt>instance</tt> variable (while <tt>it</tt> refers to {@link Group}.
 *
 * @author Witold Delekta
 * @see GroupProperties#LIST
 */
@ExportedBean
public abstract class GroupProperty implements Describable<GroupProperty>, ExtensionPoint {
    /**
     * The group object that owns this property.
     * This value will be set by the Hudson code.
     * Derived classes can expect this value to be always set.
     */
    protected transient Group group;

    /*package*/ final void setGroup(Group group) {
        this.group = group;
    }

    // descriptor must be of the GroupPropertyDescriptor type
    public abstract GroupPropertyDescriptor getDescriptor();
}
