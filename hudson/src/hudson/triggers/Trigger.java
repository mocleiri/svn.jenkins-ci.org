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
     */
    void stop();

    /**
     * List of all installed {@link Trigger}s.
     */
    public static final Descriptor<Trigger>[] TRIGGERS = Descriptor.toArray(
        TimerTrigger.DESCRIPTOR
    );
}
