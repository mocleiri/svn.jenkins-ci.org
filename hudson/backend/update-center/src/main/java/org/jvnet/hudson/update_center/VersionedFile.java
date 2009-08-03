package org.jvnet.hudson.update_center;

import net.sf.json.JSONObject;
import org.kohsuke.jnt.JNFile;
import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.ProcessingException;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Represents {@link JNFile} that has the version number as the file name.
 *
 * @author Kohsuke Kawaguchi
 */
public class VersionedFile implements Comparable<VersionedFile> {
    final VersionNumber version;
    final URL url;

    public VersionedFile(URL url, VersionNumber version) {
        this.url = url;
        this.version = version;
    }

    public JSONObject toJSON(String name) {
        JSONObject o = new JSONObject();
        o.put("name",name);
        o.put("version",version.toString());
        o.put("url",url.toExternalForm());
        return o;
    }

    public String toString() {
        return url+" ("+version+")";
    }

    public int compareTo(VersionedFile that) {
        return this.version.compareTo(that.version);
    }

    public void downloadTo(File f) throws IOException {
        InputStream in = url.openStream();
        FileOutputStream out = new FileOutputStream(f);
        try {
            IOUtils.copyLarge(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }
}
