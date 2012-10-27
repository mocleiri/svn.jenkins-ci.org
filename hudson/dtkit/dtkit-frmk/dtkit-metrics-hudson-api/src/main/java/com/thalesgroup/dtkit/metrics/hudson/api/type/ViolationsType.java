/*******************************************************************************
 * Copyright (c) 2011 Thales Corporate Services SAS                             *
 * Author : Gregory Boissinot                                                   *
 *                                                                              *
 * Permission is hereby granted, free of charge, to any person obtaining a copy *
 * of this software and associated documentation files (the "Software"), to deal*
 * in the Software without restriction, including without limitation the rights *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
 * copies of the Software, and to permit persons to whom the Software is        *
 * furnished to do so, subject to the following conditions:                     *
 *                                                                              *
 * The above copyright notice and this permission notice shall be included in   *
 * all copies or substantial portions of the Software.                          *
 *                                                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
 * THE SOFTWARE.                                                                *
 *******************************************************************************/

package com.thalesgroup.dtkit.metrics.hudson.api.type;

import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.ViolationsTypeDescriptor;
import com.thalesgroup.dtkit.metrics.model.InputMetric;
import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.model.Hudson;

@SuppressWarnings("unused")
public abstract class ViolationsType extends MetricsType implements Describable<ViolationsType> {

    protected ViolationsType(String pattern, boolean failIfNotNew, boolean deleteOutputFiles, boolean stopProcessingIfError) {
        super(pattern, failIfNotNew, deleteOutputFiles, stopProcessingIfError);
    }

    protected ViolationsType(String pattern, boolean failIfNotNew, boolean deleteOutputFiles) {
        super(pattern, failIfNotNew, deleteOutputFiles);
    }

    protected ViolationsType(String pattern) {
        super(pattern);
    }

    @SuppressWarnings("unused")
    public static ExtensionList<ViolationsType> all() {
        return Hudson.getInstance().getExtensionList(ViolationsType.class);
    }

    @SuppressWarnings("unchecked")
    public ViolationsTypeDescriptor<? extends ViolationsType> getDescriptor() {
        return (ViolationsTypeDescriptor<? extends ViolationsType>) Hudson.getInstance().getDescriptor(getClass());
    }

    @SuppressWarnings("unused")
    @Override
    public InputMetric getInputMetric() {
        return getDescriptor().getInputMetric();
    }
}
