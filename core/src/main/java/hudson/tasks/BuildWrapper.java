package hudson.tasks;

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Project;
import hudson.model.Action;
import hudson.model.AbstractProject;
import hudson.model.Run.RunnerAbortedException;

import java.io.IOException;
import java.util.Map;

/**
 * Pluggability point for performing pre/post actions for the build process.
 *
 * <p>
 * <b>STILL EXPERIMENTAL. SUBJECT TO CHANGE</b>
 *
 * <p>
 * This extension point enables a plugin to set up / tear down additional
 * services needed to perform a build, such as setting up local X display,
 * or launching and stopping application servers for testing.
 *
 * <p>
 * An instance of {@link BuildWrapper} is associated with a {@link Project}
 * with configuration information as its state. An instance is persisted
 * along with {@link Project}.
 *
 * <p>
 * The {@link #setUp(Build,Launcher,BuildListener)} method is invoked for each build.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class BuildWrapper implements ExtensionPoint, Describable<BuildWrapper> {
    /**
     * Represents the environment set up by {@link BuildWrapper#setUp(Build,Launcher,BuildListener)}.
     *
     * <p>
     * It is expected that the subclasses of {@link BuildWrapper} extends this
     * class and implements its own semantics.
     */
    public abstract class Environment {
        /**
         * Adds environmental variables for the builds to the given map.
         *
         * <p>
         * If the {@link Environment} object wants to pass in information
         * to the build that runs, it can do so by exporting additional
         * environment variables to the map.
         *
         * <p>
         * When this method is invoked, the map already contains the
         * current "planned export" list.
         *
         * @param env
         *      never null. 
         */
        public void buildEnvVars(Map<String,String> env) {
            // no-op by default
        }

        /**
         * Runs after the {@link Builder} completes, and performs a tear down.
         *
         * <p>
         * This method is invoked even when the build failed, so that the
         * clean up operation can be performed regardless of the build result
         * (for example, you'll want to stop application server even if a build
         * fails.)
         *
         * @param build
         *      The same {@link Build} object given to the set up method.
         * @param listener
         *      The same {@link BuildListener} object given to the set up method.
         * @return
         *      true if the build can continue, false if there was an error
         *      and the build needs to be aborted.
         * @throws IOException
         *      terminates the build abnormally. Hudson will handle the exception
         *      and reports a nice error message.
         * @since 1.150
         */
        public boolean tearDown( AbstractBuild build, BuildListener listener ) throws IOException, InterruptedException {
            if (build instanceof Build)
                return tearDown((Build)build, listener);
            else
                return true;
        }

        /**
         * @deprecated
         *      Use {@link #tearDown(AbstractBuild, BuildListener)} instead.
         */
        public boolean tearDown( Build build, BuildListener listener ) throws IOException, InterruptedException {
            return true;
        }
    }

    /**
     * Runs before the {@link Builder} runs, and performs a set up.
     *
     * @param build
     *      The build in progress for which an {@link Environment} object is created.
     *      Never null.
     * @param launcher
     *      This launcher can be used to launch processes for this build.
     *      If the build runs remotely, launcher will also run a job on that remote machine.
     *      Never null.
     * @param listener
     *      Can be used to send any message.
     * @return
     *      non-null if the build can continue, null if there was an error
     *      and the build needs to be aborted.
     * @throws IOException
     *      terminates the build abnormally. Hudson will handle the exception
     *      and reports a nice error message.
     * @since 1.150
     */
    public Environment setUp( AbstractBuild build, Launcher launcher, BuildListener listener ) throws IOException, InterruptedException {
        if (build instanceof Build)
            return setUp((Build)build,launcher,listener);
        else
            throw new AssertionError("The plugin '" + this.getClass().getName() + "' still uses " +
                    "deprecated setUp(Build,Launcher,BuildListener) method. " +
                    "Update the plugin to use setUp(AbstractBuild, Launcher, BuildListener) instead.");
    }

    /**
     * @deprecated
     *      Use {@link #setUp(AbstractBuild, Launcher, BuildListener)} instead.
     */
    public Environment setUp( Build build, Launcher launcher, BuildListener listener ) throws IOException, InterruptedException {
        throw new UnsupportedOperationException(getClass()+" needs to implement the setUp method");
    }

    /**
     * Provides an opportunity for a {@link BuildWrapper} to decorate a {@link Launcher} to be used in the build.
     *
     * <p>
     * This hook is called very early on in the build (even before {@link #setUp(AbstractBuild, Launcher, BuildListener)} is invoked.)
     * The typical use of {@link Launcher} decoration involves in modifying the environment that processes run,
     * such as the use of sudo/pfexec/chroot, or manipulating environment variables.
     *
     * <p>
     * The default implementation is no-op, which just returns the {@code listener} parameter as-is.
     *
     * @param build
     *      The build in progress for which this {@link BuildWrapper} is called. Never null.
     * @param launcher
     *      The default launcher. Never null. This method is expected to wrap this launcher.
     *      This makes sure that when multiple {@link BuildWrapper}s attempt to decorate the same launcher
     *      it will sort of work. But if you are developing a plugin where such collision is not a concern,
     *      you can also simply discard this {@link Launcher} and create an entirely different {@link Launcher}
     *      and return it, too.
     * @param listener
     *      Connected to the build output. Never null. Can be used for error reporting.
     * @return
     *      Must not be null. If a fatal error happens, throw an exception.
     * @throws RunnerAbortedException
     *      If a fatal error is detected but the implementation handled it gracefully, throw this exception
     *      to suppress stack trace.
     * @since 1.280
     */
    public Launcher decorateLauncher(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, RunnerAbortedException {
        return launcher;
    }

    /**
     * {@link Action} to be displayed in the job page.
     *
     * @param job
     *      This object owns the {@link BuildWrapper}. The returned action will be added to this object.
     * @return
     *      null if there's no such action.
     * @since 1.226
     */
    public Action getProjectAction(AbstractProject job) {
        return null;
    }
}
