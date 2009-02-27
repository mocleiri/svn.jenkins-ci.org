/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc.
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
package hudson.os.solaris;

import com.sun.akuma.Daemon;
import com.sun.akuma.JavaVMArguments;
import com.sun.solaris.EmbeddedSu;
import hudson.FilePath;
import hudson.Launcher.LocalLauncher;
import hudson.Util;
import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.Launcher;
import hudson.remoting.Which;
import hudson.util.ForkOutputStream;
import hudson.util.HudsonIsRestarting;
import hudson.util.StreamTaskListener;
import static hudson.util.jna.GNUCLibrary.*;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jvnet.libpam.impl.CLibrary.passwd;
import org.jvnet.solaris.libzfs.ACLBuilder;
import org.jvnet.solaris.libzfs.LibZFS;
import org.jvnet.solaris.libzfs.ZFSException;
import org.jvnet.solaris.libzfs.ZFSFileSystem;
import org.jvnet.solaris.libzfs.ZFSPool;
import org.jvnet.solaris.libzfs.ErrorCode;
import org.jvnet.solaris.mount.MountFlags;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encourages the user to migrate HUDSON_HOME on a ZFS file system. 
 *
 * @author Kohsuke Kawaguchi
 * @since 1.283
 */
public class ZFSInstaller extends AdministrativeMonitor implements Serializable {
    /**
     * True if $HUDSON_HOME is a ZFS file system by itself.
     */
    private final boolean active = shouldBeActive();

    /**
     * This will be the file system name that we'll create.
     */
    private String prospectiveZfsFileSystemName;

    public boolean isActivated() {
        return active;
    }

    public boolean isRoot() {
        return LIBC.geteuid()==0;
    }

    public String getProspectiveZfsFileSystemName() {
        return prospectiveZfsFileSystemName;
    }

    private boolean shouldBeActive() {
        if(!System.getProperty("os.name").equals("SunOS"))
            // on systems that don't have ZFS, we don't need this monitor
            return false;

        try {
            LibZFS zfs = new LibZFS();
            List<ZFSPool> roots = zfs.roots();
            if(roots.isEmpty())
                return false;       // no active ZFS pool

            // if we don't run on a ZFS file system, activate
            ZFSFileSystem hudsonZfs = zfs.getFileSystemByMountPoint(Hudson.getInstance().getRootDir());
            if(hudsonZfs!=null)
                return false;       // already on ZFS

            // decide what file system we'll create
            ZFSPool pool = roots.get(0);
            prospectiveZfsFileSystemName = computeHudsonFileSystemName(zfs,pool);

            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to detect whether Hudson is on ZFS",e);
            return false;
        } catch (LinkageError e) {
            LOGGER.log(Level.WARNING, "No ZFS available",e);
            return false;
        }
    }

    /**
     * Called from the management screen.
     */
    public void doAct(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        requirePOST();
        Hudson.getInstance().checkPermission(Hudson.ADMINISTER);

        if(req.hasParameter("n")) {
            // we'll shut up
            disable(true);
            rsp.sendRedirect2(req.getContextPath()+"/manage");
            return;
        }

        rsp.sendRedirect2("confirm");
    }

