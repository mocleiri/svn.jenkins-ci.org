package org.jvnet.hudson.update_center;

import org.kohsuke.jnt.JNFile;
import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;
import org.kohsuke.jnt.JNProject;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) throws ProcessingException, IOException {
        JavaNet jn = JavaNet.connect();
        JNProject p = jn.getProject("hudson");
        JNFileFolder plugins = p.getFolder("/plugins");
        for( JNFileFolder dir : plugins.getSubFolders().values() ) {
            System.out.println(dir.getName());

            VersionedFile latest = findLatest(dir);

            System.out.println("=> "+latest);

            if(latest!=null)
                latest.writeTo(dir.getName());
        }

        JNFileFolder release = p.getFolder("/releases");
        VersionedFile latest = findLatest(release);
        System.out.println("core\n=> "+latest);
        latest.writeTo("core");
    }

    private static VersionNumber parseVersion(JNFile file) {
        String n = file.getName();
        if(n.contains(" "))  n = n.substring(n.lastIndexOf(' ')+1);
        VersionNumber vn = new VersionNumber(n);
        return vn;
    }

    private static VersionedFile findLatest(JNFileFolder dir) throws ProcessingException {
        VersionNumber latestVer=null;
        JNFile latest=null;

        for( JNFile file : dir.getFiles().values() ) {
            try {
                VersionNumber vn = parseVersion(file);
                if(latestVer==null || vn.compareTo(latestVer)>0) {
                    latestVer = vn;
                    latest = file;
                }
            } catch (IllegalArgumentException e) {
                System.out.println("   Ignoring "+file.getName());
            }
        }

        if(latest==null)    return null;
        return new VersionedFile(latest,latestVer);
    }
}
