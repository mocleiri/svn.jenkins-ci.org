package hudson;

import hudson.model.Hudson;
import hudson.tasks.BuildStep;

import javax.servlet.ServletContext;

/**
 * Base class of Hudson plugin.
 *
 * <p>
 * A plugin needs to derive from this class.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.42
 */
public abstract class Plugin {

    /**
     * Called when a plugin is loaded to make the {@link ServletContext} object available to a plugin.
     * This object allows plugins to talk to the surrounding environment.
     *
     * <p>
     * The default implementation is no-op.
     *
     * @param context
     *      Always non-null.
     *
     * @since 1.42
     */
    public void setServletContext(ServletContext context) {
    }

    /**
     * Called to allow plugins to initialize themselves.
     *
     * <p>
     * This method is called after {@link #setServletContext(ServletContext)} is invoked.
     * You can also use {@link Hudson#getInstance()} to access the singleton hudson instance.
     *
     * <p>
     * A typical plugin would install {@link BuildStep}s to {@link BuildStep#PUBLISHERS} or
     * {@link BuildStep#BUILDERS}.
     *
     * @throws Exception
     *      any exception thrown by the plugin during the initialization will disable plugin.
     *
     * @since 1.42
     */
    public void init() throws Exception {
    }
}
