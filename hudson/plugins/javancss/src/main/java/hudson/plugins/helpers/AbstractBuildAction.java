package hudson.plugins.helpers;

import hudson.model.AbstractBuild;
import hudson.model.HealthReportingAction;

/**
 * TODO javadoc.
 *
 * @author Stephen Connolly
 * @since 04-Feb-2008 19:41:25
 */
public abstract class AbstractBuildAction<BUILD extends AbstractBuild<?, ?>> implements HealthReportingAction {
    /**
     * The owner of this Action.  Ideally I'd like this to be final and set in the constructor, but Maven does not
     * let us do that, so we need a setter.
     */
    private BUILD build = null;

    /**
     * Constructs a new AbstractBuildAction.
     */
    protected AbstractBuildAction() {
    }

    /**
     * Getter for property 'build'.
     *
     * @return Value for property 'build'.
     */
    public synchronized BUILD getBuild() {
        return build;
    }

    /**
     * Write once setter for property 'build'.
     *
     * @param build Value to set for property 'build'.
     */
    public synchronized void setBuild(BUILD build) {
        // Ideally I'd prefer to use and AtomicReference... but I'm unsure how it would work with the serialization fun
        if (this.build == null && this.build != build) {
            this.build = build;
        }
    }
}