    /**
     * Creates a ZFS file system to migrate the data to.
     *
     * <p>
     * This has to be done while we still have an interactive access with the user, since it involves the password.
     *
     * <p>
     * An exception will be thrown if the operation fails. A normal completion means a success.
     *
     * @return
     *      The ZFS dataset name to migrate the data to.
     */
    private String createZfsFileSystem(final TaskListener listener, String rootUsername, String rootPassword) throws IOException, InterruptedException, ZFSException {
        // capture the UID that Hudson runs under
        // so that we can allow this user to do everything on this new partition
        final int uid = LIBC.geteuid();
        final int gid = LIBC.getegid();
        passwd pwd = LIBC.getpwuid(uid);
        if(pwd==null)
            throw new IOException("Failed to obtain the current user information for "+uid);
        final String userName = pwd.pw_name;

        final File home = Hudson.getInstance().getRootDir();

        // this is the actual creation of the file system.
        // return true indicating a success
        Callable<String,IOException> task = new Callable<String,IOException>() {
            public String call() throws IOException {
                PrintStream out = listener.getLogger();

                LibZFS zfs = new LibZFS();
                ZFSFileSystem existing = zfs.getFileSystemByMountPoint(home);
                if(existing!=null) {
                    // no need for migration
                    out.println(home+" is already on ZFS. Doing nothing");
                    return existing.getName();
                }

                String name = computeHudsonFileSystemName(zfs, zfs.roots().get(0));
                out.println("Creating "+name);
                ZFSFileSystem hudson = zfs.create(name, ZFSFileSystem.class);

                // mount temporarily to set the owner right
                File dir = Util.createTempDir();
                hudson.setMountPoint(dir);
                hudson.mount();
                if(LIBC.chown(dir.getPath(),uid,gid)!=0)
                    throw new IOException("Failed to chown "+dir);
                hudson.unmount();

                try {
                    hudson.setProperty("hudson:managed-by","hudson"); // mark this file system as "managed by Hudson"

                    ACLBuilder acl = new ACLBuilder();
                    acl.user(userName).withEverything();
                    hudson.allow(acl);
                } catch (ZFSException e) {
                    // revert the file system creation
                    try {
                        hudson.destory();
                    } catch (Exception _) {
                        // but ignore the error and let the original error thrown
                    }
                    throw e;
                }
                return hudson.getName();
            }
        };


        // if we are the root user already, we can just do it here.
        // if that fails, no amount of pfexec and embedded_sudo would do.
        if(uid==0)
            return task.call();

        String javaExe = System.getProperty("java.home") + "/bin/java";
        String slaveJar = Which.jarFile(Launcher.class).getAbsolutePath();

        // otherwise first attempt pfexec, as that doesn't require password
        Channel channel;
        Process proc=null;

        if(rootPassword==null) {
            // try pfexec, in the hope that the user has the permission
            channel = new LocalLauncher(listener).launchChannel(
                    new String[]{"/usr/bin/pfexec", javaExe, "-jar", slaveJar},
                    listener.getLogger(), null, Collections.<String, String>emptyMap());
        } else {
            // try sudo with the given password. Also run in pfexec so that we can elevate the privileges
            listener.getLogger().println("Running with embedded_su");
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/pfexec",javaExe,"-jar",slaveJar);
            proc = EmbeddedSu.startWithSu(rootUsername, rootPassword, pb);
            channel = new Channel("zfs migration thread", Computer.threadPoolForRemoting,
                    proc.getInputStream(), proc.getOutputStream(), listener.getLogger());
        }

        try {
            return channel.call(task);
        } finally {
            channel.close();
            if(proc!=null)
                proc.destroy();
        }
    }

