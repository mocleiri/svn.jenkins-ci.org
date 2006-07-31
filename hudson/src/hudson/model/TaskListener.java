package hudson.model;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Receives events that happen during some task execution,
 * such as a build or SCM change polling.
 *
 * @author Kohsuke Kawaguchi
 */
public interface TaskListener {
    /**
     * This writer will receive the output of the build.
     *
     * @return
     *      must be non-null.
     */
    PrintStream getLogger();

    /**
     * An error in the build.
     *
     * @return
     *      A writer to receive details of the error. Not null.
     */
    PrintWriter error(String msg);

    /**
     * A fatal error in the build.
     *
     * @return
     *      A writer to receive details of the error. Not null.
     */
    PrintWriter fatalError(String msg);
}
