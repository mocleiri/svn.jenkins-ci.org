package hudson.plugins.javatest_report;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@link TestObject} that is a collection of other {@link TestObject}s.
 *
 * @param <C>
 *      Type of the child objects in this collection.
 * @param <S>
 *      The derived type of {@link TestCollection} (the same design pattern as you seen in {@link Enum})
 *
 * @author Kohsuke Kawaguchi
 * @author Rama Pulavarthi
 */
public abstract class TestCollection<
    S extends TestCollection<S,C>,
    C extends TestObject<C>> extends TestObject<S> {

    /**
     * All {@link Test}s keyed by their ID.
     */
    private final Map<String,C> tests = new TreeMap<String,C>();

    private int totalCount;
    private int failCount;

    public Collection<C> getChildren() {
        return tests.values();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getFailCount() {
        return failCount;
    }

    /**
     * Returns the caption of the children. Used in the view.
     */
    public abstract String getChildTitle();

    /**
     * Gets a {@link Test} by its id.
     */
    public C get(String id) {
        return tests.get(id);
    }

    /**
     * Adds a new child {@link TestObject} to this.
     * <p>
     * For Digester.
     */
    public void add(C t) {
        tests.put(t.getId(),t);
        totalCount += t.getTotalCount();
        failCount += t.getFailCount();
        t.parent = this;
    }

    // method for stapler
    public C getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
        return get(name);
    }
}
