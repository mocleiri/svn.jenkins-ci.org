package hudson.model;

import hudson.Launcher;

/**
 * Commonality between {@link Slave} and master {@link Hudson}.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Node {
    /**
     * Name of this node.
     *
     * @return
     *      null if this is master
     */
    String getNodeName();

    /**
     * Human-readable description of this node.
     */
    String getDescription();

    /**
     * Returns a {@link Launcher} for executing programs on this node.
     */
    Launcher createLauncher(BuildListener listener);

    /**
     * Returns the number of {@link Executor}s.
     *
     * This may be different from <code>getExecutors().size()</code>
     * because it takes time to adjust the number of executors.
     */
    int getNumExecutors();
}
