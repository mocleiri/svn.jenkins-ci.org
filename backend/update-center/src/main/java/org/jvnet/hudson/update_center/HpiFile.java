package org.jvnet.hudson.update_center;

import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents an .hpi file.
 *
 * @author Kohsuke Kawaguchi
 */
public final class HpiFile {
    public final File file;
    public final Manifest manifest;
    public final Attributes attributes;

    public HpiFile(File file) throws IOException {
        this.file = file;

        JarFile j = new JarFile(file);
        manifest = j.getManifest();
        j.close();
        attributes = manifest.getMainAttributes();
    }

    public String getBuiltWithHudsonVersion() {
        return attributes.getValue("Hudson-Version");
    }

    public String getRequiredHudsonVersion() {
        return attributes.getValue("Required-Hudson-Version");
    }

    public String getCompatibleSinceVersion() {
        return attributes.getValue("Compatible-Since-Version");
    }

    public String getDisplayName() {
        return attributes.getValue("Long-Name");
    }

    public List<Dependency> getDependencies() {
        String deps = attributes.getValue("Plugin-Dependencies");
        if(deps==null)  return Collections.emptyList();

        List<Dependency> r = new ArrayList<Dependency>();
        for(String token : deps.split(","))
            r.add(new Dependency(token));
        return r;
    }

    public static class Dependency {
        public final String name;
        public final String version;
        public final boolean optional;

        Dependency(String token) {
            this.optional = token.endsWith(OPTIONAL);
            if(optional)
                token = token.substring(0, token.length()-OPTIONAL.length());

            String[] pieces = token.split(":");
            name = pieces[0];
            version = pieces[1];
        }

        private static final String OPTIONAL = ";resolution:=optional";

        public JSONObject toJSON() {
            JSONObject o = new JSONObject();
            o.put("name",name);
            o.put("version",version);
            o.put("optional",optional);
            return o;
        }
    }
}
