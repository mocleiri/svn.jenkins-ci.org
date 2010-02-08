package hudson.model;

import hudson.ExtensionPoint;
import hudson.MarkupText;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class ConsoleAnnotator implements ExtensionPoint {
    /**
     * Annotates one line.
     */
    public abstract ConsoleAnnotator annotate(AbstractBuild<?,?> build, MarkupText text );
}
