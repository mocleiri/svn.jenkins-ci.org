package hudson.tasks;

import hudson.model.Describable;
import hudson.ExtensionPoint;

/**
 * {@link BuildStep}s that perform the actual build.
 *
 * <p>
 * To register a custom {@link Builder} from a plugin,
 * add it to {@link BuildStep#BUILDERS}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Builder implements BuildStep, Describable<Builder>, ExtensionPoint {
}
