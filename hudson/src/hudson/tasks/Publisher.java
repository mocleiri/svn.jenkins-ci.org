package hudson.tasks;

import hudson.model.Describable;
import hudson.ExtensionPoint;

/**
 * {@link BuildStep}s that run after the build is completed.
 *
 * <p>
 * To register a custom {@link Publisher} from a plugin,
 * add it to {@link BuildStep#PUBLISHERS}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Publisher implements BuildStep, Describable<Publisher>, ExtensionPoint {
}
