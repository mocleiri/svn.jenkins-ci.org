package hudson;

import hudson.Launcher.LocalLauncher;
import hudson.Launcher.RemoteLauncher;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.DelegatingCallable;
import hudson.remoting.Future;
import hudson.remoting.Pipe;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.VirtualChannel;
import hudson.util.FormFieldValidator;
import hudson.util.IOException2;
import hudson.util.StreamResource;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Untar;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;

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
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

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
 * by using the same Java serializaiton scheme that the remoting module uses.
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
public final class FilePath implements Serializable {
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

    public FilePath(FilePath base, String rel) {
        this.channel = base.channel;
        if(base.isUnix()) {
            this.remote = base.remote+'/'+rel;
        } else {
            this.remote = base.remote+'\\'+rel;
        }
    }

    /**
     * Checks if the remote path is Unix.
     */
    private boolean isUnix() {
        // Windows absolute path always include ':', but this is not a valid char in Unix file systems.
        if(remote.contains(":"))    return false;
        // Windows can handle '/' as a path separator but Unix can't,
        // so err on Unix side
        return remote.indexOf("\\")==-1;
    }

    public String getRemote() {
        return remote;
    }

    /**
     * Creates a zip file from this directory or a file and sends that to the given output stream.
     */
    public void createZipArchive(OutputStream os) throws IOException, InterruptedException {
        final OutputStream out = (channel!=null)?new RemoteOutputStream(os):os;
        act(new FileCallable<Void>() {
            private transient byte[] buf;
            public Void invoke(File f, VirtualChannel channel) throws IOException {
                buf = new byte[8192];

                ZipOutputStream zip = new ZipOutputStream(out);
                scan(f,zip,"");
                zip.close();
                return null;
            }

            private void scan(File f, ZipOutputStream zip, String path) throws IOException {
                if(f.isDirectory()) {
                    for( File child : f.listFiles() )
                        scan(child,zip,path+f.getName()+'/');
                } else {
                    zip.putNextEntry(new ZipEntry(path+f.getName()));
                    FileInputStream in = new FileInputStream(f);
                    int len;
                    while((len=in.read(buf))>0)
                        zip.write(buf,0,len);
                    in.close();
                    zip.closeEntry();
                }
            }
            
            private static final long serialVersionUID = 1L;
        });
    }

    /**
     * Creates a zip file from this directory by only including the files that match the given glob.
     *
     * @param glob
     *      Ant style glob, like "**&#x2F;*.xml". If empty or null, this method
     *      works like {@link #createZipArchive(OutputStream)}
     *
     * @since 1.129
     */
    public void createZipArchive(OutputStream os, final String glob) throws IOException, InterruptedException {
        if(glob==null || glob.length()==0) {
            createZipArchive(os);
            return;
        }
        
        final OutputStream out = (channel!=null)?new RemoteOutputStream(os):os;
        act(new FileCallable<Void>() {
            public Void invoke(File dir, VirtualChannel channel) throws IOException {
                byte[] buf = new byte[8192];

                ZipOutputStream zip = new ZipOutputStream(out);
                for( String entry : glob(dir,glob) ) {
                    zip.putNextEntry(new ZipEntry(dir.getName()+'/'+entry));
                    FileInputStream in = new FileInputStream(new File(dir,entry));
                    int len;
                    while((len=in.read(buf))>0)
                        zip.write(buf,0,len);
                    in.close();
                    zip.closeEntry();
                }

                zip.close();
                return null;
            }

            private static final long serialVersionUID = 1L;
        });
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
        if(channel!=null) {
            // run this on a remote system
            try {
                return channel.call(new FileCallableWrapper<T>(callable));
            } catch (IOException e) {
                // wrap it into a new IOException so that we get the caller's stack trace as well.
                throw new IOException2("remote file operation failed",e);
            }
        } else {
            // the file is on the local machine.
            return callable.invoke(new File(remote), Hudson.MasterComputer.localChannel);
        }
    }

