package hudson.model;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.MarkupText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class ConsoleAnnotator implements ExtensionPoint {
    /**
     * Annotates one line.
     */
    public abstract ConsoleAnnotator annotate(AbstractBuild<?,?> build, MarkupText text );

    /**
     * Bundles all the given {@link ConsoleAnnotator} into a single annotator.
     */
    public static ConsoleAnnotator combine(final Collection<? extends ConsoleAnnotator> all) {
        class Aggregator extends ConsoleAnnotator {
            List<ConsoleAnnotator> list = new ArrayList<ConsoleAnnotator>(all);

            @Override
            public ConsoleAnnotator annotate(AbstractBuild<?, ?> build, MarkupText text) {
                ListIterator<ConsoleAnnotator> itr = list.listIterator();
                while (itr.hasNext()) {
                    ConsoleAnnotator a =  itr.next();
                    ConsoleAnnotator b = a.annotate(build,text);
                    if (a!=b) {
                        if (b==null)    itr.remove();
                        else            itr.set(b);
                    }
                }
                return list.isEmpty() ? null : this;
            }
        }
        return new Aggregator();
    }
    
    public static ConsoleAnnotator initial() {
        return combine(all());
    }

    /**
     * All the registered instances.
     */
    public static ExtensionList<ConsoleAnnotator> all() {
        return Hudson.getInstance().getExtensionList(ConsoleAnnotator.class);
    }
}
