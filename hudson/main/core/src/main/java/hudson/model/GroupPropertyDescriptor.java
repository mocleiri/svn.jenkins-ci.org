package hudson.model;

/**
 * {@link Descriptor} for {@link GroupProperty}.
 * 
 * @author Witold Delekta
 */
public abstract class GroupPropertyDescriptor extends Descriptor<GroupProperty> {
    protected GroupPropertyDescriptor(Class<? extends GroupProperty> clazz) {
        super(clazz);
    }

    /**
     * Creates a default instance of {@link GroupProperty} to be associated
     * with {@link Group} object that wasn't created from a persisted XML data.
     *
     * @return null
     *      if the implementation choose not to add any property object for such group.
     */
    public abstract GroupProperty newInstance(Group group);

}
