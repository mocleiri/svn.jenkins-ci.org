package hudson.model;

import hudson.XmlFile;
import org.kohsuke.stapler.Stapler;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class Descriptor {
    private Map<String,Object> properties;

    /**
     * The class being described by this descriptor.
     */
    protected final Class clazz;

    protected Descriptor(Class clazz) {
        this.clazz = clazz;
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
