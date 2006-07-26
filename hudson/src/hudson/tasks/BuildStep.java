package hudson.tasks;

import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Action;
import hudson.model.Project;
import hudson.tasks.junit.JUnitResultArchiver;

/**
 * One step of the whole build process.
 *
 * @author Kohsuke Kawaguchi
 */
public interface BuildStep extends Describable<BuildStep> {

    /**
     * Runs before the build begins.
     *
     * @return
     *      true if the build can continue, false if there was an error
     *      and the build needs to be aborted.
     */
    boolean prebuild( Build build, BuildListener listener );

    /**
     * Runs the step over the given build and reports the progress to the listener.
     *
     * @return
     *      true if the build can continue, false if there was an error
     *      and the build needs to be aborted.
     */
    boolean perform(Build build, Launcher launcher, BuildListener listener);

    /**
     * Returns an action object if this {@link BuildStep} has an action
     * to contribute to a {@link Project}.
     */
    Action getProjectAction();

    /**
     * List of all installed {@link BuildStep}s.
     */
    public static final Descriptor<BuildStep>[] BUILDERS = Descriptor.toArray(
        Shell.DESCRIPTOR,
        Ant.DESCRIPTOR,
        Maven.DESCRIPTOR
    );

    public static final Descriptor<BuildStep>[] PUBLISHERS = Descriptor.toArray(
        ArtifactArchiver.DESCRIPTOR,
        Fingerprinter.DESCRIPTOR,
        JavadocArchiver.DESCRIPTOR,
        JUnitResultArchiver.DESCRIPTOR,
        Mailer.DESCRIPTOR,
        BuildTrigger.DESCRIPTOR
    );
}
