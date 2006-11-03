package hudson.model;

import java.util.AbstractMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.Collections;
import java.util.Map;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * {@link Map} from build number to {@link Run}.
 *
 * <p>
 * This class is multi-thread safe by using copy-on-write technique,
 * and it also updates the bi-directional links within {@link Run}
 * accordingly.
 *
 * @author Kohsuke Kawaguchi
 */
public final class RunMap<R extends Run<?,R>> extends AbstractMap<Integer,R> implements SortedMap<Integer,R> {
    // copy-on-write map
    private transient volatile SortedMap<Integer,R> builds =
        new TreeMap<Integer,R>(COMPARATOR);

    /**
     * Read-only view of this map.
     */
    private final SortedMap<Integer,R> view = Collections.unmodifiableSortedMap(this);

    public Set<Entry<Integer,R>> entrySet() {
        // since the map is copy-on-write, make sure no one modifies it
        return Collections.unmodifiableSet(builds.entrySet());
    }

    public synchronized R put(R value) {
        return put(value.getNumber(),value);
    }

    public synchronized R put(Integer key, R value) {
        // copy-on-write update
        TreeMap<Integer,R> m = new TreeMap<Integer,R>(builds);

        R r = update(m, key, value);

        this.builds = m;
        return r;
    }

    public synchronized void putAll(Map<? extends Integer,? extends R> rhs) {
        // copy-on-write update
        TreeMap<Integer,R> m = new TreeMap<Integer,R>(builds);

        for (Map.Entry<? extends Integer,? extends R> e : rhs.entrySet())
            update(m, e.getKey(), e.getValue());

        this.builds = m;
    }

    private R update(TreeMap<Integer, R> m, Integer key, R value) {
        R first = m.isEmpty() ? null : m.get(m.firstKey());
        R r = m.put(key, value);
        SortedMap<Integer,R> head = m.headMap(key);
        if(!head.isEmpty()) {
            R prev = m.get(head.lastKey());
            value.nextBuild = prev.nextBuild;
            value.previousBuild = prev;
            if(value.nextBuild!=null)
                value.nextBuild.previousBuild = value;
            prev.nextBuild=value;
        } else {
            value.nextBuild = first;
            value.previousBuild = null;
            if(first!=null)
                first.previousBuild = value;
        }
        return r;
    }

    public synchronized boolean remove(R run) {
        if(run.nextBuild!=null)
            run.nextBuild.previousBuild = run.previousBuild;
        if(run.previousBuild!=null)
            run.previousBuild.nextBuild = run.nextBuild; 

        // copy-on-write update
        TreeMap<Integer,R> m = new TreeMap<Integer,R>(builds);
        R r = m.remove(run.getNumber());
        this.builds = m;

        return r!=null;
    }

    public synchronized void reset(TreeMap<Integer,R> builds) {
        this.builds = new TreeMap<Integer,R>(COMPARATOR);
        putAll(builds);
    }

    /**
     * Gets the read-only view of this map.
     */
    public SortedMap<Integer,R> getView() {
        return view;
    }

//
// SortedMap delegation
//
    public Comparator<? super Integer> comparator() {
        return builds.comparator();
    }

    public SortedMap<Integer, R> subMap(Integer fromKey, Integer toKey) {
        return builds.subMap(fromKey, toKey);
    }

    public SortedMap<Integer, R> headMap(Integer toKey) {
        return builds.headMap(toKey);
    }

    public SortedMap<Integer, R> tailMap(Integer fromKey) {
        return builds.tailMap(fromKey);
    }

    public Integer firstKey() {
        return builds.firstKey();
    }

    public Integer lastKey() {
        return builds.lastKey();
    }

    public static final Comparator<Comparable> COMPARATOR = new Comparator<Comparable>() {
        public int compare(Comparable o1, Comparable o2) {
            return -o1.compareTo(o2);
        }
    };

    /**
     * {@link Run} factory.
     */
    public interface Constructor<R extends Run<?,R>> {
        R create(File dir) throws IOException;
    }

    /**
     * Fills in {@link RunMap} by loading build records from the file system.
     *
     * @param job
     *      Job that owns this map.
     * @param cons
     *      Used to create new instance of {@link Run}.
     */
    public synchronized void load(Job job, Constructor<R> cons) {
        TreeMap<Integer,R> builds = new TreeMap<Integer,R>(RunMap.COMPARATOR);
        File buildDir = job.getBuildDir();
        buildDir.mkdirs();
        String[] buildDirs = buildDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir,name).isDirectory();
            }
        });

        for( String build : buildDirs ) {
            File d = new File(buildDir,build);
            if(new File(d,"build.xml").exists()) {
                // if the build result file isn't in the directory, ignore it.
                try {
                    R b = cons.create(d);
                    builds.put( b.getNumber(), b );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        reset(builds);
    }
}
