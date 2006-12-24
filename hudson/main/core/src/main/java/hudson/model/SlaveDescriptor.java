package hudson.model;

import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link Descriptor} for slaves.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class SlaveDescriptor extends Descriptor<Slave> {
    protected SlaveDescriptor(Class<? extends Slave> clazz) {
        super(clazz);
    }

    /**
     * @deprecated
     *      This is not a valid operation for {@link Slave}s.
     */
    @Deprecated
    public final Slave newInstance(StaplerRequest req) throws FormException {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new {@link Job}.
     */
    public abstract Slave newInstance(StaplerRequest req,int index);
}
