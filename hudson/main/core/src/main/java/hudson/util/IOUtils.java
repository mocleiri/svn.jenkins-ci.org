package hudson.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Adds more to commons-io.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.337
 */
public class IOUtils extends org.apache.commons.io.IOUtils {
    public static void copy(File src, OutputStream out) throws IOException {
        FileInputStream in = new FileInputStream(src);
        try {
            copy(in,out);
        } finally {
            closeQuietly(in);
        }
    }

    public static void copy(InputStream in, File out) throws IOException {
        FileOutputStream fos = new FileOutputStream(out);
        try {
            copy(in,fos);
        } finally {
            closeQuietly(fos);
        }
    }

    /**
     * Ensures that the given directory exists (if not, it's created, including all the parent directories.)
     *
     * @return
     *      This method returns the 'dir' parameter so that the method call flows better.
     */
    public static File mkdirs(File dir) throws IOException {
        if(dir.mkdirs() || dir.exists())
            return dir;

        // following Ant <mkdir> task to avoid possible race condition.
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }

        if (dir.mkdirs() || dir.exists())
            return dir;

        throw new IOException("Failed to create a directory at "+dir);
    }
}
