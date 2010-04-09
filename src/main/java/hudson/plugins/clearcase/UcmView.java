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

import hudson.plugins.clearcase.ucm.UcmCommon;
import hudson.plugins.clearcase.ucm.UcmCommon.Baseline;

import java.util.Collections;
import java.util.List;

/**
 * A Clearcase UCM View.
 * 
 * @author vlatombe
 * 
 */
public class UcmView extends View {
    private final String streamSelector;
    private final List<UcmCommon.Baseline> baselines;

    public UcmView(String name, String path, String configSpec, String streamSelector, List<Baseline> baselines) {
        super(name, path, configSpec);
        this.streamSelector = streamSelector;
        if (baselines != null) {
            this.baselines = Collections.unmodifiableList(baselines);
        } else {
            this.baselines = null;
        }
    }

    public List<UcmCommon.Baseline> getBaselines() {
        return baselines;
    }

    public String getStreamSelector() {
        return streamSelector;
    }
    
    @Override
    public String toString() {
        return "UcmView [baselines=" + baselines + ", streamSelector="
                + streamSelector + "]";
    }

}
