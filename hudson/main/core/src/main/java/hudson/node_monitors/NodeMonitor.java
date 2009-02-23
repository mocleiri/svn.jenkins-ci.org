/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Thomas J. Black
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
package hudson.node_monitors;

import hudson.ExtensionPoint;
import hudson.Functions;
import hudson.model.Computer;
import hudson.model.ComputerSet;
import hudson.model.Describable;
import hudson.model.Node;
import hudson.model.Hudson;
import hudson.util.DescriptorList;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Extension point for managing and monitoring {@link Node}s.
 *
 * <h2>Views</h2>
 * <dl>
 * <dt>column.jelly</dt>
 * <dd>
 * Invoked from {@link ComputerSet} <tt>index.jelly</tt> to render a column.
 * The {@link NodeMonitor} instance is accessible through the "from" variable.
 * Also see {@link #getColumnCaption()}.
 * </dl>
 *
 * @author Kohsuke Kawaguchi
 * @since 1.123
 */
@ExportedBean
public abstract class NodeMonitor implements ExtensionPoint, Describable<NodeMonitor> {
    /**
     * Returns the name of the column to be added to {@link ComputerSet} index.jelly.
     *
     * @return
     *      null to not render a column. The convention is to use capitalization like "Foo Bar Zot".
     */
    @Exported
    public String getColumnCaption() {
        return getDescriptor().getDisplayName();
    }

    public AbstractNodeMonitorDescriptor<?> getDescriptor() {
        return (AbstractNodeMonitorDescriptor<?>)Hudson.getInstance().getDescriptor(getClass());
    }

    public Object data(Computer c) {
        return getDescriptor().get(c);
    }

    /**
     * Starts updating the data asynchronously.
     * If there's any previous updating activity going on, it'll be interrupted and aborted.
     *
     * @return
     *      {@link Thread} object that carries out the update operation.
     *      You can use this to interrupt the execution or waits for the completion.
     *      Always non-null
     * @since 1.232
     */
    public Thread triggerUpdate() {
        return getDescriptor().triggerUpdate();
    }

    /**
     * Obtains all the instances of {@link NodeMonitor}s that are alive.
     * @since 1.187
     */
    public static List<NodeMonitor> getAll() {
        return ComputerSet.get_monitors();
    }

    /**
     * All registered {@link NodeMonitor}s.
     */
    public static final DescriptorList<NodeMonitor> LIST = new DescriptorList<NodeMonitor>(NodeMonitor.class);
}
