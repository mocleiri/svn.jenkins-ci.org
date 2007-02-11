package hudson.model;

import hudson.tasks.test.TestResultProjectAction;

import java.io.Serializable;

/**
 * Object that contributes an item to the left hand side menu
 * of a {@link ModelObject}
 * (for example to {@link Project}, {@link Build}, and etc.)
 *
 * <p>
 * If an action has a view named <tt>floatBox.jelly</tt>,
 * it will be displayed as a floating box on the top page of
 * the target {@link ModelObject}. (For example, this is how
 * the JUnit test result trend shows up in the project top page.
 * See {@link TestResultProjectAction}.
 *
 * <p>
 * Actions are often serialized as a part of {@link Actionable}
 * (for example with {@link Build}.) In some other cases,
 * {@link Action}s are transient and not persisted (such as
 * when it's used with {@link Job}).
 *
 * @author Kohsuke Kawaguchi
 */
public interface Action extends Serializable, ModelObject {
    /**
     * Gets the file name of the icon (relative to /images/24x24).
     *
     * @return
     *      null to hide it from the task list. This is normally not very useful,
     *      but this can be used for actions that only contribute <tt>floatBox.jelly</tt>
     *      and no task list item.
     */
    String getIconFileName();

    /**
     * Gets the string to be displayed.
     *
     * The convention is to capitalize the first letter of each word,
     * such as "Test Result". 
     */
    String getDisplayName();

    /**
     * Gets the URL path name.
     */
    String getUrlName();
}
