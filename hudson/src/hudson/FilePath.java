package hudson;

import java.io.File;
import java.io.IOException;

/**
 * {@link File} like path-manipulation object.
 *
 * <p>
 * In general, because programs could be executed remotely,
 * we need two path strings to identify the same directory.
 * One from a point of view of the master (local), the other
 * from a point of view of the slave (remote).
 *
 * <p>
 * This class allows path manipulation to be done
 * and allow the local/remote versions to be obtained
 * after the computation.
 *
 * @author Kohsuke Kawaguchi
 */
public final class FilePath {
    private final File local;
    private final String remote;

    public FilePath(File local, String remote) {
        this.local = local;
        this.remote = remote;
    }

    /**
     * Useful when there's no remote path.
     */
    public FilePath(File local) {
        this(local,local.getPath());
    }

    public FilePath(FilePath base, String rel) {
        this.local = new File(base.local,rel);
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

    public File getLocal() {
        return local;
    }

    public String getRemote() {
        return remote;
    }

    /**
     * Creates this directory.
     */
    public void mkdirs() throws IOException {
        if(!local.mkdirs())
            throw new IOException("Failed to mkdirs: "+local);
    }

    /**
     * Deletes all the contents of this directory, but not the directory itself
     */
    public void deleteContents() throws IOException {
        // TODO: consider doing this remotely if possible
        Util.deleteContentsRecursive(getLocal());
    }

    /**
     * The same as {@code new FilePath(this,rel)} but more OO.
     */
    public FilePath child(String rel) {
        return new FilePath(this,rel);
    }

    /**
     * Creates a temporary file.
     */
    public FilePath createTempFile(String prefix, String suffix) throws IOException {
        File f = File.createTempFile(prefix, suffix, getLocal());
        return new FilePath(this,f.getName());
    }

    /**
     * Deletes this file.
     */
    public boolean delete() {
       return local.delete();
    }

    /**
     * Always use {@link #getLocal()} or {@link #getRemote()}
     */
    @Deprecated
    public String toString() {
        // to make writing JSPs easily, return local
        return local.toString();
    }
}
