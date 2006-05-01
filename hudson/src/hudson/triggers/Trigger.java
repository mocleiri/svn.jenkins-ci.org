package hudson.triggers;

import hudson.model.Build;
import hudson.model.Project;
import hudson.model.Describable;
import hudson.model.Descriptor;

/**
 * Triggers a {@link Build}.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Trigger extends Describable<Trigger> {
    /**
     * Called when a {@link Trigger} is loaded into memory and started.
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
