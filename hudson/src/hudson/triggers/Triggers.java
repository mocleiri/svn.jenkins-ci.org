package hudson.triggers;

import hudson.model.Descriptor;

/**
 * @author Kohsuke Kawaguchi
 */
public class Triggers {
    /**
     * List of all installed {@link Trigger}s.
     */
    public static final Descriptor<Trigger>[] TRIGGERS = Descriptor.toArray(
        SCMTrigger.DESCRIPTOR,
        TimerTrigger.DESCRIPTOR
    );
}
