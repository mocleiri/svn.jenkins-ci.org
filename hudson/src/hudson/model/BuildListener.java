package hudson.model;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Receives events that happen during a build.
 *
 * @author Kohsuke Kawaguchi
 */
public interface BuildListener extends TaskListener {

    /**
     * Called when a build is started.
     */
    void started();

    /**
     * Called when a build is finished.
     */
    void finished(Result result);
}
