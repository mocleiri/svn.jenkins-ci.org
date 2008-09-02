package org.jvnet.hudson.update_center;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Cache of the download plugin files.
 * 
 * @author Kohsuke Kawaguchi
 */
public class Cache {
    private final Properties metadata = new Properties();
    private final File dir;

    public Cache(File dir) throws IOException {
        this.dir = dir;
        dir.mkdirs();

        File cacheMetadata = getMetadataFile();
        if(cacheMetadata.exists())
            metadata.load(new FileInputStream(cacheMetadata));
    }

    private File getMetadataFile() {
        return new File(dir,"metadata.properties");
    }

    public File obtain(Plugin p) throws IOException {
        File f = new File(dir, p.artifactId);

        if(p.file.version.toString().equals(metadata.get(p.artifactId))) {
            if(f.exists())
                return f;   // cache hit
        }

        System.out.println("Downloading "+p.file.file.getURL());
        p.file.downloadTo(f);
        metadata.put(p.artifactId,p.file.version.toString());
        return f;
    }

    public void save() throws IOException {
        metadata.store(new FileOutputStream(getMetadataFile()),null);
    }
}
