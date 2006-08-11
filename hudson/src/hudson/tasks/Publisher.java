package hudson.tasks;

import hudson.model.Describable;

/**
 * {@link BuildStep}s that run after the build is completed.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Publisher extends BuildStep, Describable<Publisher> {
}
