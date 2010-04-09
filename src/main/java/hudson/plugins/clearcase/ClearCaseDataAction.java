/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer, Vincent Latombe
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
package hudson.plugins.clearcase;

import hudson.model.Action;
import hudson.plugins.clearcase.ucm.UcmCommon;

import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class ClearCaseDataAction implements Action {

    @Exported(visibility = 3)
    public List<UcmCommon.Baseline> latestBlsOnConfiguredStream;

    @Exported(visibility = 3)
    public String configSpec;

    @Exported(visibility = 3)
    public String streamSelector;

    public ClearCaseDataAction() {
        super();
    }

    public ClearCaseDataAction(View view) {
        if (view != null) {
            configSpec = view.getConfigSpec();
            if (view instanceof UcmView) {
                UcmView ucmView = (UcmView) view;
                streamSelector = ucmView.getStreamSelector();
            }
        }
    }

    public String getConfigSpec() {
        return configSpec;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    public List<UcmCommon.Baseline> getLatestBlsOnConfiguredStream() {
        return latestBlsOnConfiguredStream;
    }

    public String getStreamSelector() {
        return streamSelector;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    public boolean hasBaselinesInformation() {
        return latestBlsOnConfiguredStream != null && !latestBlsOnConfiguredStream.isEmpty();
    }
    
    public void setConfigSpec(String configSpec) {
        this.configSpec = configSpec;
    }

    public void setLatestBlsOnConfiguredStream(
            List<UcmCommon.Baseline> latestBlsOnConfiguredStream) {
        this.latestBlsOnConfiguredStream = latestBlsOnConfiguredStream;
    }

    public void setStreamSelector(String streamSelector) {
        this.streamSelector = streamSelector;
    }

}
