package hudson.console;

import org.apache.commons.io.output.CountingOutputStream;
import org.jvnet.hudson.BlobTreeWriter;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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
        w = new BlobTreeWriter(new File(_for.getPath()+".annotations"));
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
}
