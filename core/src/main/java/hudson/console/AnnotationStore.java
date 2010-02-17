package hudson.console;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public interface AnnotationStore extends Closeable {
    void add(ConsoleAnnotation a) throws IOException;
}
