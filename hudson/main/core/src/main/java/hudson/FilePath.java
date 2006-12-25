package hudson;

import hudson.remoting.Callable;
import hudson.remoting.Callable.Void;
import hudson.remoting.Channel;
import hudson.remoting.Pipe;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.VirtualChannel;
import hudson.util.IOException2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.FileFilter;
import java.util.List;
import java.util.ArrayList;

import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;

/**
 * {@link File} like path-manipulation object with remoting capability.
 *
 * <p>
 * Unlike {@link File}, which always implies a file path on the current computer,
 * {@link FilePath} represents a file path on a specific slave or the master.
 *
 * <p>
 * When file operations are invoked against a file on a remote node, {@link FilePath}
 * executes the code remotely, thereby providing semi-transparent file operations.
 *
 * <p>
 * {@link FilePath} can be sent over to a remote node as a part of {@link Callable}
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

    public FilePath(VirtualChannel channel, String remote) {
        if(channel==null)
            throw new IllegalArgumentException();
        this.channel = channel;
        this.remote = remote;
    }

    /**
     * To create {@link FilePath} on the master computer.
     */
    public FilePath(File localPath) {
        this.channel = null;
        this.remote = localPath.toString();
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
        // Windows can handle '/' as a path separator but Unix can't,
        // so err on Unix side
        return remote.indexOf("\\")==-1;
    }

    public String getRemote() {
        return remote;
    }

    /**
     * Code that gets executed on the machine where the {@link FilePath} is local.
     * Used to act on {@link FilePath}.
     */
    public abstract class FileCallable<T> implements Callable<T,IOException> {
        public final T call() throws IOException {
            return invoke(new File(remote));
        }

        /**
         * Performs the task
         *
         * @param f
         *      {@link File} that represents the local file that {@link FilePath} represents.
         */
        protected abstract T invoke(File f) throws IOException;
    }

    /**
     * Executes some program on the machine that this {@link FilePath} exists,
     * so that one can perform local file operations.
     */
    public <T extends Serializable> T act(FileCallable<T> callable) throws IOException, InterruptedException {
        if(channel!=null) {
            // run this on a remote system
            return channel.call(callable);
        } else {
            // the file is on the local machine
            return callable.call();
        }
    }

    /**
     * Creates this directory.
     */
    public void mkdirs() throws IOException, InterruptedException {
        if(act(new FileCallable<Boolean>() {
            protected Boolean invoke(File f) throws IOException {
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
            protected Void invoke(File f) throws IOException {
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
            protected Void invoke(File f) throws IOException {
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
        int len = remote.length()-1;
        while(len>=0) {
            char ch = remote.charAt(len);
            if(ch=='\\' || ch=='/')
                break;
            len--;
        }

        return remote.substring(len+1);
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
            return new FilePath(this,channel.call(new Callable<String,IOException>() {
                public String call() throws IOException {
                    File f = File.createTempFile(prefix, suffix, new File(remote));
                    return f.getName();
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
            protected Boolean invoke(File f) throws IOException {
                return f.delete();
            }
        });
    }

    /**
     * Checks if the file exists.
     */
    public boolean exists() throws IOException, InterruptedException {
        return act(new FileCallable<Boolean>() {
            protected Boolean invoke(File f) throws IOException {
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
            protected Long invoke(File f) throws IOException {
                return f.lastModified();
            }
        });
    }

    /**
     * Checks if the file is a directory.
     */
    public boolean isDirectory() throws IOException, InterruptedException {
        return act(new FileCallable<Boolean>() {
            protected Boolean invoke(File f) throws IOException {
                return f.isDirectory();
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
        return act(new FileCallable<ArrayList<FilePath>>() {
            protected ArrayList<FilePath> invoke(File f) throws IOException {
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
     * Reads this file.
     */
    public InputStream read() throws IOException {
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
     */
    public OutputStream write() throws IOException {
        final Pipe p = Pipe.createLocalToRemote();

        channel.callAsync(new Callable<Void,IOException>() {
            public Void call() throws IOException {
                FileOutputStream fos = new FileOutputStream(new File(remote));
                Util.copyStream(p.getIn(),fos);
                fos.close();
                p.getIn().close();
                return null;
            }
        });

        return p.getOut();
    }

    /**
     * Sends the contents of this file into the given {@link OutputStream}.
     */
    public void copyTo(OutputStream os) throws IOException, InterruptedException {
        final OutputStream out = new RemoteOutputStream(os);

        channel.call(new Callable<Void,IOException>() {
            public Void call() throws IOException {
                FileInputStream fis = new FileInputStream(new File(remote));
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
        void open(String fileName) throws IOException;
        void write(byte[] buf, int len) throws IOException;
        void close() throws IOException;
    }

    /**
     * Copies the files that match the given file mask to the specified target node.
     */
    public void copyRecursiveTo(final String fileMask, final FilePath target) throws IOException, InterruptedException {
        if(this.channel==target.channel) {
            // local to local copy.
            act(new FileCallable<Void>() {
                protected Void invoke(File base) throws IOException {
                    assert target.channel==null;

                    try {
                        Copy copyTask = new Copy();
                        copyTask.setProject(new org.apache.tools.ant.Project());
                        copyTask.setTodir(new File(target.remote));
                        FileSet src = new FileSet();
                        src.setDir(base);
                        src.setIncludes(fileMask);
                        copyTask.addFileset(src);

                        copyTask.execute();
                        return null;
                    } catch (BuildException e) {
                        throw new IOException2("Failed to copy "+base+"/"+fileMask+" to "+target,e);
                    }
                }
            });
        } else {
            // remote copy
            final FilePath src = this;

            target.act(new FileCallable<Void>() {
                // this code is executed on the node that receives files.
                protected Void invoke(final File dest) throws IOException {
                    final RemoteCopier copier = Channel.current().export(
                        RemoteCopier.class,
                        new RemoteCopier() {
                            private OutputStream os;
                            public void open(String fileName) throws IOException {
                                File file = new File(dest, fileName);
                                file.getParentFile().mkdirs();
                                os = new FileOutputStream(file);
                            }

                            public void write(byte[] buf, int len) throws IOException {
                                os.write(buf,0,len);
                            }

                            public void close() throws IOException {
                                os.close();
                                os = null;
                            }
                        });

                    try {
                        src.act(new FileCallable<Void>() {
                            protected Void invoke(File base) throws IOException {
                                // copy to a remote node
                                FileSet fs = new FileSet();
                                fs.setDir(base);
                                fs.setIncludes(fileMask);

                                byte[] buf = new byte[8192];

                                DirectoryScanner ds = fs.getDirectoryScanner(new org.apache.tools.ant.Project());
                                for( String f : ds.getIncludedFiles() ) {
                                    File file = new File(base, f);

                                    copier.open(f);

                                    FileInputStream in = new FileInputStream(file);
                                    int len;
                                    while((len=in.read(buf))>=0)
                                        copier.write(buf,len);

                                    copier.close();
                                }
                                return null;
                            }
                        });
                    } catch (InterruptedException e) {
                        throw new IOException2("Copy operation interrupted",e);
                    }
                    return null;
                }
            });
        }
    }

    @Deprecated
    public String toString() {
        // to make writing JSPs easily, return local
        return remote;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        Channel target = Channel.current();

        if(channel!=null && channel!=target)
            throw new IllegalStateException("Can't send a remote FilePath to a different remote channel");

        oos.writeBoolean(channel==null);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        Channel channel = Channel.current();
        assert channel!=null;

        if(ois.readBoolean()) {
            this.channel = channel;
        } else {
            this.channel = null;
        }
    }

    private static final long serialVersionUID = 1L;
}
