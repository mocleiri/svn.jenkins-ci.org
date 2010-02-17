/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc.
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
package hudson.console;

import hudson.model.Hudson;
import hudson.remoting.ObjectInputStreamEx;
import hudson.util.IOException2;
import org.apache.commons.io.output.CountingOutputStream;
import org.jvnet.hudson.Blob;
import org.jvnet.hudson.BlobTreeReader;
import org.jvnet.hudson.BlobTreeWriter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Stores {@link ConsoleAnnotation}
 *
 * @author Kohsuke Kawaguchi
 */
public class FileAnnotationStore implements AnnotationStore {
    private final BlobTreeWriter w;
    private CountingOutputStream counter;

    /**
     * @param _for
     *      The file for which annotations are attached.
     */
    public FileAnnotationStore(File _for) throws IOException {
        w = new BlobTreeWriter(getAnnotationFileName(_for));
    }

    public OutputStream hook(OutputStream out) {
        if (counter!=null)
            throw new IllegalStateException();
        return counter = new CountingOutputStream(out);
    }

    public void close() throws IOException {
        w.close();
    }

    public void add(ConsoleAnnotation a) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(w.writeNext(counter.getByteCount())));
        oos.writeObject(a);
        oos.close();
    }

    public static BlobTreeReader read(File _for) throws IOException {
        try {
            return new BlobTreeReader(getAnnotationFileName(_for));
        } catch (IOException e) {
            // the file doesn't exist, which can happen if the log file is written by older Hudson
            return null;
        }
    }

    public static ConsoleAnnotation readBlobFrom(Blob blob) throws IOException {
        try {
            ObjectInputStream ois = new ObjectInputStreamEx(
                    new GZIPInputStream(new ByteArrayInputStream(blob.payload)),
                    Hudson.getInstance().pluginManager.uberClassLoader
            );
            return (ConsoleAnnotation)ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException2(e);
        }
    }

    private static File getAnnotationFileName(File _for) {
        return new File(_for.getPath()+".annotations");
    }
}
