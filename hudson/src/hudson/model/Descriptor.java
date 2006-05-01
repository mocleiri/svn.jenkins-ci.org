package hudson.model;

import hudson.XmlFile;
import hudson.tasks.BuildStep;
import org.kohsuke.stapler.Stapler;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata about a configurable instance.
 *
 * <p>
 * When using stapler, it's convenient to have one object for each kind of
 * configurable object, so that instance-agnostic information (such as
 * the config HTML page) can be served from it.
 *
 * {@link Descriptor} represents such metadata, and one instance exists
 * for each kind of configurable object.
 *
 * <p>
 * {@link Descriptor} and {@link Describable} works like {@link Class} and
 * {@link Object}.
 *
 * @author Kohsuke Kawaguchi
 * @see Describable
 */
public abstract class Descriptor<T extends Describable<T>> {
    private Map<String,Object> properties;

    /**
     * The class being described by this descriptor.
     */
    protected final Class<? extends T> clazz;

    protected Descriptor(Class<? extends T> clazz) {
        this.clazz = clazz;
    }

    /**
     * Human readable name of this kind of configurable object.
     */
    public abstract String getDisplayName();

    /**
     * Creates a configured instance from the submitted form.
     */
    public abstract T newInstance(HttpServletRequest req);


    /**
     * Checks if the given object is created from this {@link Descriptor}.
     */
    public final boolean isInstance( T instance ) {
        return clazz.isInstance(instance);
    }

    /**
     * Returns the data store that can be used to store configuration info.
     */
    protected synchronized Map<String,Object> getProperties() {
        if(properties==null)
            properties = load();
        return properties;
    }

    /**
     * Invoked when the global configuration page is submitted.
     *
     * Can be overrided to store descriptor-specific information.
     *
     * @return false
     *      to keep the client in the same config page.
     */
    public boolean configure( HttpServletRequest req ) {
        return true;
    }

    public final String getConfigPage() {
        return Stapler.getViewURL(clazz,"config.jsp");
    }

    public final String getGlobalConfigPage() {
        return Stapler.getViewURL(clazz,"global.jsp");
    }


    /**
     * Saves the configuration info to the disk.
     */
    protected synchronized void save() {
        if(properties!=null)
            try {
                getConfigFile().write(properties);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private Map<String,Object> load() {
        // load
        XmlFile file = getConfigFile();
        if(!file.exists())
            return new HashMap<String,Object>();

        try {
            return (Map<String,Object>)file.read();
        } catch (IOException e) {
            return new HashMap<String,Object>();
        }
    }

    private XmlFile getConfigFile() {
        return new XmlFile(new File(Hudson.getInstance().getRootDir(),clazz.getName()+".xml"));
    }
}
