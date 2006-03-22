package hudson;

import hudson.model.BuildListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        File[] files = file.listFiles();
        if(files==null)
            return;     // the directory didn't exist in the first place
        for (File child : files) {
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
     * Creates a new temporary directory.
     */
    public static File createTempDir() throws IOException {
        File tmp = File.createTempFile("hudson", "tmp");
        if(!tmp.delete())
            throw new IOException("Failed to delete "+tmp);
        if(!tmp.mkdirs())
            throw new IOException("Failed to create a new directory "+tmp);
        return tmp;
    }

    private static final Pattern errorCodeParser = Pattern.compile(".*error=([0-9]+).*");

    /**
     * On Windows, error messages for IOException aren't very helpful.
     * This method generates additional user-friendly error message to the listener
     */
    public static void displayIOException( IOException e, BuildListener listener ) {
        if(File.separatorChar!='\\')
            return; // not Windows

        Matcher m = errorCodeParser.matcher(e.getMessage());
        if(!m.matches())
            return; // failed to parse

        try {
            ResourceBundle rb = ResourceBundle.getBundle("/hudson/win32errors");
            listener.getLogger().println(rb.getString("error"+m.group(1)));
        } catch (Exception _) {
            // silently recover from resource related failures
        }
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

    public static void copyStream(InputStream in,OutputStream out) throws IOException {
        byte[] buf = new byte[256];
        int len;
        while((len=in.read(buf))>0)
            out.write(buf,0,len);
    }

    public static String[] tokenize(String s) {
        StringTokenizer st = new StringTokenizer(s);
        String[] a = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++)
            a[i] = st.nextToken();
        return a;
    }

    public static String[] mapToEnv(Map<?,?> m) {
        String[] r = new String[m.size()];
        int idx=0;

        for (final Map.Entry e : m.entrySet()) {
            r[idx++] = e.getKey().toString() + '=' + e.getValue().toString();
        }
        return r;
    }

    public static int min(int x, int... values) {
        for (int i : values) {
            if(i<x)
                x=i;
        }
        return x;
    }
}
