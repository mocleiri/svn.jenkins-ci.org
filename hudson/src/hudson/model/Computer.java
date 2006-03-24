package hudson.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of {@link Executor}s on the same computer.
 *
 * <p>
 * {@link Executor}s on one {@link Computer} is transparently interchangeable
 * (that is the definition of {@link Computer}.)
 *
 * <p>
 * This object is related to {@link Node} but they have some significant difference.
 * {@link Computer} primarily works as a holder of {@link Executor}s, so
 * if a {@link Node} is configured (probably temporarily) with 0 executors,
 * you won't have a {@link Computer} object for it.
 *
 * Also, even if you remove a {@link Node}, it takes time for the corresponding
 * {@link Computer} to be removed, if some builds are already in progress on that
 * node.
 *
 * @author Kohsuke Kawaguchi
 */
public class Computer {
    private final List<Executor> executors = new ArrayList<Executor>();

    private int numExecutors;

    private Node node;

    public Computer(Node node) {
        assert node.getNumExecutors()!=0 : "Computer created with 0 executors";
        setNode(node);
    }

    /**
     * Number of {@link Executor}s that are configured for this computer.
     *
     * <p>
     * When this value is decreased, it is temporarily possible
     * for {@link #executors} to have a larger number than this.
     */
    // ugly name to let EL access this
    public int getNumExecutors() {
        return numExecutors;
    }

    /**
     * Returns the {@link Node} that this computer represents.
     */
    public Node getNode() {
        return node;
    }

    /*package*/ void setNode(Node node) {
        assert node!=null;
        this.node = node;

        setNumExecutors(node.getNumExecutors());
    }

    /*package*/ void kill() {
        setNumExecutors(0);
    }

    private synchronized void setNumExecutors(int n) {
        this.numExecutors = n;

        // send signal to all idle executors to potentially kill them off
        for( Executor e : executors )
            if(e.getCurrentBuild()==null)
                e.interrupt();

        // if the number is increased, add new ones
        while(executors.size()<numExecutors)
            executors.add(new Executor(this));
    }

    /**
     * Returns the number of idle {@link Executor}s that can start working immediately.
     */
    public synchronized int countIdle() {
        int n = 0;
        for (Executor e : executors) {
            if(e.isIdle())
                n++;
        }
        return n;
    }

    /**
     * Gets the read-only view of all {@link Executor}s.
     */
    public synchronized List<Executor> getExecutors() {
        return new ArrayList<Executor>(executors);
    }

    /**
     * Called by {@link Executor} to kill excessive executors from this computer.
     */
    /*package*/ synchronized void removeExecutor(Executor e) {
        executors.remove(e);
        if(executors.isEmpty())
            Hudson.getInstance().removeComputer(this);
    }

    /**
     * Interrupt all {@link Executor}s.
     */
    public synchronized void interrupt() {
        for (Executor e : executors) {
            e.interrupt();
        }
    }
}
