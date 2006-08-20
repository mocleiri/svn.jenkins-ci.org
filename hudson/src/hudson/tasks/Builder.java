package hudson.tasks;

import hudson.model.Describable;

/**
 * {@link BuildStep}s that perform the actual build.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Builder implements BuildStep, Describable<Builder> {
}
