package hudson.slaves;

import hudson.FilePath;
import hudson.model.Computer;

import java.util.HashSet;
import java.util.Set;

/**
 * Used by {@link Computer} to keep track of workspaces that are actively in use.
 *
 * <p>
 * SUBJECT TO CHANGE! Do not use this from plugins directly.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.XXX
 * @see Computer#getWorkspaceList()
 */
public final class WorkspaceList {
    private final Set<FilePath> inUse = new HashSet<FilePath>();

    public WorkspaceList() {
    }

    /**
     * Allocates some workspace by adding some variation to the given base if necessary.
     */
    public synchronized FilePath allocate(FilePath base) {
        for (int i=1; ; i++) {
            FilePath candidate = i==1 ? base : base.withSuffix("@"+i);
            if(inUse.contains(candidate))
                continue;
            inUse.add(candidate);
            return candidate;
        }
    }

    /**
     * Releases an allocated or acquired workspace.
     */
    public synchronized void release(FilePath p) {
        if (!inUse.remove(p))
            throw new AssertionError("Releasing unallocated workspace "+p);
        notifyAll();
    }

    /**
     * Acquires the given workspace. If necessary, this method blocks until it's made available.
     *
     * @return
     *      The same {@link FilePath} as given to this method.
     */
    public synchronized FilePath acquire(FilePath p) throws InterruptedException {
        while (inUse.contains(p))
            wait();
        inUse.add(p);
        return p;
    }
}
