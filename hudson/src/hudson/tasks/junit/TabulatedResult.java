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

    /**
     * Gets the counter part of this {@link TabulatedResult} in the previous run.
     *
     * @return null
     *      if nosuch counter part exists.
     */
    public abstract TabulatedResult getPreviousResult();

    /**
     * Gets the human readable title of this result object.
     */
    public abstract String getTitle();

    /**
     * Gets the total number of passed tests.
     */
    public abstract int getPassCount();

    /**
     * Gets the total number of failed tests.
     */
    public abstract int getFailCount();

    /**
     * Gets the total number of tests.
     */
    public final int getTotalCount() {
        return getPassCount()+getFailCount();
    }

    /**
     * Gets the child test result objects.
     */
    public abstract Collection<?> getChildren();

}
