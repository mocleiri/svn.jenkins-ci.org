package hudson.maven;

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.tasks.BuildStep;
import hudson.tasks.Publisher;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.Serializable;

/**
 * Listens to the build execution of {@link MavenBuild},
 * and normally records some information and exposes thoses
 * in {@link MavenBuild} later.
 *
 * <p>
 * {@link MavenReporter} is first instanciated on the master.
 * Then during the build, it is serialized and sent over into
 * the maven process by serialization. Reporters will then receive
 * event callbacks as mojo execution progresses. Those event callbacks
 * are the ones that take {@link MavenBuildProxy}.
 *
 * <p>
 * Once the maven build completes normally or abnormally, the reporters
 * will be sent back to the master by serialization again, then
 * have its {@link #end(MavenBuild, Launcher, BuildListener)} method invoked.
 * This is a good opportunity to perform the post-build action.
 *
 * <p>
 * This is the {@link MavenBuild} equivalent of {@link BuildStep}. Instances
 * of {@link MavenReporter}s are persisted with {@link MavenModule}/{@link MavenModuleSet},
 * possibly with configuration specific to that job.
 *
 *
 * <h2>Callback Firing Sequence</h2>
 * <p>
 * The callback methods are invoked in the following order:
 *
 * <pre>
 * SEQUENCE := preBuild MODULE* postBuild end
 * MODULE   := enterModule MOJO+ leaveModule
 * MOJO     := preExecute postExecute
 * </pre>
 *
 * <p>
 * When an error happens, the call sequence could be terminated at any point
 * and no further callback methods may be invoked.
 *
 *
 * <h2>Action</h2>
 * <p>
 * {@link MavenReporter} can {@link MavenBuild#addAction(Action) contribute}
 * {@link Action} to {@link MavenBuild} so that the report can be displayed
 * in the web UI.
 *
 * <p>
 * Such action can also implement {@link AggregatableAction} if it further
 * wishes to contribute a separate action to {@link MavenModuleSetBuild}.
 * This mechanism is usually used to provide aggregated report for all the
 * module builds.
 *
 * @author Kohsuke Kawaguchi
 * @see MavenReporters
 */
public abstract class MavenReporter implements Describable<MavenReporter>, ExtensionPoint, Serializable {
    /**
     * Called before the actual maven2 execution begins.
     *
     * @param pom
     *      Represents the POM to be executed.
     * @return
     *      true if the build can continue, false if there was an error
     *      and the build needs to be aborted.
     * @throws InterruptedException
     *      If the build is interrupted by the user (in an attempt to abort the build.)
     *      Normally the {@link MavenReporter} implementations may simply forward the exception
     *      it got from its lower-level functions.
     * @throws IOException
     *      If the implementation wants to abort the processing when an {@link IOException}
     *      happens, it can simply propagate the exception to the caller. This will cause
     *      the build to fail, with the default error message.
     *      Implementations are encouraged to catch {@link IOException} on its own to
     *      provide a better error message, if it can do so, so that users have better
     *      understanding on why it failed.
     */
    public boolean preBuild(MavenBuildProxy build, MavenProject pom, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    /**
     * Called when the build enters a next {@link MavenProject}.
     *
     * <p>
     * When the current build is a multi-module reactor build, every time the build
     * moves on to the next module, this method will be invoked.
     *
     * <p>
     * Note that as of Maven 2.0.4, Maven does not perform any smart optimization
     * on the order of goal executions. Therefore, the same module might be entered more than
     * once during the build.
     *
     * @return
     *      See {@link #preBuild}
     * @throws InterruptedException
     *      See {@link #preBuild}
     * @throws IOException
     *      See {@link #preBuild}
     */
    public boolean enterModule(MavenBuildProxy build, MavenProject pom, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    /**
     * Called when the build leaves the current {@link MavenProject}.
     *
     * @see #enterModule
     */
    public boolean leaveModule(MavenBuildProxy build, MavenProject pom, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    /**
     * Called before execution of a single mojo.
     * 
     * @return
     *      See {@link #preBuild}
     * @throws InterruptedException
     *      See {@link #preBuild}
     * @throws IOException
     *      See {@link #preBuild}
     */
    public boolean preExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    /**
     * Called after execution of a single mojo.
     * <p>
     * See {@link #preExecute} for the contract.
     *
     * @param error
     *      If mojo execution failed with {@link MojoFailureException} or
     *      {@link MojoExecutionException}, this method is still invoked
     *      with those error objects.
     *      If mojo executed successfully, this parameter is null.
     */
    public boolean postExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener, Throwable error) throws InterruptedException, IOException {
        return true;
    }

    /**
     * Called after a build of one maven2 module is completed.
     *
     * <p>
     * Note that at this point the build result is still not determined.
     *
     * @return
     *      See {@link #preBuild}
     * @throws InterruptedException
     *      See {@link #preBuild}
     * @throws IOException
     *      See {@link #preBuild}
     */
    public boolean postBuild(MavenBuildProxy build, MavenProject pom, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    /**
     * Called after the maven execution finished and the result is determined.
     *
     * <p>
     * This method fires after {@link #postBuild(MavenBuildProxy, MavenProject, BuildListener)}.
     * Works like {@link Publisher#perform(Build, Launcher, BuildListener)}.
     */
    public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    /**
     * Equivalent of {@link BuildStep#getProjectAction(AbstractProject)}
     * for {@link MavenReporter}.
     *
     * <p>
     * Registers a transient action to {@link MavenModule} when it's rendered.
     * This is useful if you'd like to display an action at the module level.
     *
     * <p>
     * Since this contributes a transient action, the returned {@link Action}
     * will not be serialized.
     *
     * <p>
     * For this method to be invoked, your {@link MavenReporter} has to invoke
     * {@link MavenBuildProxy#registerAsProjectAction(MavenReporter)} during the build.
     *
     * @return
     *      null not to contribute an action, which is the default.
     */
    public Action getProjectAction(MavenModule module) {
        return null;
    }

    /**
     * Works like {@link #getProjectAction(MavenModule)} but
     * works at {@link MavenModuleSet} level.
     *
     * <p>
     * For this method to be invoked, your {@link MavenReporter} has to invoke
     * {@link MavenBuildProxy#registerAsAggregatedProjectAction(MavenReporter)} during the build.
     *
     * @return
     *      null not to contribute an action, which is the default.
     */
    public Action getAggregatedProjectAction(MavenModuleSet project) {
        return null;
    }

    public abstract MavenReporterDescriptor getDescriptor();
}
