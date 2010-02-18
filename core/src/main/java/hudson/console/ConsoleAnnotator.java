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

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.MarkupText;
import hudson.model.Hudson;
import org.jvnet.tiger_types.Types;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class ConsoleAnnotator<T> implements ExtensionPoint, Serializable {
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
     * For which context type does this annotator work?
     */
    public Class type() {
        Type type = Types.getBaseClass(getClass(), ConsoleAnnotator.class);
        if (type instanceof ParameterizedType)
            return Types.erasure(Types.getTypeArgument(type,0));
        else
            return Object.class;
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
    public static <T> ConsoleAnnotator<T> initial(Class<T> contextType) {
        return combine(_for(contextType));
    }

    /**
     * All the registered instances.
     */
    public static ExtensionList<ConsoleAnnotator> all() {
        return Hudson.getInstance().getExtensionList(ConsoleAnnotator.class);
    }

    /**
     * List all the console annotators that can work for the specified context type.
     */
    public static <T> List<ConsoleAnnotator<T>> _for(Class<T> contextType) {
        List<ConsoleAnnotator<T>> r  = new ArrayList<ConsoleAnnotator<T>>();
        for (ConsoleAnnotator ca : all()) {
            if (ca.type().isAssignableFrom(contextType))
                r.add(ca);
        }
        return r;
    }

    private static final long serialVersionUID = 1L;
}
