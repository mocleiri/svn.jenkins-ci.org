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
package hudson.model;

import hudson.Extension;
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
    public abstract ConsoleAnnotator annotate(Run<?,?> build, MarkupText text );

    /**
     * Bundles all the given {@link ConsoleAnnotator} into a single annotator.
     */
    public static ConsoleAnnotator combine(final Collection<? extends ConsoleAnnotator> all) {
        class Aggregator extends ConsoleAnnotator {
            List<ConsoleAnnotator> list = new ArrayList<ConsoleAnnotator>(all);

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

    /**
     * {@link ConsoleAnnotator} for testing this feature, during the development.
     * To be removed when this branch is merged to the trunk.
     */
    @Extension
    public static class DemoConsoleAnnotator extends ConsoleAnnotator {
        public ConsoleAnnotator annotate(Run<?, ?> build, MarkupText text) {
            if (text.length()>2)
                text.addMarkup(0,2,"<font color=red>","</font>");
            return this;
        }
    }
}
