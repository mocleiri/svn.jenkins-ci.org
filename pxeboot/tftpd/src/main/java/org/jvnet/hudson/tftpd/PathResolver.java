package org.jvnet.hudson.tftpd;

import java.io.IOException;

/**
 * The file system view of TFTP server.
 *
 * <p>
 * This abstracts away the actual data storage. 
 *
 * @author Kohsuke Kawaguchi
 */
public interface PathResolver {
    /**
     * If a file name points to a resource, return it.
     *
     * @throws IOException
     *      If no such file exists.
     */
    Data open(String fileName) throws IOException;
}
