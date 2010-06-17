/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
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
package hudson.plugins.clearcase.action;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.ClearCaseDataAction;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ucm.UcmCommon;

import java.io.IOException;

/**
 * Check out action for dynamic views. This will not update any files from the repository as it is a dynamic view. It
 * only makes sure the view is started as config specs don't exist in UCM
 */
public class UcmDynamicCheckoutAction implements CheckOutAction {

    private ClearTool cleartool;
    private String stream;
    private boolean createDynView;
    private AbstractBuild<?, ?> build;
    private boolean freezeCode;

    public UcmDynamicCheckoutAction(final ClearTool cleartool, final String stream, final boolean createDynView, final AbstractBuild<?, ?> build, final boolean freezeCode) {
        super();
        this.cleartool = cleartool;
        this.stream = stream;
        this.createDynView = createDynView;
        this.build = build;
        this.freezeCode = freezeCode;
    }

    public boolean checkout(final Launcher launcher, final FilePath workspace, final String viewName) throws IOException, InterruptedException {
        // add stream to data action (to be used by ClearCase report)
        ClearCaseDataAction dataAction = build.getAction(ClearCaseDataAction.class);
        if (dataAction != null) {
            // sync the project in order to allow other builds to safely check if there is
            // already a build running on the same stream
            synchronized (build.getProject()) {
                dataAction.setStream(stream);
            }
        }
        if (createDynView) {
            if (freezeCode) {
                UcmCommon.checkoutCodeFreeze(cleartool, build, viewName, stream);
            } else {
                cleartool.mountVobs();
                cleartool.prepareView(viewName, stream);
                cleartool.syncronizeViewWithStream(viewName, stream);
            }
        } else {
            cleartool.startView(viewName);
            cleartool.syncronizeViewWithStream(viewName, stream);
        }
        return true;
    }
}