    /**
     * Called from the confirmation screen to actually initiate the migration.
     */
    public void doStart(StaplerRequest req, StaplerResponse rsp, @QueryParameter String username, @QueryParameter String password) throws ServletException, IOException {
        requirePOST(); 
        Hudson hudson = Hudson.getInstance();
        hudson.checkPermission(Hudson.ADMINISTER);

        final String datasetName;
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        StreamTaskListener listener = new StreamTaskListener(log);
        try {
            datasetName = createZfsFileSystem(listener,username,password);
        } catch (Exception e) {
            e.printStackTrace(listener.error(e.getMessage()));

            if (e instanceof ZFSException) {
                ZFSException ze = (ZFSException) e;
                if(ze.getCode()==ErrorCode.EZFS_PERM) {
                    // permission problem. ask the user to give us the root password
                    req.setAttribute("message",log.toString());
                    rsp.forward(this,"askRootPassword",req);
                    return;
                }
            }

            // for other kinds of problems, report and bail out
            req.setAttribute("pre",true);
            sendError(log.toString(),req,rsp);
            return;
        }

        // file system creation successful, so restart

        hudson.servletContext.setAttribute("app",new HudsonIsRestarting());
        // redirect the user to the manage page
        rsp.sendRedirect2(req.getContextPath()+"/manage");

        // asynchronously restart, so that we can give a bit of time to the browser to load "restarting..." screen.
        new Thread("restart thread") {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);

                    // close all descriptors on exec except stdin,out,err
                    int sz = LIBC.getdtablesize();
                    for(int i=3; i<sz; i++) {
                        int flags = LIBC.fcntl(i, F_GETFD);
                        if(flags<0) continue;
                        LIBC.fcntl(i, F_SETFD,flags| FD_CLOEXEC);
                    }

                    // re-exec with the system property to indicate where to migrate the data to.
                    // the 2nd phase is implemented in the migrate method.
                    JavaVMArguments args = JavaVMArguments.current();
                    args.setSystemProperty(ZFSInstaller.class.getName()+".migrate",datasetName);
                    Daemon.selfExec(args);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Restart failed",e);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Restart failed",e);
                }
            }
        }.start();
    }

    @Extension
    public static AdministrativeMonitor init() {
        String migrationTarget = System.getProperty(ZFSInstaller.class.getName() + ".migrate");
        if(migrationTarget!=null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            StreamTaskListener listener = new StreamTaskListener(new ForkOutputStream(System.out, out));
            try {
                if(migrate(listener,migrationTarget)) {
                    // completed successfully
                    return new MigrationCompleteNotice();
                }
            } catch (Exception e) {
                // if we let any exception from here, it will prevent Hudson from starting.
                e.printStackTrace(listener.error("Migration failed"));
            }
            // migration failed
            return new MigrationFailedNotice(out);
        }

        // install the monitor if applicable
        ZFSInstaller zi = new ZFSInstaller();
        if(zi.isActivated())
            return zi;

        return null;
    }

    /**
     * Migrates $HUDSON_HOME to a new ZFS file system.
     *
     * TODO: do this in a separate JVM to elevate the privilege.
     *
     * @param listener
     *      Log of migration goes here.
     * @param target
     *      Dataset to move the data to.
     * @return
     *      false if a migration failed.
     */
    private static boolean migrate(TaskListener listener, String target) throws IOException, InterruptedException {
        PrintStream out = listener.getLogger();

        File home = Hudson.getInstance().getRootDir();
        // do the migration
        LibZFS zfs = new LibZFS();
        ZFSFileSystem existing = zfs.getFileSystemByMountPoint(home);
        if(existing!=null) {
            out.println(home+" is already on ZFS. Doing nothing");
            return true;
        }

        File tmpDir = Util.createTempDir();

        // mount a new file system to a temporary location
        out.println("Opening "+target);
        ZFSFileSystem hudson = zfs.open(target, ZFSFileSystem.class);
        hudson.setMountPoint(tmpDir);
        hudson.setProperty("hudson:managed-by","hudson"); // mark this file system as "managed by Hudson"
        hudson.mount();

        // copy all the files
        out.println("Copying all existing data files");
        if(system(home,listener, "/usr/bin/cp","-pR",".", tmpDir.getAbsolutePath())!=0) {
            out.println("Failed to copy "+home+" to "+tmpDir);
            return false;
        }

        // unmount
        out.println("Unmounting "+target);
        hudson.unmount(MountFlags.MS_FORCE);

        // move the original directory to the side
        File backup = new File(home.getPath()+".backup");
        out.println("Moving "+home+" to "+backup);
        if(backup.exists())
            Util.deleteRecursive(backup);
        if(!home.renameTo(backup)) {
            out.println("Failed to move your current data "+home+" out of the way");
        }

        // update the mount point
        out.println("Creating a new mount point at "+home);
        if(!home.mkdir())
            throw new IOException("Failed to create mount point "+home);

        out.println("Mounting "+target);
        hudson.setMountPoint(home);
        hudson.mount();

        out.println("Sharing "+target);
        try {
            hudson.setProperty("sharesmb","on");
            hudson.setProperty("sharenfs","on");
            hudson.share();
        } catch (ZFSException e) {
            listener.error("Failed to share the file systems: "+e.getCode());
        }

        // delete back up
        out.println("Deleting "+backup);
        if(system(new File("/"),listener,"/usr/bin/rm","-rf",backup.getAbsolutePath())!=0) {
            out.println("Failed to delete "+home+" to "+tmpDir);
            return false;
        }

        out.println("Migration completed");
        return true;
    }

    private static int system(File pwd, TaskListener listener, String... args) throws IOException, InterruptedException {
        return new LocalLauncher(listener).launch(args, new String[0], System.out, new FilePath(pwd)).join();
    }

    private static String computeHudsonFileSystemName(LibZFS zfs, ZFSPool pool) {
        if(!zfs.exists(pool.getName()+"/hudson"))
            return pool.getName()+"/hudson";
        for( int i=2; ; i++ ) {
            String name = pool.getName() + "/hudson" + i;
            if(!zfs.exists(name))
                return name;
        }
    }

    /**
     * Used to indicate that the migration was completed successfully.
     */
    public static final class MigrationCompleteNotice extends AdministrativeMonitor {
        public boolean isActivated() {
            return true;
        }
    }

    /**
     * Used to indicate a failure in the migration.
     */
    public static final class MigrationFailedNotice extends AdministrativeMonitor {
        ByteArrayOutputStream record;

        MigrationFailedNotice(ByteArrayOutputStream record) {
            this.record = record;
        }

        public boolean isActivated() {
            return true;
        }
        
        public String getLog() {
            return record.toString();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ZFSInstaller.class.getName());
}
