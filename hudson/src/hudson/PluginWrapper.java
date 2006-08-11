package hudson;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

/**
 * Represents a Hudson plug-in and associated control information
 * for Hudson to control {@link Plugin}.
 *
 * <p>
 * A plug-in is packaged into a jar file whose extension is ".hudson-plugin".
 *
 * <p>
 * A plugin needs to have a special manifest entry to identify what it is.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PluginWrapper {
    /**
     * Plugin manifest.
     * Contains description of the plugin.
     */
    private final Manifest manifest;

    /**
     * Loaded plugin instance.
     */
    private final Plugin plugin;

    /**
     * @param archive
     *      A .hudson-plugin archive file.
     *
     * @throws IOException
     *      if an installation of this plugin failed. The caller should
     *      proceed to work with other plugins.
     */
    public PluginWrapper(PluginManager owner, File archive) throws IOException {
        LOGGER.info("Loading plugin: "+archive);

        JarFile jarFile = new JarFile(archive);
        manifest = jarFile.getManifest();
        if(manifest==null) {
            throw new IOException("Plugin installation failed. No manifest in "+archive);
        }
        jarFile.close();

        File controlFile = new File(archive.getPath()+".disabled");
        if(controlFile.exists()) {
            LOGGER.info("Plugin is disabled");
            this.plugin = null;
            return;
        }


        ClassLoader classLoader = new URLClassLoader(new URL[]{archive.toURL()}, getClass().getClassLoader());

        String className = manifest.getMainAttributes().getValue("Plugin-Class");
        if(className ==null) {
            throw new IOException("Plugin installation failed. No 'Plugin-Class' entry in the manifest of "+archive);
        }

        try {
            Class clazz = classLoader.loadClass(className);
            Object plugin = clazz.newInstance();
            if(!(plugin instanceof Plugin)) {
                throw new IOException(className+" doesn't extend from hudson.Plugin");
            }
            this.plugin = (Plugin)plugin;
        } catch (ClassNotFoundException e) {
            IOException ioe = new IOException("Unable to load " + className + " from " + archive);
            ioe.initCause(e);
            throw ioe;
        } catch (IllegalAccessException e) {
            IOException ioe = new IOException("Unable to create instance of " + className + " from " + archive);
            ioe.initCause(e);
            throw ioe;
        } catch (InstantiationException e) {
            IOException ioe = new IOException("Unable to create instance of " + className + " from " + archive);
            ioe.initCause(e);
            throw ioe;
        }

        // initialize plugin
        try {
            plugin.setServletContext(owner.context);
            plugin.init();
        } catch(Throwable t) {
            // gracefully handle any error in plugin.
            IOException ioe = new IOException("Failed to initialize");
            ioe.initCause(t);
            throw ioe;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(PluginWrapper.class.getName());
}
