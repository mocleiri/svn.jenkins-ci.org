package hudson.triggers;

import hudson.model.Build;
import hudson.model.Project;
import hudson.model.Describable;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Shell;
import hudson.tasks.Ant;
import hudson.tasks.Maven;

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

    TriggerDescriptor getDescriptor();

    /**
     * List of all installed {@link TriggerDescriptor}s.
     */
    public static final TriggerDescriptor[] TRIGGERS = new TriggerDescriptor[] {
        TimerTrigger.DESCRIPTOR
    };
}
