package hudson;

import hudson.model.TaskListener;
import hudson.util.IOException2;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Chmod;
import org.apache.tools.ant.taskdefs.Copy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kohsuke Kawaguchi
 */
public class Util {

    /**
     * Creates a filtered sublist.
     */
    public static <T> List<T> filter( List<?> base, Class<T> type ) {
        List<T> r = new ArrayList<T>();
        for (Object i : base) {
            if(type.isInstance(i))
                r.add(type.cast(i));
        }
        return r;
    }

    /**
     * Pattern for capturing variables. Either $xyz or ${xyz}, while ignoring "$$"
      */
    private static final Pattern VARIABLE = Pattern.compile("(?<!\\$)\\$([A-Za-z0-9_]+|\\{[A-Za-z0-9_]+\\})");

    /**
     * Replaces the occurrence of '$key' by <tt>properties.get('key')</tt>.
     *
     * <p>
     * Unlike shell, undefined variables are left as-is (this behavior is the same as Ant.) 
     *
     */
    public static String replaceMacro(String s, Map<String,String> properties) {
        int idx=0;
        while(true) {
            Matcher m = VARIABLE.matcher(s);
            if(!m.find(idx))   return s;

            String key = m.group().substring(1);
            if(key.charAt(0)=='{')  key = key.substring(1,key.length()-1);

            String value = properties.get(key);
            if(value==null)
                idx = m.start()+1; // skip this
            else {
                s = s.substring(0,m.start())+value+s.substring(m.end());
                idx = m.start();
            }
        }
    }

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
        for (File child : files)
            deleteRecursive(child);
    }

    private static void deleteFile(File f) throws IOException {
        if (!f.delete()) {
            if(!f.exists())
                // we are trying to delete a file that no longer exists, so this is not an error
                return;

            // perhaps this file is read-only?
            // try chmod. this becomes no-op if this is not Unix.
            try {
                Chmod chmod = new Chmod();
                chmod.setProject(new org.apache.tools.ant.Project());
                chmod.setFile(f);
                chmod.setPerm("u+w");
                chmod.execute();
            } catch (BuildException e) {
                LOGGER.log(Level.INFO,"Failed to chmod "+f,e);
            }

            // also try JDK6-way of doing it.
            try {
                f.setWritable(true);
            } catch (NoSuchMethodError e) {
                // not JDK6
            }

            if(!f.delete() && f.exists())
                throw new IOException("Unable to delete " + f.getPath());
        }
    }

    public static void deleteRecursive(File dir) throws IOException {
        if(!isSymlink(dir))
            deleteContentsRecursive(dir);
        deleteFile(dir);
    }

    /*
     * Copyright 2001-2004 The Apache Software Foundation.
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    /**
     * Checks if the given file represents a symlink.
     */
    //Taken from http://svn.apache.org/viewvc/maven/shared/trunk/file-management/src/main/java/org/apache/maven/shared/model/fileset/util/FileSetManager.java?view=markup
    public static boolean isSymlink(File file) throws IOException {
        File parent = file.getParentFile();
        File canonicalFile = file.getCanonicalFile();

        return parent != null
            && (!canonicalFile.getName().equals(file.getName()) || !canonicalFile.getPath().startsWith(
            parent.getCanonicalPath()));
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

    private static final Pattern errorCodeParser = Pattern.compile(".*CreateProcess.*error=([0-9]+).*");

    /**
     * On Windows, error messages for IOException aren't very helpful.
     * This method generates additional user-friendly error message to the listener
     */
    public static void displayIOException( IOException e, TaskListener listener ) {
        String msg = getWin32ErrorMessage(e);
        if(msg!=null)
            listener.getLogger().println(msg);
    }

    public static String getWin32ErrorMessage(IOException e) {
        return getWin32ErrorMessage((Throwable)e);
    }

    /**
     * Extracts the Win32 error message from {@link Throwable} if possible.
     *
     * @return
     *      null if there seems to be no error code or if the platform is not Win32.
     */
    public static String getWin32ErrorMessage(Throwable e) {
        String msg = e.getMessage();
        if(msg!=null) {
            Matcher m = errorCodeParser.matcher(msg);
            if(m.matches()) {
                try {
                    ResourceBundle rb = ResourceBundle.getBundle("/hudson/win32errors");
                    return rb.getString("error"+m.group(1));
                } catch (Exception _) {
                    // silently recover from resource related failures
                }
            }
        }

        if(e.getCause()!=null)
            return getWin32ErrorMessage(e.getCause());
        return null; // no message
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

    public static void copyStream(Reader in, Writer out) throws IOException {
        char[] buf = new char[8192];
        int len;
        while((len=in.read(buf))>0)
            out.write(buf,0,len);
    }

    /**
     * @since 1.145
     */
    public static String[] tokenize(String s,String delimiter) {
        StringTokenizer st = new StringTokenizer(s,delimiter);
        String[] a = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++)
            a[i] = st.nextToken();
        return a;
    }

    public static String[] tokenize(String s) {
        return tokenize(s," \t\n\r\f");
    }

    public static String[] mapToEnv(Map<String,String> m) {
        String[] r = new String[m.size()];
        int idx=0;

        for (final Map.Entry<String,String> e : m.entrySet()) {
            r[idx++] = e.getKey() + '=' + e.getValue();
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

    public static String removeTrailingSlash(String s) {
        if(s.endsWith("/")) return s.substring(0,s.length()-1);
        else                return s;
    }

    /**
     * Write-only buffer.
     */
    private static final byte[] garbage = new byte[8192];

    /**
     * Computes MD5 digest of the given input stream.
     *
     * @param source
     *      The stream will be closed by this method at the end of this method.
     * @return
     *      32-char wide string
     */
    public static String getDigestOf(InputStream source) throws IOException {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            DigestInputStream in =new DigestInputStream(source,md5);
            try {
                while(in.read(garbage)>0)
                    ; // simply discard the input
            } finally {
                in.close();
            }
            return toHexString(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException2("MD5 not installed",e);    // impossible
        }
    }
    
    public static String getDigestOf(String text) {
        try {
            return getDigestOf(new ByteArrayInputStream(text.getBytes("UTF-8")));
        } catch (IOException e) {
            throw new Error(e);
        }
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

    /**
     * Returns a human readable text of the time duration.
     *
     * @param duration
     *      number of milliseconds.
     */
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

    /**
     * Combines number and unit, with a plural suffix if needed.
     */
    public static String combine(long n, String suffix) {
        String s = Long.toString(n)+' '+suffix;
        if(n!=1)
            s += 's';
        return s;
    }

    /**
     * Create a sub-list by only picking up instances of the specified type.
     */
    public static <T> List<T> createSubList( Collection<?> source, Class<T> type ) {
        List<T> r = new ArrayList<T>();
        for (Object item : source) {
            if(type.isInstance(item))
                r.add(type.cast(item));
        }
        return r;
    }

    /**
     * Escapes non-ASCII characters in URL.
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

    /**
     * Escapes HTML unsafe characters like &lt;, &amp;to the respective character entities.
     */
    public static String escape(String text) {
        StringBuffer buf = new StringBuffer(text.length()+64);
        for( int i=0; i<text.length(); i++ ) {
            char ch = text.charAt(i);
            if(ch=='\n')
                buf.append("<br>");
            else
            if(ch=='<')
                buf.append("&lt;");
            else
            if(ch=='&')
                buf.append("&amp;");
            else
            if(ch==' ')
                buf.append("&nbsp;");
            else
                buf.append(ch);
        }
        return buf.toString();
    }

    public static String xmlEscape(String text) {
        StringBuffer buf = new StringBuffer(text.length()+64);
        for( int i=0; i<text.length(); i++ ) {
            char ch = text.charAt(i);
            if(ch=='<')
                buf.append("&lt;");
            else
            if(ch=='&')
                buf.append("&amp;");
            else
                buf.append(ch);
        }
        return buf.toString();
    }

    private static char toDigit(int n) {
        char ch = Character.forDigit(n,16);
        if(ch>='a')     ch = (char)(ch-'a'+'A');
        return ch;
    }

    /**
     * Creates an empty file.
     */
    public static void touch(File file) throws IOException {
        new FileOutputStream(file).close();
    }

    /**
     * Copies a single file by using Ant.
     */
    public static void copyFile(File src, File dst) throws BuildException {
        Copy cp = new Copy();
        cp.setProject(new org.apache.tools.ant.Project());
        cp.setTofile(dst);
        cp.setFile(src);
        cp.setOverwrite(true);
        cp.execute();
    }

    /**
     * Convert null to "".
     */
    public static String fixNull(String s) {
        if(s==null)     return "";
        else            return s;
    }

    /**
     * Convert empty string to null.
     */
    public static String fixEmpty(String s) {
        if(s==null || s.length()==0)    return null;
        return s;
    }

    /**
     * Convert empty string to null, and trim whitespace.
     *
     * @since 1.154
     */
    public static String fixEmptyAndTrim(String s) {
        if(s==null)    return null;
        return fixEmpty(s.trim());
    }

    /**
     * Cuts all the leading path portion and get just the file name.
     */
    public static String getFileName(String filePath) {
        int idx = filePath.lastIndexOf('\\');
        if(idx>=0)
            return getFileName(filePath.substring(idx+1));
        idx = filePath.lastIndexOf('/');
        if(idx>=0)
            return getFileName(filePath.substring(idx+1));
        return filePath;
    }

    /**
     * Concatenate multiple strings by inserting a separator.
     */
    public static String join(Collection<String> strings, String seprator) {
        StringBuilder buf = new StringBuilder();
        boolean first=true;
        for (String s : strings) {
            if(first)   first=false;
            else        buf.append(seprator);
            buf.append(s);
        }
        return buf.toString();
    }


    public static final SimpleDateFormat XS_DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // Note: RFC822 dates must not be localized!
    public static final SimpleDateFormat RFC822_DATETIME_FORMATTER
            = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    static {
        XS_DATETIME_FORMATTER.setTimeZone(new SimpleTimeZone(0,"GMT"));
    }



    private static final Logger LOGGER = Logger.getLogger(Util.class.getName());
}
