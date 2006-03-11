package hudson.tasks.junit;

import hudson.model.ModelObject;
import hudson.model.Build;

/**
 * Base class for all test result objects.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class TestObject implements ModelObject {
    public abstract Build getOwner();

    /**
     * Gets the counter part of this {@link TestObject} in the previous run.
     *
     * @return null
     *      if no such counter part exists.
     */
    public abstract TestObject getPreviousResult();
}
