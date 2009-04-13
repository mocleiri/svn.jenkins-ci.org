package org.jvnet.hudson.tftpd;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class Data {
    /**
     * Reads this data.
     *
     * @throws IOException
     *      If it turns out that this data doesn't exist, or failed to read.
     */
    public abstract InputStream read() throws IOException;

    /**
     * Computes the size of the data.
     *
     * <p>
     * The default implementation isn't very efficient.
     */
    public int size() throws IOException {
        InputStream in = read();
        byte[] buf = new byte[4096];
        int size = 0;

        int chunk;
        while((chunk=in.read(buf))>=0)
            size+=chunk;

        return size;
    }

    /**
     * Creates a {@link Data} object from a URL.
     */
    public static Data fromURL(final URL url) {
        return new Data() {
            public InputStream read() throws IOException {
                return url.openStream();
            }

            public int size() throws IOException {
                int len = url.openConnection().getContentLength();
                if(len>=0)      return len;
                return super.size();
            }
        };
    }
}
