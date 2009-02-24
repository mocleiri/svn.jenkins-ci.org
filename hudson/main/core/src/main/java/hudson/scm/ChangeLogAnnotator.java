/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
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
package hudson.scm;

import hudson.ExtensionPoint;
import hudson.MarkupText;
import hudson.ExtensionListView;
import hudson.Extension;
import hudson.DescriptorExtensionList;
import hudson.ExtensionList;
import hudson.slaves.RetentionStrategy;
import hudson.util.CopyOnWriteList;
import hudson.scm.ChangeLogSet.Entry;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;

import java.util.logging.Logger;

/**
 * Performs mark up on changelog messages to be displayed.
 *
 * <p>
 * SCM changelog messages are usually plain text, but when we display that in Hudson,
 * it is often nice to be able to put mark up on the text (for example to link to
 * external issue tracking system.)
 *
 * <p>
 * Plugins that are interested in doing so may extend this class and put {@link Extension} on it.
 * When multiple annotators are registered, their results will be combined.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.70
 */
public abstract class ChangeLogAnnotator implements ExtensionPoint {
    /**
     * Called by Hudson to allow markups to be added to the changelog text.
     *
     * <p>
     * This method is invoked each time a page is rendered, so implementations
     * of this method should not take too long to execute. Also note that
     * this method may be invoked concurrently by multiple threads.
     *
     * <p>
     * If there's any error during the processing, it should be recorded in
     * {@link Logger} and the method should return normally.
     *
     * @param build
     *      Build that owns this changelog. From here you can access broader contextual
     *      information, like the project, or it settings. Never null.
     * @param change
     *      The changelog entry for which this method is adding markup.
     *      Never null.
     * @param text
     *      The text and markups. Implementation of this method is expected to
     *      add additional annotations into this object. If other annotators
     *      are registered, the object may already contain some markups when this
     *      method is invoked. Never null. {@link MarkupText#getText()} on this instance
     *      will return the same string as {@link Entry#getMsgEscaped()}.
     */
    public abstract void annotate(AbstractBuild<?,?> build, Entry change, MarkupText text );

    /**
     * Registers this annotator, so that Hudson starts using this object
     * for adding markup.
     *
     * @deprecated as of 1.286
     *      Prefer automatic registration via {@link Extension}
     */
    public final void register() {
        annotators.add(this);
    }

    /**
     * Unregisters this annotator, so that Hudson stops using this object.
     */
    public final boolean unregister() {
        return annotators.remove(this);
    }

    /**
     * All registered {@link ChangeLogAnnotator}s.
     *
     * @deprecated as of 1.286
     *      Use {@link #all()} for read access, and {@link Extension} for registration.
     */
    public static final CopyOnWriteList<ChangeLogAnnotator> annotators = ExtensionListView.createCopyOnWriteList(ChangeLogAnnotator.class);

    /**
     * Returns all the registered {@link ChangeLogAnnotator} descriptors.
     */
    public static ExtensionList<ChangeLogAnnotator> all() {
        return Hudson.getInstance().getExtensionList(ChangeLogAnnotator.class);
    }
}
