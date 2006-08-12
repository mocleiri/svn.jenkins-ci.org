package hudson;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

/**
 * Represents a Hudson plug-in and associated control information
 * for Hudson to control {@link Plugin}.
 *
 * <p>
 * A plug-in is packaged into a jar file whose extension is <tt>".hpi"</tt>,
 * A plugin needs to have a special manifest entry to identify what it is.
 *
 * <p>
 * At the runtime, a plugin has two distinct state axis.
 * <ol>
 *  <li>Enabled/Disabled. If enabled, Hudson is going to use it
 *      next time Hudson runs. Otherwise the next run will ignore it.
 *  <li>Activated/Deactivated. If activated, that means Hudson is using
 *      the plugin in this session. Otherwise it's not.
 * </ol>
 * <p>
 * For example, an activated but disabled plugin is still running but the next
 * time it won't.
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

    /**
     * Short name of the plugin. The "abc" portion of "abc.hpl".
     */
    private final String shortName;

    /**
     * @param archive
     *      A .hpi archive file jar file, or a .hpl linked plugin.
     *
     * @throws IOException
     *      if an installation of this plugin failed. The caller should
     *      proceed to work with other plugins.
     */
    public PluginWrapper(PluginManager owner, File archive) throws IOException {
        LOGGER.info("Loading plugin: "+archive);

        this.shortName = getShortName(archive);

        boolean isLinked = archive.getName().endsWith(".hpl");

        if(isLinked) {
            FileInputStream in = new FileInputStream(archive);
            try {
                manifest = new Manifest(in);
            } finally {
                in.close();
            }
        } else {
            JarFile jarFile = new JarFile(archive);
            manifest = jarFile.getManifest();
            if(manifest==null) {
                throw new IOException("Plugin installation failed. No manifest in "+archive);
            }
            jarFile.close();
        }

        // TODO: define a mechanism to hide classes
        // String export = manifest.getMainAttributes().getValue("Export");

        if(isLinked) {
            String classPath = manifest.getMainAttributes().getValue("Class-Path");
            List<URL> paths = new ArrayList<URL>();
            for (String s : classPath.split(" +")) {
                File file = new File(archive.getParentFile(), s);
                if(!file.exists())
                    throw new IOException("No such file: "+file);
                paths.add(file.toURL());
            }
            this.classLoader = new URLClassLoader(paths.toArray(new URL[0]), getClass().getClassLoader());
        } else {
            this.classLoader = new URLClassLoader(new URL[]{archive.toURL()}, getClass().getClassLoader());
        }


        disableFile = new File(archive.getPath()+".disabled");
        if(disableFile.exists()) {
            LOGGER.info("Plugin is disabled");
            this.plugin = null;
            return;
        }

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
    }

    /**
     * Returns the URL of the index page jelly script.
     */
    public URL getIndexPage() {
        return classLoader.getResource("index.jelly");
    }

    /**
     * Returns the short name suitable for URL.
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Returns a one-line descriptive name of this plugin.
     */
    public String getLongName() {
        String name = manifest.getMainAttributes().getValue("Long-Name");
        if(name!=null)      return name;
        return shortName;
    }

    /**
     * Gets the "abc" portion from "abc.ext".
     */
    private static String getShortName(File archive) {
        String n = archive.getName();
        int idx = n.lastIndexOf('.');
        if(idx>=0)
            n = n.substring(0,idx);
        return n;
    }

    /**
     * Terminates the plugin.
     */
    void stop() {
        LOGGER.info("Stopping "+shortName);
        try {
            plugin.stop();
        } catch(Throwable t) {
            System.err.println("Failed to shut down "+shortName);
            System.err.println(t);
        }
    }

    /**
     * Enables this plugin next time Hudson runs.
     */
    public void enable() throws IOException {
        if(!disableFile.delete())
            throw new IOException("Failed to delete "+disableFile);
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
     * Returns true if this plugin is enabled for this session.
     */
    public boolean isActive() {
        return plugin!=null;
    }

    /**
     * If true, the plugin is going to be activated next time
     * Hudson runs.
     */
    public boolean isEnabled() {
        return !disableFile.exists();
    }


//
//
// Action methods
//
//
    public void doMakeEnabled(StaplerRequest req, StaplerResponse rsp) throws IOException {
        enable();
        rsp.setStatus(200);
    }
    public void doMakeDisabled(StaplerRequest req, StaplerResponse rsp) throws IOException {
        disable();
        rsp.setStatus(200);
    }

    private static final Logger LOGGER = Logger.getLogger(PluginWrapper.class.getName());

}
