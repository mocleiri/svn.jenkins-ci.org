package test;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.jvnet.solaris.libzfs.LibZFS;
import org.jvnet.solaris.libzfs.ZFSFileSystem;
import org.jvnet.solaris.mount.MountFlags;

import java.io.File;
import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);

        LibZFS zfs = new LibZFS();
        ZFSFileSystem existing = zfs.getFileSystemByMountPoint(dir);
        if(existing!=null) {
            System.out.println("File system "+existing+" is already mounted here");
            return;
        }

        File tmpDir = createTmpDir();

        // mount a new file system to a temporary location
        ZFSFileSystem hudson = zfs.roots().get(0).createFileSystem("hudson", null);
        hudson.setMountPoint(tmpDir);
        hudson.mount();

        // copy all the files
        // TODO: better to do this with "cp -pr . /tmpdir from $HUDSON_HOME
        Copy cp = new Copy();
        cp.setProject(new Project());
        cp.setTodir(tmpDir);
        FileSet fs = new FileSet();
        fs.setDir(dir);
        cp.addFileset(fs);
        cp.execute();

        // unmount
        hudson.unmount(MountFlags.MS_FORCE);

        // move the original directory to the side
        File backup = new File(dir.getPath()+".backup");
        if(!dir.renameTo(backup))
            throw new IOException("Failed to move your current data "+dir+" out of the way");

        // update the mount point
        if(!dir.mkdir())
            throw new IOException("Failed to create mount point "+dir+"; your old data is left in "+backup);
        hudson.setMountPoint(dir);
        hudson.mount();
    }

    private static File createTmpDir() throws IOException {
        File tmpDir = File.createTempFile("zfs", "zfs");
        tmpDir.delete();
        tmpDir.mkdir();
        return tmpDir;
    }
}
