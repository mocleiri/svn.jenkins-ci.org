package org.jvnet.hudson.update_center;

import org.kohsuke.jnt.JNFile;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

import net.sf.json.JSONObject;

/**
 * @author Kohsuke Kawaguchi
 */
public class VersionedFile {
    final JNFile file;
    final VersionNumber version;

    public VersionedFile(JNFile file, VersionNumber version) {
        this.file = file;
        this.version = version;
    }

    public JSONObject toJSON(String name) {
        JSONObject o = new JSONObject();
        o.put("name",name);
        o.put("version",version.toString());
        o.put("url",file.getURL().toExternalForm());
        return o;
    }

    public void writeTo(String name) throws IOException {
        PrintWriter w = new PrintWriter(new FileWriter(new File(name+".js")));
        w.printf("updateCenter('%s','%s');\n",name,version);
        w.close();
    }

    public String toString() {
        return file.getName()+" ("+version+")";
    }
}
