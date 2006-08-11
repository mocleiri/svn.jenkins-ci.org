package hudson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
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
     * Null if disabled.
     */
    public final Plugin plugin;

    /**
     * {@link ClassLoader} for loading classes from this plugin.
     * Null if disabled.
     */
    public final ClassLoader classLoader;

    /**
     * Used to control enable/disable setting of the plugin.
     * If this file exists, plugin will be disabled.
     */
    private final File disableFile;

    private final File archive;

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

        this.archive = archive;

        JarFile jarFile = new JarFile(archive);
        manifest = jarFile.getManifest();
        if(manifest==null) {
            throw new IOException("Plugin installation failed. No manifest in "+archive);
        }
        jarFile.close();

        disableFile = new File(archive.getPath()+".disabled");
        if(disableFile.exists()) {
            LOGGER.info("Plugin is disabled");
            this.plugin = null;
            classLoader = null;
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
            plugin.start();
        } catch(Throwable t) {
            // gracefully handle any error in plugin.
            IOException ioe = new IOException("Failed to initialize");
            ioe.initCause(t);
            throw ioe;
        }

        // create public classloader
        // TODO: define a mechanism to hide classes
        // String export = manifest.getMainAttributes().getValue("Export");
        this.classLoader = classLoader;
    }

    /**
     * Terminates the plugin.
     */
    void stop() {
        LOGGER.info("Stopping "+archive);
        try {
            plugin.stop();
        } catch(Throwable t) {
            System.err.println("Failed to shut down "+archive);
            System.err.println(t);
        }
    }

    /**
     * Enables this plugin next time Hudson runs.
     */
    public void enable() {
        disableFile.delete();
    }

    /**
     * Disables this plugin next time Hudson runs.
     */
    public void disable() throws IOException {
        // creates an empty file
        OutputStream os = new FileOutputStream(disableFile);
        os.close();
    }

    /**
     * Returns true if this plugin is activated for this session.
     */
    public boolean isActive() {
        return plugin!=null;
    }

    private static final Logger LOGGER = Logger.getLogger(PluginWrapper.class.getName());

}
