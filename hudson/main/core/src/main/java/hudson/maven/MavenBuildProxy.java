package hudson.maven;

import hudson.FilePath;
import hudson.model.Result;
import hudson.remoting.Callable;
import hudson.remoting.DelegatingCallable;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;

/**
 * Remoting proxy interface for {@link MavenReporter}s to talk to {@link MavenBuild}
 * during the build.
 *
 * @author Kohsuke Kawaguchi
 */
public interface MavenBuildProxy {
    /**
     * Executes the given {@link BuildCallable} on the master, where one
     * has access to {@link MavenBuild} and all the other Hudson objects.
     *
     * <p>
     * The parameter, return value, and exception are all transfered by using
     * Java serialization.
     *
     * @return
     *      the value that {@link BuildCallable} returned.
     * @throws T
     *      if {@link BuildCallable} throws this exception.
     * @throws IOException
     *      if the remoting failed.
     * @throws InterruptedException
     *      if the remote execution is aborted.
     * @see #executeAsync(BuildCallable)
     */
    <V,T extends Throwable> V execute( BuildCallable<V,T> program ) throws T, IOException, InterruptedException;

    /**
     * Executes the given {@link BuildCallable} asynchronously on the master.
     * <p>
     * This method works like {@link #execute(BuildCallable)} except that
     * the method returns immediately and doesn't wait for the completion of the program.
     * <p>
     * The completions of asynchronous executions are accounted for before
     * the build completes. If they throw exceptions, they'll be reported
     * and the build will be marked as a failure. 
     */
    void executeAsync( BuildCallable<?,?> program ) throws IOException;

    /**
     * Root directory of the build.
     *
     * @see MavenBuild#getRootDir() 
     */
    FilePath getRootDir();

    /**
     * Root directory of the parent of this build.
     */
    FilePath getProjectRootDir();

    /**
     * @see MavenBuild#getArtifactsDir()
     */
    FilePath getArtifactsDir();

    /**
     * @see MavenBuild#setResult(Result)
     */
    void setResult(Result result);

    /**
     * @see MavenBuild#getTimestamp()
     */
    Calendar getTimestamp();

    /**
     * Nominates that the reporter will contribute a project action
     * for this build by using {@link MavenReporter#getProjectAction(MavenModule)}.
     *
     * <p>
     * The specified {@link MavenReporter} object will be transfered to the master
     * and will become a persisted part of the {@link MavenBuild}. 
     */
    void registerAsProjectAction(MavenReporter reporter);

    /**
     * Called at the end of the build to record what mojos are executed.
     */
    void setExecutedMojos(List<ExecutedMojo> executedMojos);

    public interface BuildCallable<V,T extends Throwable> extends Serializable {
        /**
         * Performs computation and returns the result,
         * or throws some exception.
         *
         * @throws InterruptedException
         *      if the processing is interrupted in the middle. Exception will be
         *      propagated to the caller.
         * @throws IOException
         *      if the program simply wishes to propage the exception, it may throw
         *      {@link IOException}.
         */
        V call(MavenBuild build) throws T, IOException, InterruptedException;
    }

    /**
     * Filter for {@link MavenBuildProxy}.
     *
     * Meant to be useful as the base class for other filters.
     */
    /*package*/ static abstract class Filter<CORE extends MavenBuildProxy> implements MavenBuildProxy, Serializable {
        protected final CORE core;

        protected Filter(CORE core) {
            this.core = core;
        }

        public <V, T extends Throwable> V execute(BuildCallable<V, T> program) throws T, IOException, InterruptedException {
            return core.execute(program);
        }

        public void executeAsync(BuildCallable<?, ?> program) throws IOException {
            core.executeAsync(program);
        }

        public FilePath getRootDir() {
            return core.getRootDir();
        }

        public FilePath getProjectRootDir() {
            return core.getProjectRootDir();
        }

        public FilePath getArtifactsDir() {
            return core.getArtifactsDir();
        }

        public void setResult(Result result) {
            core.setResult(result);
        }

        public Calendar getTimestamp() {
            return core.getTimestamp();
        }

        public void registerAsProjectAction(MavenReporter reporter) {
            core.registerAsProjectAction(reporter);
        }

        public void setExecutedMojos(List<ExecutedMojo> executedMojos) {
            core.setExecutedMojos(executedMojos);
        }

        private static final long serialVersionUID = 1L;

        /**
         * {@link Callable} for invoking {@link BuildCallable} asynchronously.
         */
        protected static final class AsyncInvoker implements DelegatingCallable<Object,Throwable> {
            private final MavenBuildProxy proxy;
            private final BuildCallable<?,?> program;

            public AsyncInvoker(MavenBuildProxy proxy, BuildCallable<?,?> program) {
                this.proxy = proxy;
                this.program = program;
            }

            public ClassLoader getClassLoader() {
                return program.getClass().getClassLoader();
            }

            public Object call() throws Throwable {
                // by the time this method is invoked on the master, proxy points to a real object
                proxy.execute(program);
                return null;    // ignore the result, as there's no point in sending it back
            }

            private static final long serialVersionUID = 1L;
        }
    }
}
