package hudson.tasks;

import hudson.model.Describable;

/**
 * {@link BuildStep}s that perform the actual build.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Builder extends BuildStep, Describable<Builder> {
}
