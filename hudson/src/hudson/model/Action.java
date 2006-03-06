package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Serializable;

/**
 * Contributes an item to the task list.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Action extends Serializable, ModelObject {
    /**
     * Gets the file name of the icon (relative to /images/24x24)
     */
    String getIconFileName();

    /**
     * Gets the string to be displayed.
     */
    String getDisplayName();

    /**
     * Gets the URL path name.
     */
    String getUrlName();
}
