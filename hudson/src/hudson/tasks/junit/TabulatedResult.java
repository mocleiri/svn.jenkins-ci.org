package hudson.tasks.junit;

import hudson.model.Build;
import hudson.model.ModelObject;

import java.util.Collection;

/**
 * Cumulated result of multiple tests.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class TabulatedResult implements ModelObject {
    public abstract Build getOwner();

    public abstract String getTitle();
    public abstract int getPassCount();
    public abstract int getFailCount();

    public final int getTotalCount() {
        return getPassCount()+getFailCount();
    }

    public abstract Collection<?> getChildren();

}
