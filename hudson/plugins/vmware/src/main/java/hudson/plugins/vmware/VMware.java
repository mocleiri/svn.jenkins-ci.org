package hudson.plugins.vmware;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import hudson.plugins.vmware.vix.Vix;
import hudson.model.Hudson;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 *
 * @author connollys
 * @since 28-Sep-2007 09:59:20
 */
public class VMware {
// ------------------------------ FIELDS ------------------------------

    private static final Logger LOGGER = Logger.getLogger(VMware.class.getName());

    /**
     * There are problems if we load the same library more than once.
     */
    private static final Map<String, VMware> libraries = new HashMap<String, VMware>();

    private final Vix instance;

// -------------------------- STATIC METHODS --------------------------

    public static synchronized VMware getSingleton(String pathToLib) {
        File path = new File(pathToLib);
        try {
            pathToLib = path.getCanonicalPath();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Could not get canonical path, reverting to absolute", e);
            pathToLib = path.getAbsolutePath();
        }
        VMware ref = libraries.get(pathToLib);
        if (ref == null) {
            LOGGER.log(Level.INFO, "Attempting to load VMware libraries at path {0}", pathToLib);
            try {
                libraries.put(pathToLib, ref = new VMware(pathToLib));
                LOGGER.log(Level.INFO, "VMware libraries at path {0} loaded", pathToLib);
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "VMware libraries at path " + pathToLib + " failed to load", t);
            }
        }
        return ref;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private VMware(String pathToLib) {
        instance = PluginImpl.getVixInstance(pathToLib);
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    Vix getInstance() {
        return instance;
    }

// -------------------------- OTHER METHODS --------------------------

}
