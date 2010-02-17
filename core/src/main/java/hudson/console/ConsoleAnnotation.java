package hudson.console;

import java.io.Serializable;

/**
 *
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ConsoleAnnotation implements Serializable {
    // TODO: if ConsoleAnnotator is just for build output, how does this work with other kinds of console output?
    public abstract ConsoleAnnotator createAnnotator(int charPos);
}
