package hudson;

import hudson.model.Hudson;
import hudson.scm.SCM;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.triggers.Trigger;

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
     * Plugins should override this method and register custom
     * {@link Publisher}, {@link Builder}, {@link SCM}, and {@link Trigger}s to the corresponding list.
     *
     *
     * @throws Exception
     *      any exception thrown by the plugin during the initialization will disable plugin.
     *
     * @since 1.42
     */
    public void start() throws Exception {
    }

    /**
     * Called to orderly shut down Hudson.
     *
     * <p>
     * This is a good opportunity to clean up resources that plugin started.
     * This method will not be invoked if the {@link #start()} failed abnormally.
     *
     * @throws Exception
     *      if any exception is thrown, it is simply recorded and shut-down of other
     *      plugins continue. This is primarily just a convenience feature, so that
     *      each plugin author doesn't have to worry about catching an exception and
     *      recording it.
     *
     * @since 1.42
     */
    public void stop() throws Exception {
    }
}
