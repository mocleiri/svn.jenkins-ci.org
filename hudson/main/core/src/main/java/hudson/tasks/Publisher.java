package hudson.tasks;

import hudson.ExtensionPoint;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Project;

/**
 * {@link BuildStep}s that run after the build is completed.
 *
 * <p>
 * To register a custom {@link Publisher} from a plugin,
 * add it to {@link BuildStep#PUBLISHERS}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Publisher extends BuildStepCompatibilityLayer implements BuildStep, Describable<Publisher>, ExtensionPoint {
//
// these two methods need to remain to keep binary compatibility with plugins built with Hudson < 1.150
//
    /**
     * Default implementation that does nothing.
     */
    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    /**
     * Default implementation that does nothing.
     */
    public Action getProjectAction(Project project) {
        return null;
    }
}
