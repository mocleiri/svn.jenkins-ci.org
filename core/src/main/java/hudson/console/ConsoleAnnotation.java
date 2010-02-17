package hudson.console;

import hudson.MarkupText;
import hudson.model.Run;

import java.io.Serializable;

/**
 *
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ConsoleAnnotation implements Serializable {
    // TODO: if ConsoleAnnotator is just for build output, how does this work with other kinds of console output?
    public abstract ConsoleAnnotator createAnnotator(int charPos);

    private static final long serialVersionUID = 1L;

    public static class Demo extends ConsoleAnnotation {
        public ConsoleAnnotator createAnnotator(final int charPos) {
            return new ConsoleAnnotator() {
                public ConsoleAnnotator annotate(Run<?, ?> build, MarkupText text) {
                    text.addMarkup(charPos,charPos+1,"<a href=www.google.com>","</a>");
                    return null;
                }
            };
        }

        private static final long serialVersionUID = 1L;
    }
}
