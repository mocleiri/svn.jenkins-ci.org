package hudson;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Util {
    /**
     * Deletes the contents of the given directory (but not the directory itself)
     * recursively.
     *
     * @throws IOException
     *      if the operation fails.
     */
    public static void deleteContentsRecursive(File file) throws IOException {
        for (File child : file.listFiles()) {
            if (child.isDirectory())
                deleteContentsRecursive(child);
            if (!child.delete())
                throw new IOException("Unable to delete " + child.getPath());
        }
    }

    public static void deleteRecursive(File dir) throws IOException {
        deleteContentsRecursive(dir);
        if(!dir.delete())
            throw new IOException("Unable to delete "+dir);
    }

    /**
     * Guesses the current host name.
     */
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }
}
