package hudson.model;

import hudson.tasks.BuildStep;
import hudson.tasks.Recorder;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.scm.SCM;

/**
 * Provides a mechanism for synchronizing build executions in the face of concurrent builds.
 *
 * <p>
 * At certain points of a build, {@link BuildStep}s and other extension points often need
 * to refer to what happened in its earlier build. For example, a {@link SCM} check out
 * can run concurrently, but the changelog computation requires that the check out of the
 * earlier build has completed. Or if Hudson is sending out an e-mail, he needs to know
 * the result of the previous build, so that he can decide an e-mail is necessary or not.
 *
 * <p>
 * Check pointing is a primitive mechanism to provide this sort of synchronization.
 * These methods can be only invoked from {@link Executor} threads.
 *
 *
 *
 * <h2>Example</h2>
 * <p>
 * {@link JUnitResultArchiver} provides a good example of how a {@link Recorder} can
 * depend on its earlier result.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.XXX
 */
public final class CheckPoint {
    private CheckPoint() {} // no instantiation allowed

    /**
     * Records that the execution of the build has reached to a check point, idenified
     * by the given identifier.
     *
     * <p>
     * If the successive builds are {@linkplain #waitForCheckpoint(Object) waiting for this check point},
     * they'll be released.
     *
     * <p>
     * This method can be only called from an {@link Executor} thread.
     *
     * @param id
     *      An object that identifies a check point. The only purpose of this object is so
     *      that different IDs represent different check points, so you can pass in anything.
     *      {@link Class} object of your {@link Recorder} etc. can be used if you only use
     *      one checkpoint per your class, or this could be some unique string, like FQCN+suffix.
     *      <p>
     *      Identifies are compared by their equality.
     */
    public static void reportCheckpoint(Object id) {
        AbstractBuild.reportCheckpoint(id);
    }

    /**
     * Waits until the previous build in progress reaches a check point, identified
     * by the given identifier, or until the current executor becomes the youngest build in progress.
     *
     * <p>
     * Note that "previous build in progress" should be interpreted as "previous (build in progress)" instead of
     * "(previous build) if it's in progress". This makes a difference around builds that are aborted or
     * failed very early without reporting the check points. Imagine the following time sequence:
     *
     * <ol>
     * <li>Build #1, #2, and #3 happens around the same time
     * <li>Build #3 waits for check point {@link JUnitResultArchiver}
     * <li>Build #2 aborts before getting to that check point
     * <li>Build #1 finally checks in {@link JUnitResultArchiver}
     * </ol>
     *
     * <p>
     * Using this method, build #3 correctly waits until the step 4. Because of this behavior,
     * the {@link #reportCheckpoint(Object)}/{@link #waitForCheckpoint(Object)} pair can normally
     * be used without a try/finally block.
     *
     * <p>
     * This method can be only called from an {@link Executor} thread.
     *
     * @param id
     *      This must be equal to the identifier passed to the {@link #reportCheckpoint(Object)} method.
     * @throws InterruptedException
     *      If the build (represented by the calling executor thread) is aborted while it's waiting.  
     */
    public static void waitForCheckpoint(Object id) throws InterruptedException {
        AbstractBuild.waitForCheckpoint(id);
    }
}
