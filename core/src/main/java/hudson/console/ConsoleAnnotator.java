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

import hudson.MarkupText;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class ConsoleAnnotator<T> implements Serializable {
    /**
     * Annotates one line.
     */
    public abstract ConsoleAnnotator annotate(T context, MarkupText text );

    /**
     * Cast operation that restricts T.
     */
    public static <T> ConsoleAnnotator<T> cast(ConsoleAnnotator<? super T> a) {
        return (ConsoleAnnotator)a;
    }

    /**
     * Bundles all the given {@link ConsoleAnnotator} into a single annotator.
     */
    public static <T> ConsoleAnnotator<T> combine(Collection<? extends ConsoleAnnotator<? super T>> all) {
        switch (all.size()) {
        case 0:     return null;    // none
        case 1:     return  cast(all.iterator().next()); // just one
        }

        class Aggregator extends ConsoleAnnotator<T> {
            List<ConsoleAnnotator<T>> list;

            Aggregator(Collection list) {
                this.list = new ArrayList<ConsoleAnnotator<T>>(list);
            }

            public ConsoleAnnotator annotate(T context, MarkupText text) {
                ListIterator<ConsoleAnnotator<T>> itr = list.listIterator();
                while (itr.hasNext()) {
                    ConsoleAnnotator a =  itr.next();
                    ConsoleAnnotator b = a.annotate(context,text);
                    if (a!=b) {
                        if (b==null)    itr.remove();
                        else            itr.set(b);
                    }
                }

                switch (list.size()) {
                case 0:     return null;    // no more annotator left
                case 1:     return list.get(0); // no point in aggregating
                default:    return this;
                }
            }
        }
        return new Aggregator(all);
    }

    /**
     * Returns the all {@link ConsoleAnnotator}s for the given context type aggregated into a single
     * annotator.
     */
    public static <T> ConsoleAnnotator<T> initial(T context) {
        return combine(_for(context));
    }

    /**
     * List all the console annotators that can work for the specified context type.
     */
    public static <T> List<ConsoleAnnotator<T>> _for(T context) {
        List<ConsoleAnnotator<T>> r  = new ArrayList<ConsoleAnnotator<T>>();
        for (ConsoleAnnotatorFactory f : ConsoleAnnotatorFactory.all()) {
            if (f.type().isInstance(context)) {
                ConsoleAnnotator ca = f.newInstance(context);
                if (ca!=null)
                    r.add(ca);
            }
        }
        return r;
    }

    private static final long serialVersionUID = 1L;
}
