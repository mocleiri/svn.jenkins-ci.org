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
    public abstract class Environment extends hudson.tasks.Environment {
    	/**
    	 * For backward compatibility.
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
