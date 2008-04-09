package hudson.tasks;

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.maven.MavenReporter;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Project;
import hudson.model.AbstractBuild;

/**
 * {@link BuildStep}s that run after the build is completed.
 *
 * <p>
 * To register a custom {@link Publisher} from a plugin,
 * add it to {@link BuildStep#PUBLISHERS}.
 *
 * <p>
 * Starting 1.178, publishers are exposed to all kinds of different
 * project type, not just the freestyle project type (in particular,
 * the native maven2 job type.) This is convenient default for
 * {@link Publisher}s in particular initially, but we encourage advanced
 * plugins to consider writing {@link MavenReporter}, as it offers the
 * potential of reducing the amount of configuration needed to run the plugin.
 *
 * For those plugins that don't want {@link Publisher} to show up in
 * different job type, use {@link BuildStepDescriptor} for the base type
 * of your descriptor to control which job type it supports.
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

    /**
     * Returne true if this {@link Publisher} needs to run after the build result is
     * fully finalized.
     *
     * <p>
     * The execution of normal {@link Publisher}s are considered within a part
     * of the build. This allows publishers to mark the build as a failure, or
     * to include their execution time in the total build time.
     *
     * <p>
     * So normally, that is the preferrable behavior, but in a few cases
     * this is problematic. One of such cases is when a publisher needs to
     * trigger other builds, whcih in turn need to see this build as a
     * completed build. Those plugins that need to do this can return true
     * from this method, so that the {@link #perform(AbstractBuild, Launcher, BuildListener)}
     * method is called after the build is marked as completed.
     *
     * <p>
     * When {@link Publisher} behaves this way, note that they can no longer
     * change the build status anymore.
     *
     * @author Kohsuke Kawaguchi
     * @since 1.153
     */
    public boolean needsToRunAfterFinalized() {
        return false;
    }
}
