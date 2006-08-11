package hudson;

import hudson.model.BuildListener;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kohsuke Kawaguchi
 */
public class Util {
    /**
     * Loads the contents of a file into a string.
     */
    public static String loadFile(File logfile) throws IOException {
        if(!logfile.exists())
            return "";

        StringBuffer str = new StringBuffer((int)logfile.length());

        BufferedReader r = new BufferedReader(new FileReader(logfile));
        char[] buf = new char[1024];
        int len;
        while((len=r.read(buf,0,buf.length))>0)
           str.append(buf,0,len);
        r.close();

        return str.toString();
    }

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
        byte[] buf = new byte[8192];
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

    public static String nullify(String v) {
        if(v!=null && v.length()==0)    v=null;
        return v;
    }

    public static String toHexString(byte[] data, int start, int len) {
        StringBuffer buf = new StringBuffer();
        for( int i=0; i<len; i++ ) {
            int b = data[start+i]&0xFF;
            if(b<16)    buf.append('0');
            buf.append(Integer.toHexString(b));
        }
        return buf.toString();
    }

    public static String toHexString(byte[] bytes) {
        return toHexString(bytes,0,bytes.length);
    }

    public static String getTimeSpanString(long duration) {
        duration /= 1000;
        if(duration<60)
            return combine(duration,"second");
        duration /= 60;
        if(duration<60)
            return combine(duration,"minute");
        duration /= 60;
        if(duration<24)
            return combine(duration,"hour");
        duration /= 24;
        if(duration<30)
            return combine(duration,"day");
        duration /= 30;
        if(duration<12)
            return combine(duration,"month");
        duration /= 12;
        return combine(duration,"year");
    }

    private static String combine(long n, String suffix) {
        String s = Long.toString(n)+' '+suffix;
        if(n!=1)
            s += 's';
        return s;
    }

    /**
     * Escapes non-ASCII characters.
     */
    public static String encode(String s) {
        try {
            boolean escaped = false;

            StringBuffer out = new StringBuffer(s.length());

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            OutputStreamWriter w = new OutputStreamWriter(buf,"UTF-8");

            for (int i = 0; i < s.length(); i++) {
                int c = (int) s.charAt(i);
                if (c<128 && c!=' ') {
                    out.append((char) c);
                } else {
                    // 1 char -> UTF8
                    w.write(c);
                    w.flush();
                    for (byte b : buf.toByteArray()) {
                        out.append('%');
                        out.append(toDigit((b >> 4) & 0xF));
                        out.append(toDigit(b & 0xF));
                    }
                    buf.reset();
                    escaped = true;
                }
            }

            return escaped ? out.toString() : s;
        } catch (IOException e) {
            throw new Error(e); // impossible
        }
    }

    private static char toDigit(int n) {
        char ch = Character.forDigit(n,16);
        if(ch>='a')     ch = (char)(ch-'a'+'A');
        return ch;
    }
}
