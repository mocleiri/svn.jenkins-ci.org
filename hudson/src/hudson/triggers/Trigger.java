package hudson.triggers;

import hudson.model.Build;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Project;

/**
 * Triggers a {@link Build}.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Trigger extends Describable<Trigger> {
    /**
     * Called when a {@link Trigger} is loaded into memory and started.
     *
     * @param project
     *      given so that the persisted form of this object won't have to have a back pointer.
     */
    void start(Project project);

    /**
     * Called before a {@link Trigger} is removed.
     * Under some circumstances, this may be invoked more than once for
     * a given {@link Trigger}, so be prepared for that.
     */
    void stop();

    /**
     * List of all installed {@link Trigger}s.
     */
    public static final Descriptor<Trigger>[] TRIGGERS = Descriptor.toArray(
        TimerTrigger.DESCRIPTOR
    );
}
