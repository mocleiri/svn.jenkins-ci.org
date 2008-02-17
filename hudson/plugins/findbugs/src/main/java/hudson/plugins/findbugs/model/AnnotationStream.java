package hudson.plugins.findbugs.model;

import hudson.util.StringConverter2;
import hudson.util.XStream2;

/**
 * An XStream for annotations.
 */
public class AnnotationStream extends XStream2 {
    /**
     * Creates a new instance of <code>AnnotationStream</code>.
     */
    public AnnotationStream() {
        super();

        alias("file", WorkspaceFile.class);
        alias("priority", Priority.class);
        registerConverter(new StringConverter2(), 100);
        registerConverter(new Priority.PriorityConverter(), 100);
        addImmutableType(Priority.class);
    }
}

