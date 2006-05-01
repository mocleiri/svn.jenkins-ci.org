package hudson.scm;

import hudson.model.Descriptor;

/**
 * Describes the {@link SCM} implementation.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class SCMDescriptor extends Descriptor<SCM> {
    protected SCMDescriptor(Class<? extends SCM> clazz) {
        super(clazz);
    }
}