    /**
     * Executes some program on the machine that this {@link FilePath} exists,
     * so that one can perform local file operations.
     */
    public <T> Future<T> actAsync(final FileCallable<T> callable) throws IOException, InterruptedException {
        try {
            return (channel!=null ? channel : Hudson.MasterComputer.localChannel)
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
        if(act(new FileCallable<Boolean>() {
            public Boolean invoke(File f, VirtualChannel channel) throws IOException {
                return !f.mkdirs() && !f.exists();
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
                Util.deleteRecursive(f);
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
                Util.deleteContentsRecursive(f);
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
     * The same as {@code new FilePath(this,rel)} but more OO.
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
     * Creates a temporary file.
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
                        dir = null;
                    else
                        dir.mkdirs();
                    File f = File.createTempFile(prefix, suffix, dir);

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
     * Deletes this file.
     */
    public boolean delete() throws IOException, InterruptedException {
        return act(new FileCallable<Boolean>() {
            public Boolean invoke(File f, VirtualChannel channel) throws IOException {
                return f.delete();
            }
        });
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
     */
    public long lastModified() throws IOException, InterruptedException {
        return act(new FileCallable<Long>() {
            public Long invoke(File f, VirtualChannel channel) throws IOException {
                return f.lastModified();
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
     * List up files in this directory.
     *
     * @param filter
     *      The optional filter used to narrow down the result.
     *      If non-null, must be {@link Serializable}.
     *      If this {@link FilePath} represents a remote path,
     *      the filter object will be executed on the remote machine.
     */
    public List<FilePath> list(final FileFilter filter) throws IOException, InterruptedException {
        return act(new FileCallable<List<FilePath>>() {
            public List<FilePath> invoke(File f, VirtualChannel channel) throws IOException {
                File[] children = f.listFiles(filter);
                if(children ==null)     return null;

                ArrayList<FilePath> r = new ArrayList<FilePath>(children.length);
                for (File child : children)
                    r.add(new FilePath(child));

                return r;
            }
        });
    }

    /**
     * List up files in this directory that matches the given Ant-style filter.
     *
     * @param includes
     *      See {@link FileSet} for the syntax. String like "foo/*.zip".
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
     */
    private static String[] glob(File dir, String includes) {
        FileSet fs = new FileSet();
        fs.setDir(dir);
        fs.setIncludes(includes);

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
                FileInputStream fis = new FileInputStream(new File(remote));
                Util.copyStream(fis,p.getOut());
                fis.close();
                p.getOut().close();
                return null;
            }
        });

        return p.getIn();
    }

    /**
     * Writes to this file.
     * If this file already exists, it will be overwritten.
     * If the directory doesn't exist, it will be created.
     */
    public OutputStream write() throws IOException, InterruptedException {
        if(channel==null) {
            File f = new File(remote);
            f.getParentFile().mkdirs();
            return new FileOutputStream(f);
        }

        return channel.call(new Callable<OutputStream,IOException>() {
            public OutputStream call() throws IOException {
                File f = new File(remote);
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
        channel.call(new Callable<Void,IOException>() {
            public Void call() throws IOException {
                File f = new File(remote);
                f.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(f);
                try {
                    Writer w;
                    if(encoding!=null)
                    w = new OutputStreamWriter(fos, encoding);
                    else
                        w = new OutputStreamWriter(fos);
                    w.write(content);
                } finally {
                    fos.close();
                }
                return null;
            }
        });
    }

    /**
     * Computes the MD5 digest of the file in hex string.
     */
    public String digest() throws IOException, InterruptedException {
        return act(new FileCallable<String>() {
            public String invoke(File f, VirtualChannel channel) throws IOException {
                return Util.getDigestOf(new FileInputStream(f));
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
                FileInputStream fis = new FileInputStream(f);
                Util.copyStream(fis,out);
                fis.close();
                out.close();
                return null;
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

    public int copyRecursiveTo(String fileMask, FilePath target) throws IOException, InterruptedException {
        return copyRecursiveTo(fileMask,null,target);
    }

    /**
     * Copies the files that match the given file mask to the specified target node.
     *
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
                    assert target.channel==null;

                    try {
                        class CopyImpl extends Copy {
                            private int copySize;

                            public CopyImpl() {
                                setProject(new org.apache.tools.ant.Project());
                            }

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
                        FileSet src = new FileSet();
                        src.setDir(base);
                        src.setIncludes(fileMask);
                        src.setExcludes(excludes);
                        copyTask.addFileset(src);

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
                        readFromTar(remote+'/'+fileMask, f,pipe.getIn());
                        return null;
                    } finally {
                        pipe.getIn().close();
                    }
                }
            });
            int r = writeToTar(new File(remote),fileMask,excludes,pipe);
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
                        return writeToTar(f,fileMask,excludes,pipe);
                    } finally {
                        pipe.getOut().close();
                    }
                }
            });
            try {
                readFromTar(remote+'/'+fileMask,new File(target.remote),pipe.getIn());
            } catch (IOException e) {// BuildException or IOException
                try {
                    future.get(3,TimeUnit.SECONDS);
                    throw e;    // the remote side completed successfully, so the error must be local
                } catch (ExecutionException x) {
                    // report both errors
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    throw new IOException2(sw.toString(),x);
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

    /**
     * Writes to a tar stream and stores obtained files to the base dir.
     *
     * @return
     *      number of files/directories that are written.
     */
    private Integer writeToTar(File baseDir, String fileMask, String excludes, Pipe pipe) throws IOException {
        FileSet fs = new FileSet();
        fs.setDir(baseDir);
        fs.setIncludes(fileMask);
        if(excludes!=null)
            fs.setExcludes(excludes);

        byte[] buf = new byte[8192];

        TarOutputStream tar = new TarOutputStream(new GZIPOutputStream(new BufferedOutputStream(pipe.getOut())));
        tar.setLongFileMode(TarOutputStream.LONGFILE_GNU);
        DirectoryScanner ds = fs.getDirectoryScanner(new org.apache.tools.ant.Project());
        String[] files = ds.getIncludedFiles();
        for( String f : files) {
            if(Functions.isWindows())
                f = f.replace('\\','/');

            File file = new File(baseDir, f);

            TarEntry te = new TarEntry(f);
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
        }

        tar.close();

        return files.length;
    }

    /**
     * Reads from a tar stream and stores obtained files to the base dir.
     */
    private static void readFromTar(String name, File baseDir, InputStream in) throws IOException {
        Untar untar = new Untar();
        untar.setProject(new Project());
        untar.add(new StreamResource(name,new BufferedInputStream(new GZIPInputStream(in))));
        untar.setDest(baseDir);
        try {
            untar.execute();
        } catch (BuildException e) {
            throw new IOException2("Failed to read the remote stream "+name,e);
        }
    }

    /**
     * Creates a {@link Launcher} for starting processes on the node
     * that has this file.
     * @since 1.89
     */
    public Launcher createLauncher(TaskListener listener) {
        if(channel==null)
            return new LocalLauncher(listener);
        else
            return new RemoteLauncher(listener,channel,isUnix());
    }

    /**
     * Validates the ant file mask (like "foo/bar/*.txt, zot/*.jar")
     * against this directory, and try to point out the problem.
     *
     * <p>
     * This is useful in conjunction with {@link FormFieldValidator}.
     *
     * @return
     *      null if no error was found.
     * @since 1.90
     * @see FormFieldValidator.WorkspaceFileMask
     */
    public String validateAntFileMask(final String fileMasks) throws IOException, InterruptedException {
        return act(new FileCallable<String>() {
            public String invoke(File dir, VirtualChannel channel) throws IOException {
                StringTokenizer tokens = new StringTokenizer(fileMasks);

                OUTER:
                while(tokens.hasMoreTokens()) {
                    final String fileMask = tokens.nextToken().trim();
                    String previous = null;
                    String pattern = fileMask;

                    while(true) {
                        FileSet fs = new FileSet();
                        fs.setDir(dir);
                        fs.setIncludes(pattern);

                        DirectoryScanner ds = fs.getDirectoryScanner(new org.apache.tools.ant.Project());

                        if(ds.getIncludedFilesCount()!=0 || ds.getIncludedDirsCount()!=0) {
                            // found a match
                            if(pattern.equals(fileMask))
                                continue OUTER;    // no error
                            if(previous==null)
                                return String.format("'%s' doesn't match anything, although '%s' exists",
                                    fileMask, pattern );
                            else
                                return String.format("'%s' doesn't match anything: '%s' exists but not '%s'",
                                    fileMask, pattern, previous );
                        }

                        int idx = Math.max(pattern.lastIndexOf('\\'),pattern.lastIndexOf('/'));
                        if(idx<0) {
                            if(pattern.equals(fileMask))
                                return String.format("'%s' doesn't match anything", fileMask );
                            else
                                return String.format("'%s' doesn't match anything: even '%s' doesn't exist",
                                    fileMask, pattern );
                        }

                        // cut off the trailing component and try again
                        previous = pattern;
                        pattern = pattern.substring(0,idx);
                    }
                }

                return null; // no error
            }
        });
    }

    @Deprecated
    public String toString() {
        // to make writing JSPs easily, return local
        return remote;
    }

    public VirtualChannel getChannel() {
        if(channel!=null)   return channel;
        else                return Hudson.MasterComputer.localChannel;
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

    /**
     * Adapts {@link FileCallable} to {@link Callable}.
     */
    private class FileCallableWrapper<T> implements DelegatingCallable<T,IOException> {
        private final FileCallable<T> callable;

        public FileCallableWrapper(FileCallable<T> callable) {
            this.callable = callable;
        }

        public T call() throws IOException {
            return callable.invoke(new File(remote), Channel.current());
        }

        public ClassLoader getClassLoader() {
            return callable.getClass().getClassLoader();
        }

        private static final long serialVersionUID = 1L;
    }
}
