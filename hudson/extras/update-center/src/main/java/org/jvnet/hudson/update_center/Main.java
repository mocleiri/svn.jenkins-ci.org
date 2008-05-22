package org.jvnet.hudson.update_center;

import org.kohsuke.jnt.JNFile;
import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;

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

            if(latest!=null)
                System.out.println("=> "+latest.toJSON(dir.getName()));
        }

        JNFileFolder release = p.getFolder("/releases");
        VersionedFile latest = findLatest(release);
        System.out.println("core\n=> "+latest.toJSON("core"));
    }

    private static VersionNumber parseVersion(JNFile file) {
        String n = file.getName();
        if(n.contains(" "))  n = n.substring(n.lastIndexOf(' ')+1);
        return new VersionNumber(n);
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
