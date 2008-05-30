package org.jvnet.hudson.update_center;

import org.kohsuke.jnt.JNFile;
import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.ProcessingException;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

import net.sf.json.JSONObject;

/**
 * @author Kohsuke Kawaguchi
 */
public class VersionedFile implements Comparable<VersionedFile> {
    final JNFile file;
    final VersionNumber version;

    public static VersionedFile findLatestFrom(JNFileFolder dir) throws ProcessingException {
        VersionedFile latest=null;

        for( JNFile file : dir.getFiles().values() ) {
            try {
                VersionedFile vf = new VersionedFile(file);
                if(latest==null || vf.compareTo(latest)>0) {
                    latest = vf;
                }
            } catch (IllegalArgumentException e) {
                System.out.println("   Ignoring "+file.getName());
            }
        }

        return latest;
    }

    public VersionedFile(JNFile file) {
        this.file = file;
        this.version = parseVersion(file);
    }

    private static VersionNumber parseVersion(JNFile file) {
        String n = file.getName();
        if(n.contains(" "))  n = n.substring(n.lastIndexOf(' ')+1);
        return new VersionNumber(n);
    }

    public JSONObject toJSON(String name) {
        JSONObject o = new JSONObject();
        o.put("name",name);
        o.put("version",version.toString());
        o.put("url",file.getURL().toExternalForm());
        return o;
    }

    public String toString() {
        return file.getName()+" ("+version+")";
    }

    public int compareTo(VersionedFile that) {
        return this.version.compareTo(that.version);
    }
}
