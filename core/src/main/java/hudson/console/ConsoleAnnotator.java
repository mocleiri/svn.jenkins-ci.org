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

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.MarkupText;
import hudson.model.Hudson;
import hudson.model.Run;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class ConsoleAnnotator implements ExtensionPoint, Serializable {
    /**
     * Annotates one line.
     */
    public abstract ConsoleAnnotator annotate(Run<?,?> build, MarkupText text );

    /**
     * Bundles all the given {@link ConsoleAnnotator} into a single annotator.
     */
    public static ConsoleAnnotator combine(Collection<? extends ConsoleAnnotator> all) {
        switch (all.size()) {
        case 0:     return null;    // none
        case 1:     return all.iterator().next(); // just one
        }

        class Aggregator extends ConsoleAnnotator {
            List<ConsoleAnnotator> list;

            Aggregator(Collection<? extends ConsoleAnnotator> list) {
                this.list = new ArrayList<ConsoleAnnotator>(list);
            }

            public ConsoleAnnotator annotate(Run<?, ?> build, MarkupText text) {
                ListIterator<ConsoleAnnotator> itr = list.listIterator();
                while (itr.hasNext()) {
                    ConsoleAnnotator a =  itr.next();
                    ConsoleAnnotator b = a.annotate(build,text);
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
    
    public static ConsoleAnnotator initial() {
        return combine(all());
    }

    /**
     * All the registered instances.
     */
    public static ExtensionList<ConsoleAnnotator> all() {
        return Hudson.getInstance().getExtensionList(ConsoleAnnotator.class);
    }

    private static final long serialVersionUID = 1L;
}
