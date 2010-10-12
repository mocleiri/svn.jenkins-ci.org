/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Eric Lefevre-Ardant, Erik Ramfelt, Michael B. Donohue
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.remoting.util;

import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.DelegatingCallable;
import hudson.remoting.Future;
import hudson.remoting.Pipe;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.VirtualChannel;
import hudson.remoting.RemoteInputStream;
import hudson.remoting.LocalChannel;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.zip.ZipOutputStream;
import org.apache.tools.zip.ZipEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.Closeable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * {@link File} like object with remoting support.
 *
 * <p>
 * Unlike {@link File}, which always implies a file path on the current computer,
 * {@link FilePath} represents a file path on a specific slave or the master.
 *
 * Despite that, {@link FilePath} can be used much like {@link File}. It exposes
 * a bunch of operations (and we should add more operations as long as they are
 * generally useful), and when invoked against a file on a remote node, {@link FilePath}
 * executes the necessary code remotely, thereby providing semi-transparent file
 * operations.
 *
 * <h2>Using {@link FilePath} smartly</h2>
 * <p>
 * The transparency makes it easy to write plugins without worrying too much about
 * remoting, by making it works like NFS, where remoting happens at the file-system
 * later.
 *
 * <p>
 * But one should note that such use of remoting may not be optional. Sometimes,
 * it makes more sense to move some computation closer to the data, as opposed to
 * move the data to the computation. For example, if you are just computing a MD5
 * digest of a file, then it would make sense to do the digest on the host where
 * the file is located, as opposed to send the whole data to the master and do MD5
 * digesting there.
 *
 * <p>
 * {@link FilePath} supports this "code migration" by in the
 * {@link #act(FileCallable)} method. One can pass in a custom implementation
 * of {@link FileCallable}, to be executed on the node where the data is located.
 * The following code shows the example:
 *
 * <pre>
 * FilePath file = ...;
 *
 * // make 'file' a fresh empty directory.
 * file.act(new FileCallable&lt;Void>() {
 *   // if 'file' is on a different node, this FileCallable will
 *   // be transfered to that node and executed there.
 *   public Void invoke(File f,VirtualChannel channel) {
 *     // f and file represents the same thing
 *     f.deleteContents();
 *     f.mkdirs();
 *   }
 * });
 * </pre>
 *
 * <p>
 * When {@link FileCallable} is transfered to a remote node, it will be done so
 * by using the same Java serialization scheme that the remoting module uses.
 * See {@link Channel} for more about this.
 *
 * <p>
 * {@link FilePath} itself can be sent over to a remote node as a part of {@link Callable}
 * serialization. For example, sending a {@link FilePath} of a remote node to that
 * node causes {@link FilePath} to become "local". Similarly, sending a
 * {@link FilePath} that represents the local computer causes it to become "remote."
 *
 * @author Kohsuke Kawaguchi
 */
public class FilePath implements Serializable {
    /**
     * When this {@link FilePath} represents the remote path,
     * this field is always non-null on master (the field represents
     * the channel to the remote slave.) When transferred to a slave via remoting,
     * this field reverts back to null, since it's transient.
     *
     * When this {@link FilePath} represents a path on the master,
     * this field is null on master. When transferred to a slave via remoting,
     * this field becomes non-null, representing the {@link Channel}
     * back to the master.
     *
     * This is used to determine whether we are running on the master or the slave.
     */
    private transient VirtualChannel channel;

    // since the platform of the slave might be different, can't use java.io.File
    private final String remote;

    /**
     * Creates a {@link FilePath} that represents a path on the given node.
     *
     * @param channel
     *      To create a path that represents a remote path, pass in a {@link Channel}
     *      that's connected to that machine. If null, that means the local file path.
     */
    public FilePath(VirtualChannel channel, String remote) {
        this.channel = channel;
        this.remote = remote;
    }

    /**
     * To create {@link FilePath} that represents a "local" path.
     *
     * <p>
     * A "local" path means a file path on the computer where the
     * constructor invocation happened.
     */
    public FilePath(File localPath) {
        this.channel = null;
        this.remote = localPath.getPath();
    }

    /**
     * Construct a path starting with a base location.
     * @param base starting point for resolution, and defines channel
     * @param rel a path which if relative will be resolved against base
     */
    public FilePath(FilePath base, String rel) {
        this.channel = base.channel;
        if(isAbsolute(rel)) {
            // absolute
            this.remote = rel;
        } else
        if(base.isUnix()) {
            this.remote = base.remote+'/'+rel;
        } else {
            this.remote = base.remote+'\\'+rel;
        }
    }

    private static boolean isAbsolute(String rel) {
        return rel.startsWith("/") || DRIVE_PATTERN.matcher(rel).matches();
    }

    private static final Pattern DRIVE_PATTERN = Pattern.compile("[A-Za-z]:\\\\.+");

    /**
     * Checks if the remote path is Unix.
     */
    private boolean isUnix() {
        // if the path represents a local path, there' no need to guess.
        if(!isRemote())
            return File.pathSeparatorChar!=';';

        // note that we can't use the usual File.pathSeparator and etc., as the OS of
        // the machine where this code runs and the OS that this FilePath refers to may be different.

        // Windows absolute path is 'X:\...', so this is usually a good indication of Windows path
        if(remote.length()>3 && remote.charAt(1)==':' && remote.charAt(2)=='\\')
            return false;
        // Windows can handle '/' as a path separator but Unix can't,
        // so err on Unix side
        return remote.indexOf("\\")==-1;
    }

    public String getRemote() {
        return remote;
    }

    /**
     * Creates a zip file from this directory or a file and sends that to the given output stream.
     *
     * @deprecated as of 1.315. Use {@link #zip(OutputStream)} that has more consistent name.
     */
    public void createZipArchive(OutputStream os) throws IOException, InterruptedException {
        zip(os);
    }

    /**
     * Creates a zip file from this directory or a file and sends that to the given output stream.
     */
    public void zip(OutputStream os) throws IOException, InterruptedException {
        zip(os,(FileFilter)null);
    }

    /**
     * Creates a zip file from this directory by using the specified filter,
     * and sends the result to the given output stream.
     *
     * @param filter
     *      Must be serializable since it may be executed remotely. Can be null to add all files.
     *
     * @since 1.315
     */
    public void zip(OutputStream os, FileFilter filter) throws IOException, InterruptedException {
        archive(new ZipArchiverFactory(),os,filter);
    }

    /**
     * Creates a zip file from this directory by only including the files that match the given glob.
     *
     * @param glob
     *      Ant style glob, like "**&#x2F;*.xml". If empty or null, this method
     *      works like {@link #createZipArchive(OutputStream)}
     *
     * @since 1.129
     * @deprecated as of 1.315
     *      Use {@link #zip(OutputStream,String)} that has more consistent name.
     */
    public void createZipArchive(OutputStream os, final String glob) throws IOException, InterruptedException {
        archive(new ZipArchiverFactory(),os,glob);
    }

    /**
     * Creates a zip file from this directory by only including the files that match the given glob.
     *
     * @param glob
     *      Ant style glob, like "**&#x2F;*.xml". If empty or null, this method
     *      works like {@link #createZipArchive(OutputStream)}
     *
     * @since 1.315
     */
    public void zip(OutputStream os, final String glob) throws IOException, InterruptedException {
        archive(new ZipArchiverFactory(),os,glob);
    }

    /**
     * Archives this directory into the specified archive format, to the given {@link OutputStream}, by using
     * {@link DirScanner} to choose what files to include.
     *
     * @return
     *      number of files/directories archived. This is only really useful to check for a situation where nothing
     *      is archived.
     */
    private int archive(final ArchiverFactory factory, OutputStream os, final DirScanner scanner) throws IOException, InterruptedException {
        final OutputStream out = (channel!=null)?new RemoteOutputStream(os):os;
        return act(new FileCallable<Integer>() {
            public Integer invoke(File f, VirtualChannel channel) throws IOException {
                Archiver a = factory.create(out);
                try {
                    scanner.scan(f,a);
                } finally {
                    a.close();
                }
                return a.countEntries();
            }

            private static final long serialVersionUID = 1L;
        });
    }

    private int archive(final ArchiverFactory factory, OutputStream os, final FileFilter filter) throws IOException, InterruptedException {
        return archive(factory,os,new DirScanner.Filter(filter));
    }

    private int archive(final ArchiverFactory factory, OutputStream os, final String glob) throws IOException, InterruptedException {
        return archive(factory,os,new DirScanner.Glob(glob,null));
    }

    /**
     * When this {@link FilePath} represents a zip file, extracts that zip file.
     *
     * @param target
     *      Target directory to expand files to. All the necessary directories will be created.
     * @since 1.248
     * @see #unzipFrom(InputStream)
     */
    public void unzip(final FilePath target) throws IOException, InterruptedException {
        target.act(new FileCallable<Void>() {
            public Void invoke(File dir, VirtualChannel channel) throws IOException {
                unzip(dir,FilePath.this.read());
                return null;
            }
            private static final long serialVersionUID = 1L;
        });
    }

    /**
     * When this {@link FilePath} represents a tar file, extracts that tar file.
     *
     * @param target
     *      Target directory to expand files to. All the necessary directories will be created.
     * @param compression
     *      Compression mode of this tar file.
     * @since 1.292
     * @see #untarFrom(InputStream, TarCompression)
     */
    public void untar(final FilePath target, final TarCompression compression) throws IOException, InterruptedException {
        target.act(new FileCallable<Void>() {
            public Void invoke(File dir, VirtualChannel channel) throws IOException {
                readFromTar(FilePath.this.getName(),dir,compression.extract(FilePath.this.read()));
                return null;
            }
            private static final long serialVersionUID = 1L;
        });
    }

    /**
     * Reads the given InputStream as a zip file and extracts it into this directory.
     *
     * @param _in
     *      The stream will be closed by this method after it's fully read.
     * @since 1.283
     * @see #unzip(FilePath)
     */
    public void unzipFrom(InputStream _in) throws IOException, InterruptedException {
        final InputStream in = new RemoteInputStream(_in);
        act(new FileCallable<Void>() {
            public Void invoke(File dir, VirtualChannel channel) throws IOException {
                unzip(dir, in);
                return null;
            }
            private static final long serialVersionUID = 1L;
        });
    }

    private void unzip(File dir, InputStream in) throws IOException {
        dir = dir.getAbsoluteFile();    // without absolutization, getParentFile below seems to fail
        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(in));
        java.util.zip.ZipEntry e;

        try {
            while((e=zip.getNextEntry())!=null) {
                File f = new File(dir,e.getName());
                if(e.isDirectory()) {
                    f.mkdirs();
                } else {
                    File p = f.getParentFile();
                    if(p!=null) p.mkdirs();
                    FileOutputStream out = new FileOutputStream(f);
                    try {
                        IOUtils.copy(zip, out);
                    } finally {
                        out.close();
                    }
                    f.setLastModified(e.getTime());
                    zip.closeEntry();
                }
            }
        } finally {
            zip.close();
        }
    }

    /**
     * Absolutizes this {@link FilePath} and returns the new one.
     */
    public FilePath absolutize() throws IOException, InterruptedException {
        return new FilePath(channel,act(new FileCallable<String>() {
            public String invoke(File f, VirtualChannel channel) throws IOException {
                return f.getAbsolutePath();
            }
        }));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilePath that = (FilePath) o;

        if (channel != null ? !channel.equals(that.channel) : that.channel != null) return false;
        return remote.equals(that.remote);

    }

    @Override
    public int hashCode() {
        return 31 * (channel != null ? channel.hashCode() : 0) + remote.hashCode();
    }

    /**
     * Supported tar file compression methods.
     */
    public enum TarCompression {
        NONE {
            public InputStream extract(InputStream in) {
                return in;
            }
            public OutputStream compress(OutputStream out) {
                return out;
            }
        },
        GZIP {
            public InputStream extract(InputStream in) throws IOException {
                return new GZIPInputStream(in,8192);
            }
            public OutputStream compress(OutputStream out) throws IOException {
                return new GZIPOutputStream(new BufferedOutputStream(out));
            }
        };

        public abstract InputStream extract(InputStream in) throws IOException;
        public abstract OutputStream compress(OutputStream in) throws IOException;
    }

    /**
     * Reads the given InputStream as a tar file and extracts it into this directory.
     *
     * @param _in
     *      The stream will be closed by this method after it's fully read.
     * @param compression
     *      The compression method in use.
     * @since 1.292
     */
    public void untarFrom(InputStream _in, final TarCompression compression) throws IOException, InterruptedException {
        try {
            final InputStream in = new RemoteInputStream(_in);
            act(new FileCallable<Void>() {
                public Void invoke(File dir, VirtualChannel channel) throws IOException {
                    readFromTar("input stream",dir, compression.extract(in));
                    return null;
                }
                private static final long serialVersionUID = 1L;
            });
        } finally {
            IOUtils.closeQuietly(_in);
        }
    }

    /**
     * Reads the URL on the current VM, and writes all the data to this {@link FilePath}
     * (this is different from resolving URL remotely.)
     *
     * @since 1.293
     */
    public void copyFrom(URL url) throws IOException, InterruptedException {
        InputStream in = url.openStream();
        try {
            copyFrom(in);
        } finally {
            in.close();
        }
    }

    /**
     * Replaces the content of this file by the data from the given {@link InputStream}.
     *
     * @since 1.293
     */
    public void copyFrom(InputStream in) throws IOException, InterruptedException {
        OutputStream os = write();
        try {
            IOUtils.copy(in, os);
        } finally {
            os.close();
        }
    }

    /**
     * Conveniene method to call {@link FilePath#copyTo(FilePath)}.
     *
     * @since 1.311
     */
    public void copyFrom(FilePath src) throws IOException, InterruptedException {
        src.copyTo(this);
    }

    /**
     * Code that gets executed on the machine where the {@link FilePath} is local.
     * Used to act on {@link FilePath}.
     *
     * @see FilePath#act(FileCallable)
     */
    public static interface FileCallable<T> extends Serializable {
        /**
         * Performs the computational task on the node where the data is located.
         *
         * @param f
         *      {@link File} that represents the local file that {@link FilePath} has represented.
         * @param channel
         *      The "back pointer" of the {@link Channel} that represents the communication
         *      with the node from where the code was sent.
         */
        T invoke(File f, VirtualChannel channel) throws IOException;
    }

    /**
     * Executes some program on the machine that this {@link FilePath} exists,
     * so that one can perform local file operations.
     */
    public <T> T act(final FileCallable<T> callable) throws IOException, InterruptedException {
        return act(callable,callable.getClass().getClassLoader());
    }

    private <T> T act(final FileCallable<T> callable, ClassLoader cl) throws IOException, InterruptedException {
        if(channel!=null) {
            // run this on a remote system
            try {
                return channel.call(new FileCallableWrapper<T>(callable,cl));
            } catch (IOException e) {
                // wrap it into a new IOException so that we get the caller's stack trace as well.
                throw new IOException2("remote file operation failed",e);
            }
        } else {
            // the file is on the local machine.
            return callable.invoke(new File(remote), DEFAULT_LOCAL_CHANNEL);
        }
    }

    /**
     * Executes some program on the machine that this {@link FilePath} exists,
     * so that one can perform local file operations.
     */
    public <T> Future<T> actAsync(final FileCallable<T> callable) throws IOException, InterruptedException {
        try {
            return (channel!=null ? channel : DEFAULT_LOCAL_CHANNEL)
                .callAsync(new FileCallableWrapper<T>(callable));
        } catch (IOException e) {
            // wrap it into a new IOException so that we get the caller's stack trace as well.
            throw new IOException2("remote file operation failed",e);
        }
    }

    /**
     * Executes some program on the machine that this {@link FilePath} exists,
     * so that one can perform local file operations.
     */
    public <V,E extends Throwable> V act(Callable<V,E> callable) throws IOException, InterruptedException, E {
        if(channel!=null) {
            // run this on a remote system
            return channel.call(callable);
        } else {
            // the file is on the local machine
            return callable.call();
        }
    }

    /**
     * Converts this file to the URI, relative to the machine
     * on which this file is available.
     */
    public URI toURI() throws IOException, InterruptedException {
        return act(new FileCallable<URI>() {
            public URI invoke(File f, VirtualChannel channel) {
                return f.toURI();
            }
        });
    }

    /**
     * Creates this directory.
     */
    public void mkdirs() throws IOException, InterruptedException {
        if(!act(new FileCallable<Boolean>() {
            public Boolean invoke(File f, VirtualChannel channel) throws IOException {
                if(f.mkdirs() || f.exists())
                    return true;    // OK

                // following Ant <mkdir> task to avoid possible race condition.
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // ignore
                }

                return f.mkdirs() || f.exists();
            }
        }))
            throw new IOException("Failed to mkdirs: "+remote);
    }

    /**
     * Deletes this directory, including all its contents recursively.
     */
    public void deleteRecursive() throws IOException, InterruptedException {
        act(new FileCallable<Void>() {
            public Void invoke(File f, VirtualChannel channel) throws IOException {
                FileUtils.deleteDirectory(f);
                return null;
            }
        });
    }

    /**
     * Deletes all the contents of this directory, but not the directory itself
     */
    public void deleteContents() throws IOException, InterruptedException {
        act(new FileCallable<Void>() {
            public Void invoke(File f, VirtualChannel channel) throws IOException {
                FileUtils.cleanDirectory(f);
                return null;
            }
        });
    }

    /**
     * Gets just the file name portion.
     *
     * This method assumes that the file name is the same between local and remote.
     */
    public String getName() {
        String r = remote;
        if(r.endsWith("\\") || r.endsWith("/"))
            r = r.substring(0,r.length()-1);

        int len = r.length()-1;
        while(len>=0) {
            char ch = r.charAt(len);
            if(ch=='\\' || ch=='/')
                break;
            len--;
        }

        return r.substring(len+1);
    }

    /**
     * Short for {@code getParent().child(rel)}. Useful for getting other files in the same directory.
     */
    public FilePath sibling(String rel) {
        return getParent().child(rel);
    }

    /**
     * Returns a {@link FilePath} by adding the given suffix to this path name.
     */
    public FilePath withSuffix(String suffix) {
        return new FilePath(channel,remote+suffix);
    }

    /**
     * The same as {@link FilePath#FilePath(FilePath,String)} but more OO.
     * @param rel a relative or absolute path
     * @return a file on the same channel
     */
    public FilePath child(String rel) {
        return new FilePath(this,rel);
    }

    /**
     * Gets the parent file.
     */
    public FilePath getParent() {
        int len = remote.length()-1;
        while(len>=0) {
            char ch = remote.charAt(len);
            if(ch=='\\' || ch=='/')
                break;
            len--;
        }

        return new FilePath( channel, remote.substring(0,len) );
    }

    /**
     * Creates a temporary file in the directory that this {@link FilePath} object designates.
     */
    public FilePath createTempFile(final String prefix, final String suffix) throws IOException, InterruptedException {
        try {
            return new FilePath(this,act(new FileCallable<String>() {
                public String invoke(File dir, VirtualChannel channel) throws IOException {
                    File f = File.createTempFile(prefix, suffix, dir);
                    return f.getName();
                }
            }));
        } catch (IOException e) {
            throw new IOException2("Failed to create a temp file on "+remote,e);
        }
    }

    /**
     * Creates a temporary file in this directory and set the contents by the
     * given text (encoded in the platform default encoding)
     */
    public FilePath createTextTempFile(final String prefix, final String suffix, final String contents) throws IOException, InterruptedException {
        return createTextTempFile(prefix,suffix,contents,true);
    }

    /**
     * Creates a temporary file in this directory and set the contents by the
     * given text (encoded in the platform default encoding)
     */
    public FilePath createTextTempFile(final String prefix, final String suffix, final String contents, final boolean inThisDirectory) throws IOException, InterruptedException {
        try {
            return new FilePath(channel,act(new FileCallable<String>() {
                public String invoke(File dir, VirtualChannel channel) throws IOException {
                    if(!inThisDirectory)
                        dir = new File(System.getProperty("java.io.tmpdir"));
                    else
                        dir.mkdirs();

                    File f;
                    try {
                        f = File.createTempFile(prefix, suffix, dir);
                    } catch (IOException e) {
                        throw new IOException2("Failed to create a temporary directory in "+dir,e);
                    }

                    Writer w = new FileWriter(f);
                    w.write(contents);
                    w.close();

                    return f.getAbsolutePath();
                }
            }));
        } catch (IOException e) {
            throw new IOException2("Failed to create a temp file on "+remote,e);
        }
    }

    /**
     * Creates a temporary directory inside the directory represented by 'this'
     * @since 1.311
     */
    public FilePath createTempDir(final String prefix, final String suffix) throws IOException, InterruptedException {
        try {
            return new FilePath(this,act(new FileCallable<String>() {
                public String invoke(File dir, VirtualChannel channel) throws IOException {
                    File f = File.createTempFile(prefix, suffix, dir);
                    f.delete();
                    f.mkdir();
                    return f.getName();
                }
            }));
        } catch (IOException e) {
            throw new IOException2("Failed to create a temp directory on "+remote,e);
        }
    }

    /**
     * Deletes this file.
     * @throws IOException if it exists but could not be successfully deleted
     * @return true, for a modicum of compatibility
     */
    public boolean delete() throws IOException, InterruptedException {
        act(new FileCallable<Void>() {
            public Void invoke(File f, VirtualChannel channel) throws IOException {
                f.delete();
                return null;
            }
        });
        return true;
    }

    /**
     * Checks if the file exists.
     */
    public boolean exists() throws IOException, InterruptedException {
        return act(new FileCallable<Boolean>() {
            public Boolean invoke(File f, VirtualChannel channel) throws IOException {
                return f.exists();
            }
        });
    }

    /**
     * Gets the last modified time stamp of this file, by using the clock
     * of the machine where this file actually resides.
     *
     * @see File#lastModified()
     * @see #touch(long)
     */
    public long lastModified() throws IOException, InterruptedException {
        return act(new FileCallable<Long>() {
            public Long invoke(File f, VirtualChannel channel) throws IOException {
                return f.lastModified();
            }
        });
    }

    /**
     * Creates a file (if not already exist) and sets the timestamp.
     *
     * @since 1.299
     */
    public void touch(final long timestamp) throws IOException, InterruptedException {
        act(new FileCallable<Void>() {
            public Void invoke(File f, VirtualChannel channel) throws IOException {
                if(!f.exists())
                    new FileOutputStream(f).close();
                if(!f.setLastModified(timestamp))
                    throw new IOException("Failed to set the timestamp of "+f+" to "+timestamp);
                return null;
            }
        });
    }

    /**
     * Checks if the file is a directory.
     */
    public boolean isDirectory() throws IOException, InterruptedException {
        return act(new FileCallable<Boolean>() {
            public Boolean invoke(File f, VirtualChannel channel) throws IOException {
                return f.isDirectory();
            }
        });
    }

    /**
     * Returns the file size in bytes.
     *
     * @since 1.129
     */
    public long length() throws IOException, InterruptedException {
        return act(new FileCallable<Long>() {
            public Long invoke(File f, VirtualChannel channel) throws IOException {
                return f.length();
            }
        });
    }

    /**
     * List up files and directories in this directory.
     *
     * <p>
     * This method returns direct children of the directory denoted by the 'this' object.
     */
    public List<FilePath> list() throws IOException, InterruptedException {
        return list((FileFilter)null);
    }

    /**
     * List up subdirectories.
     *
     * @return can be empty but never null. Doesn't contain "." and ".."
     */
    public List<FilePath> listDirectories() throws IOException, InterruptedException {
        return list(new DirectoryFilter());
    }

    private static final class DirectoryFilter implements FileFilter, Serializable {
        public boolean accept(File f) {
            return f.isDirectory();
        }
        private static final long serialVersionUID = 1L;
    }

    /**
     * List up files in this directory, just like {@link File#listFiles(FileFilter)}.
     *
     * @param filter
     *      The optional filter used to narrow down the result.
     *      If non-null, must be {@link Serializable}.
     *      If this {@link FilePath} represents a remote path,
     *      the filter object will be executed on the remote machine.
     */
    public List<FilePath> list(final FileFilter filter) throws IOException, InterruptedException {
        if (filter != null && !(filter instanceof Serializable)) {
            throw new IllegalArgumentException("Non-serializable filter of " + filter.getClass());
        }
        return act(new FileCallable<List<FilePath>>() {
            public List<FilePath> invoke(File f, VirtualChannel channel) throws IOException {
                File[] children = f.listFiles(filter);
                if(children ==null)     return null;

                ArrayList<FilePath> r = new ArrayList<FilePath>(children.length);
                for (File child : children)
                    r.add(new FilePath(child));

                return r;
            }
        }, (filter!=null?filter:this).getClass().getClassLoader());
    }

    /**
     * List up files in this directory that matches the given Ant-style filter.
     *
     * @param includes
     *      See {@link FileSet} for the syntax. String like "foo/*.zip" or "foo/*&#42;/*.xml"
     * @return
     *      can be empty but always non-null.
     */
    public FilePath[] list(final String includes) throws IOException, InterruptedException {
        return act(new FileCallable<FilePath[]>() {
            public FilePath[] invoke(File f, VirtualChannel channel) throws IOException {
                String[] files = glob(f,includes);

                FilePath[] r = new FilePath[files.length];
                for( int i=0; i<r.length; i++ )
                    r[i] = new FilePath(new File(f,files[i]));

                return r;
            }
        });
    }

    /**
     * Runs Ant glob expansion.
     *
     * @return
     *      A set of relative file names from the base directory.
     */
    private static String[] glob(File dir, String includes) throws IOException {
        if(isAbsolute(includes))
            throw new IOException("Expecting Ant GLOB pattern, but saw '"+includes+"'. See http://ant.apache.org/manual/CoreTypes/fileset.html for syntax");
        FileSet fs = createFileSet(dir,includes);
        DirectoryScanner ds = fs.getDirectoryScanner(new Project());
        String[] files = ds.getIncludedFiles();
        return files;
    }

    /**
     * Reads this file.
     */
    public InputStream read() throws IOException {
        if(channel==null)
            return new FileInputStream(new File(remote));

        final Pipe p = Pipe.createRemoteToLocal();
        channel.callAsync(new Callable<Void,IOException>() {
            public Void call() throws IOException {
                FileInputStream fis=null;
                try {
                    fis = new FileInputStream(new File(remote));
                    IOUtils.copy(fis,p.getOut());
                    return null;
                } finally {
                    IOUtils.closeQuietly(fis);
                    IOUtils.closeQuietly(p.getOut());
                }
            }
        });

        return p.getIn();
    }

    /**
     * Reads this file into a string, by using the current system encoding.
     */
    public String readToString() throws IOException {
        InputStream in = read();
        try {
            return IOUtils.toString(in);
        } finally {
            in.close();
        }
    }

    /**
     * Writes to this file.
     * If this file already exists, it will be overwritten.
     * If the directory doesn't exist, it will be created.
     */
    public OutputStream write() throws IOException, InterruptedException {
        if(channel==null) {
            File f = new File(remote).getAbsoluteFile();
            f.getParentFile().mkdirs();
            return new FileOutputStream(f);
        }

        return channel.call(new Callable<OutputStream,IOException>() {
            public OutputStream call() throws IOException {
                File f = new File(remote).getAbsoluteFile();
                f.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(f);
                return new RemoteOutputStream(fos);
            }
        });
    }

    /**
     * Overwrites this file by placing the given String as the content.
     *
     * @param encoding
     *      Null to use the platform default encoding.
     * @since 1.105
     */
    public void write(final String content, final String encoding) throws IOException, InterruptedException {
        act(new FileCallable<Void>() {
            public Void invoke(File f, VirtualChannel channel) throws IOException {
                f.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(f);
                Writer w = encoding != null ? new OutputStreamWriter(fos, encoding) : new OutputStreamWriter(fos);
                try {
                    w.write(content);
                } finally {
                    w.close();
                }
                return null;
            }
        });
    }

    /**
     * Rename this file/directory to the target filepath.  This FilePath and the target must
     * be on the some host
     */
    public void renameTo(final FilePath target) throws IOException, InterruptedException {
    	if(this.channel != target.channel) {
    		throw new IOException("renameTo target must be on the same host");
    	}
        act(new FileCallable<Void>() {
            public Void invoke(File f, VirtualChannel channel) throws IOException {
            	f.renameTo(new File(target.remote));
                return null;
            }
        });
    }

    /**
     * Moves all the contents of this directory into the specified directory, then delete this directory itself.
     *
     * @since 1.308.
     */
    public void moveAllChildrenTo(final FilePath target) throws IOException, InterruptedException {
        if(this.channel != target.channel) {
            throw new IOException("pullUpTo target must be on the same host");
        }
        act(new FileCallable<Void>() {
            public Void invoke(File f, VirtualChannel channel) throws IOException {
                File t = new File(target.getRemote());

                for(File child : f.listFiles()) {
                    File target = new File(t, child.getName());
                    if(!child.renameTo(target))
                        throw new IOException("Failed to rename "+child+" to "+target);
                }
                f.delete();
                return null;
            }
        });
    }

    /**
     * Copies this file to the specified target.
     */
    public void copyTo(FilePath target) throws IOException, InterruptedException {
        OutputStream out = target.write();
        try {
            copyTo(out);
        } finally {
            out.close();
        }
    }

    /**
     * Sends the contents of this file into the given {@link OutputStream}.
     */
    public void copyTo(OutputStream os) throws IOException, InterruptedException {
        final OutputStream out = new RemoteOutputStream(os);

        act(new FileCallable<Void>() {
            public Void invoke(File f, VirtualChannel channel) throws IOException {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(f);
                    IOUtils.copy(fis,out);
                    return null;
                } finally {
                    IOUtils.closeQuietly(fis);
                    IOUtils.closeQuietly(out);
                }
            }
        });
    }

    /**
     * Remoting interface used for {@link FilePath#copyRecursiveTo(String, FilePath)}.
     *
     * TODO: this might not be the most efficient way to do the copy.
     */
    interface RemoteCopier {
        /**
         * @param fileName
         *      relative path name to the output file. Path separator must be '/'.
         */
        void open(String fileName) throws IOException;
        void write(byte[] buf, int len) throws IOException;
        void close() throws IOException;
    }

    /**
     * Copies the contents of this directory recursively into the specified target directory.
     * @since 1.312
     */
    public int copyRecursiveTo(FilePath target) throws IOException, InterruptedException {
        return copyRecursiveTo("**/*",target);
    }

    public int copyRecursiveTo(String fileMask, FilePath target) throws IOException, InterruptedException {
        return copyRecursiveTo(fileMask,null,target);
    }

    /**
     * Copies the files that match the given file mask to the specified target node.
     *
     * @param fileMask
     *      Ant GLOB pattern.
     *      String like "foo/bar/*.xml" Multiple patterns can be separated
     *      by ',', and whitespace can surround ',' (so that you can write
     *      "abc, def" and "abc,def" to mean the same thing.
     * @param excludes
     *      Files to be excluded. Can be null.
     * @return
     *      the number of files copied.
     */
    public int copyRecursiveTo(final String fileMask, final String excludes, final FilePath target) throws IOException, InterruptedException {
        if(this.channel==target.channel) {
            // local to local copy.
            return act(new FileCallable<Integer>() {
                public Integer invoke(File base, VirtualChannel channel) throws IOException {
                    if(!base.exists())  return 0;
                    assert target.channel==null;

                    try {
                        class CopyImpl extends Copy {
                            private int copySize;

                            public CopyImpl() {
                                setProject(new org.apache.tools.ant.Project());
                            }

                            @Override
                            protected void doFileOperations() {
                                copySize = super.fileCopyMap.size();
                                super.doFileOperations();
                            }

                            public int getNumCopied() {
                                return copySize;
                            }
                        }

                        CopyImpl copyTask = new CopyImpl();
                        copyTask.setTodir(new File(target.remote));
                        copyTask.addFileset(createFileSet(base,fileMask,excludes));
                        copyTask.setIncludeEmptyDirs(false);

                        copyTask.execute();
                        return copyTask.getNumCopied();
                    } catch (BuildException e) {
                        throw new IOException2("Failed to copy "+base+"/"+fileMask+" to "+target,e);
                    }
                }
            });
        } else
        if(this.channel==null) {
            // local -> remote copy
            final Pipe pipe = Pipe.createLocalToRemote();

            Future<Void> future = target.actAsync(new FileCallable<Void>() {
                public Void invoke(File f, VirtualChannel channel) throws IOException {
                    try {
                        readFromTar(remote+'/'+fileMask, f,TarCompression.GZIP.extract(pipe.getIn()));
                        return null;
                    } finally {
                        pipe.getIn().close();
                    }
                }
            });
            int r = writeToTar(new File(remote),fileMask,excludes,TarCompression.GZIP.compress(pipe.getOut()));
            try {
                future.get();
            } catch (ExecutionException e) {
                throw new IOException2(e);
            }
            return r;
        } else {
            // remote -> local copy
            final Pipe pipe = Pipe.createRemoteToLocal();

            Future<Integer> future = actAsync(new FileCallable<Integer>() {
                public Integer invoke(File f, VirtualChannel channel) throws IOException {
                    try {
                        return writeToTar(f,fileMask,excludes,TarCompression.GZIP.compress(pipe.getOut()));
                    } finally {
                        pipe.getOut().close();
                    }
                }
            });
            try {
                readFromTar(remote+'/'+fileMask,new File(target.remote),TarCompression.GZIP.extract(pipe.getIn()));
            } catch (IOException e) {// BuildException or IOException
                try {
                    future.get(3,TimeUnit.SECONDS);
                    throw e;    // the remote side completed successfully, so the error must be local
                } catch (ExecutionException x) {
                    // report both errors
                    StringWriter s = new StringWriter();
                    e.printStackTrace(new PrintWriter(s));
                    throw new IOException2(s.toString(),x);
                } catch (TimeoutException _) {
                    // remote is hanging
                    throw e;
                }
            }
            try {
                return future.get();
            } catch (ExecutionException e) {
                throw new IOException2(e);
            }
        }
    }

    private static abstract class DirScanner implements Serializable {
        abstract void scan(File dir, FileVisitor visitor) throws IOException;

        /**
         * Scans everything recursively.
         */
        private static class Full extends DirScanner {
            private void scan(File f, String path, FileVisitor visitor) throws IOException {
                if (f.canRead()) {
                    visitor.visit(f,path+f.getName());
                    if(f.isDirectory()) {
                        for( File child : f.listFiles() )
                            scan(child,path+f.getName()+'/',visitor);
                    }
                }
            }

            void scan(File dir, FileVisitor visitor) throws IOException {
                scan(dir,"",visitor);
            }

            private static final long serialVersionUID = 1L;
        }

        /**
         * Scans by filtering things out from {@link FileFilter}
         */
        private static class Filter extends Full {
            private final FileFilter filter;

            Filter(FileFilter filter) {
                this.filter = filter;
            }

            @Override
            void scan(File dir, FileVisitor visitor) throws IOException {
                super.scan(dir,visitor.with(filter));
            }

            private static final long serialVersionUID = 1L;
        }

        /**
         * Scans by using Ant GLOB syntax.
         */
        private static class Glob extends DirScanner {
            private final String includes, excludes;

            private Glob(String includes, String excludes) {
                this.includes = includes;
                this.excludes = excludes;
            }

            void scan(File dir, FileVisitor visitor) throws IOException {
                if(includes==null && excludes==null) {
                    // optimization
                    new Full().scan(dir,visitor);
                    return;
                }

                FileSet fs = createFileSet(dir,includes,excludes);

                if(dir.exists()) {
                    DirectoryScanner ds = fs.getDirectoryScanner(new org.apache.tools.ant.Project());
                    for( String f : ds.getIncludedFiles()) {
                        File file = new File(dir, f);
                        visitor.visit(file,f);
                    }
                }
            }

            private static final long serialVersionUID = 1L;
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Visits files in a directory recursively.
     * Primarily used for building an archive with various filtering.
     */
    private static abstract class FileVisitor {
        /**
         * Caleld for each file and directory that matches the criteria.
         *
         * @param relativePath
         *      The file/directory name in question
         */
        abstract void visit(File f, String relativePath) throws IOException;

        /**
         * Decorates by a given filter.
         */
        FileVisitor with(FileFilter f) {
            if(f==null) return this;
            return new FilterFileVisitor(f,this);
        }
    }

    private static final class FilterFileVisitor extends FileVisitor implements Serializable {
        private final FileFilter filter;
        private final FileVisitor visitor;

        private FilterFileVisitor(FileFilter filter, FileVisitor visitor) {
            this.filter = filter!=null ? filter : PASS_THROUGH;
            this.visitor = visitor;
        }

        public void visit(File f, String relativePath) throws IOException {
            if(f.isDirectory() || filter.accept(f))
                visitor.visit(f,relativePath);
        }

        private static final FileFilter PASS_THROUGH = new FileFilter() {
            public boolean accept(File pathname) {
                return true;
            }
        };

        private static final long serialVersionUID = 1L;
    }

    private static interface ArchiverFactory extends Serializable {
        Archiver create(OutputStream out);
    }

    private static final class TarArchiverFactory implements ArchiverFactory, Serializable {
        public Archiver create(OutputStream out) {
            return new TarWriter(out);
        }

        private static final long serialVersionUID = 1L;
    }

    private static final class ZipArchiverFactory implements ArchiverFactory, Serializable {
        public Archiver create(OutputStream out) {
            return new ZipWriter(out);
        }

        private static final long serialVersionUID = 1L;
    }


    /**
     * Base for {@link TarWriter} and {@link ZipWriter}.
     */
    private static abstract class Archiver extends FileVisitor implements Closeable {
        protected int entriesWritten =0;

        /**
         * Number of files/directories archived.
         */
        public int countEntries() {
            return entriesWritten;
        }
    }

    /**
     * {@link FileVisitor} that creates a tar archive.
     */
    private static final class TarWriter extends Archiver {
        private final byte[] buf = new byte[8192];
        private final TarOutputStream tar;

        private TarWriter(OutputStream out) {
            tar = new TarOutputStream(new BufferedOutputStream(out) {
                // TarOutputStream uses TarBuffer internally,
                // which flushes the stream for each block. this creates unnecessary
                // data stream fragmentation, and flush request to a remote, which slows things down.
                @Override
                public void flush() throws IOException {
                    // so don't do anything in flush
                }
            });
            tar.setLongFileMode(TarOutputStream.LONGFILE_GNU);
        }

        public void visit(File file, String relativePath) throws IOException {
            if(isWindows())
                relativePath = relativePath.replace('\\','/');

            if(file.isDirectory())
                relativePath+='/';
            TarEntry te = new TarEntry(relativePath);
            te.setModTime(file.lastModified());
            if(!file.isDirectory())
                te.setSize(file.length());

            tar.putNextEntry(te);

            if (!file.isDirectory()) {
                FileInputStream in = new FileInputStream(file);
                int len;
                while((len=in.read(buf))>=0)
                    tar.write(buf,0,len);
                in.close();
            }

            tar.closeEntry();
            entriesWritten++;
        }

        public void close() throws IOException {
            tar.close();
        }
    }

    /**
     * {@link FileVisitor} that creates a zip archive.
     */
    private static final class ZipWriter extends Archiver {
        private final byte[] buf = new byte[8192];
        private final ZipOutputStream zip;

        private ZipWriter(OutputStream out) {
            zip = new ZipOutputStream(out);
            zip.setEncoding(System.getProperty("file.encoding"));
        }

        public void visit(File f, String relativePath) throws IOException {
            if(f.isDirectory()) {
                ZipEntry dirZipEntry = new ZipEntry(relativePath+'/');
                // Setting this bit explicitly is needed by some unzipping applications (see HUDSON-3294).
                dirZipEntry.setExternalAttributes(BITMASK_IS_DIRECTORY);
                zip.putNextEntry(dirZipEntry);
                zip.closeEntry();
            } else {
                zip.putNextEntry(new ZipEntry(relativePath));
                FileInputStream in = new FileInputStream(f);
                int len;
                while((len=in.read(buf))>0)
                    zip.write(buf,0,len);
                in.close();
                zip.closeEntry();
            }
            entriesWritten++;
        }

        public void close() throws IOException {
            zip.close();
        }

        // Bitmask indicating directories in 'external attributes' of a ZIP archive entry.
        private static final long BITMASK_IS_DIRECTORY = 1<<4;
    }

    /**
     * Writes files in 'this' directory to a tar stream.
     *
     * @param glob
     *      Ant file pattern mask, like "**&#x2F;*.java".
     */
    public int tar(OutputStream out, final String glob) throws IOException, InterruptedException {
        return archive(new TarArchiverFactory(), out, glob);
    }

    public int tar(OutputStream out, FileFilter filter) throws IOException, InterruptedException {
        return archive(new TarArchiverFactory(), out, filter);
    }

    /**
     * Writes to a tar stream and stores obtained files to the base dir.
     *
     * @return
     *      number of files/directories that are written.
     */
    private Integer writeToTar(File baseDir, String fileMask, String excludes, OutputStream out) throws IOException {
        TarWriter tw = new TarWriter(out);
        try {
            new DirScanner.Glob(fileMask,excludes).scan(baseDir,tw);
        } finally {
            tw.close();
        }
        return tw.entriesWritten;
    }

    /**
     * Reads from a tar stream and stores obtained files to the base dir.
     */
    private static void readFromTar(String name, File baseDir, InputStream in) throws IOException {
        TarInputStream t = new TarInputStream(in);
        try {
            TarEntry te;
            while ((te = t.getNextEntry()) != null) {
                File f = new File(baseDir,te.getName());
                if(te.isDirectory()) {
                    f.mkdirs();
                } else {
                    File parent = f.getParentFile();
                    if (parent != null) parent.mkdirs();

                    OutputStream fos = new FileOutputStream(f);
                    try {
                        IOUtils.copy(t,fos);
                    } finally {
                        fos.close();
                    }
                    f.setLastModified(te.getModTime().getTime());
//                    int mode = te.getMode()&0777;
//                    if(mode!=0 && !Hudson.isWindows()) // be defensive
//                        try {
//                            LIBC.chmod(f.getPath(),mode);
//                        } catch (NoClassDefFoundError e) {
//                            // be defensive. see http://www.nabble.com/-3.0.6--Site-copy-problem%3A-hudson.util.IOException2%3A--java.lang.NoClassDefFoundError%3A-Could-not-initialize-class--hudson.util.jna.GNUCLibrary-td23588879.html
//                        }
                }
            }
        } catch(IOException e) {
            throw new IOException2("Failed to extract "+name,e);
        } finally {
            t.close();
        }
    }

//    /**
//     * Creates a {@link Launcher} for starting processes on the node
//     * that has this file.
//     * @since 1.89
//     */
//    public Launcher createLauncher(TaskListener listener) throws IOException, InterruptedException {
//        if(channel==null)
//            return new LocalLauncher(listener);
//        else
//            return new RemoteLauncher(listener,channel,channel.call(new IsUnix()));
//    }

    private static final class IsUnix implements Callable<Boolean,IOException> {
        public Boolean call() throws IOException {
            return File.pathSeparatorChar==':';
        }
        private static final long serialVersionUID = 1L;
    }

    @Deprecated @Override
    public String toString() {
        // to make writing JSPs easily, return local
        return remote;
    }

    public VirtualChannel getChannel() {
        if(channel!=null)   return channel;
        else                return DEFAULT_LOCAL_CHANNEL;
    }

    /**
     * Returns true if this {@link FilePath} represents a remote file.
     */
    public boolean isRemote() {
        return channel!=null;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        Channel target = Channel.current();

        if(channel!=null && channel!=target)
            throw new IllegalStateException("Can't send a remote FilePath to a different remote channel");

        oos.defaultWriteObject();
        oos.writeBoolean(channel==null);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        Channel channel = Channel.current();
        assert channel!=null;

        ois.defaultReadObject();
        if(ois.readBoolean()) {
            this.channel = channel;
        } else {
            this.channel = null;
        }
    }

    private static final long serialVersionUID = 1L;

    public static int SIDE_BUFFER_SIZE = 1024;

    /**
     * Adapts {@link FileCallable} to {@link Callable}.
     */
    private class FileCallableWrapper<T> implements DelegatingCallable<T,IOException> {
        private final FileCallable<T> callable;
        private transient ClassLoader classLoader;

        public FileCallableWrapper(FileCallable<T> callable) {
            this.callable = callable;
            this.classLoader = callable.getClass().getClassLoader();
        }

        private FileCallableWrapper(FileCallable<T> callable, ClassLoader classLoader) {
            this.callable = callable;
            this.classLoader = classLoader;
        }

        public T call() throws IOException {
            return callable.invoke(new File(remote), Channel.current());
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        private static final long serialVersionUID = 1L;
    }

    private static final Comparator<String> SHORTER_STRING_FIRST = new Comparator<String>() {
        public int compare(String o1, String o2) {
            return o1.length()-o2.length();
        }
    };

    private static final LocalChannel DEFAULT_LOCAL_CHANNEL = new LocalChannel(Executors.newCachedThreadPool());

    private static final class IOException2 extends IOException {
        private IOException2(String message, Throwable cause) {
            super(message);
            initCause(cause);
        }

        private IOException2(Throwable cause) {
            super(cause!=null?cause.toString():null);
            initCause(cause);
        }
    }

    private static boolean isWindows() {
        return File.pathSeparatorChar==';';
    }

    private static FileSet createFileSet(File baseDir, String includes, String excludes) {
        FileSet fs = new FileSet();
        fs.setDir(baseDir);
        fs.setProject(new Project());

        StringTokenizer tokens;

        tokens = new StringTokenizer(includes,",");
        while(tokens.hasMoreTokens()) {
            String token = tokens.nextToken().trim();
            fs.createInclude().setName(token);
        }
        if(excludes!=null) {
            tokens = new StringTokenizer(excludes,",");
            while(tokens.hasMoreTokens()) {
                String token = tokens.nextToken().trim();
                fs.createExclude().setName(token);
            }
        }
        return fs;
    }

    private static FileSet createFileSet(File baseDir, String includes) {
        return createFileSet(baseDir,includes,null);
    }
}
