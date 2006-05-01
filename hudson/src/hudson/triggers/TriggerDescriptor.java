package hudson.triggers;

import hudson.model.Descriptor;

/**
 * Metadata of {@link Trigger}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class TriggerDescriptor extends Descriptor<Trigger> {
    protected TriggerDescriptor(Class<? extends Trigger> clazz) {
        super(clazz);
    }

    /**
     * Returns the resource path to the help screen HTML, if any.
     */
    public String getHelpFile() {
        return "";
    }
}
