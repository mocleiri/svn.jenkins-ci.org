package hudson.model;

/**
 * {@link Descriptor} for {@link UserProperty}.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class UserPropertyDescriptor extends Descriptor<UserProperty> {
    protected UserPropertyDescriptor(Class<? extends UserProperty> clazz) {
        super(clazz);
    }

    /**
     * Creates a default instance of {@link UserProperty} to be associated
     * with {@link User} that doesn't have any back up data store.
     *
     * @return null
     *      if the implementation choose not to add any property object for such user.
     */
    public abstract UserProperty newInstance(User user);

    /**
     * Whether or not the described property is enabled in the current context.
     * Defaults to true.  Over-ride in sub-classes as required.
     */
    public boolean isEnabled() {
        return true;
    }
}
