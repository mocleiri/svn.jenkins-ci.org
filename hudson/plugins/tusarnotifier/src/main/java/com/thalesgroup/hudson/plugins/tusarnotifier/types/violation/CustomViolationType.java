/*******************************************************************************
 * Copyright (c) 2010 Thales Corporate Services SAS                             *
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

package com.thalesgroup.hudson.plugins.tusarnotifier.types.violation;

import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.ViolationsTypeDescriptor;
import com.thalesgroup.dtkit.metrics.hudson.api.type.ViolationsType;
import com.thalesgroup.dtkit.metrics.model.InputMetric;
import com.thalesgroup.dtkit.metrics.model.InputMetricException;
import com.thalesgroup.dtkit.metrics.model.InputMetricFactory;
import com.thalesgroup.hudson.plugins.tusarnotifier.types.CustomType;
import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

@SuppressWarnings("unused")
public class CustomViolationType extends ViolationsType implements CustomType {

    private static CustomViolationsTypeDescriptor DESCRIPTOR = new CustomViolationsTypeDescriptor();

    private String customXSL;

    @DataBoundConstructor
    @SuppressWarnings("unused")
    public CustomViolationType(String pattern, String customXSL, boolean faildedIfNotNew, boolean deleteOutputFiles) {
        super(pattern, faildedIfNotNew, deleteOutputFiles);
        this.customXSL = customXSL;
    }

    public ViolationsTypeDescriptor<? extends ViolationsType> getDescriptor() {
        return DESCRIPTOR;
    }

    @SuppressWarnings("unused")
    @Override
    public String getCustomXSL() {
        return customXSL;
    }

    @Extension
    public static class CustomViolationsTypeDescriptor extends ViolationsTypeDescriptor<CustomViolationType> {

        public CustomViolationsTypeDescriptor() {
            super(CustomViolationType.class, null);
        }

        @Override
        public String getId() {
            throw new UnsupportedOperationException("The descriptor registry with the called getId() method is not used. The descriptor redefines its own getInputMetric() method.");
        }

        @Override
        public InputMetric getInputMetric() {
            try {
                return InputMetricFactory.getInstance(CustomViolationInputMetric.class);
            } catch (InputMetricException e) {
                throw new RuntimeException("Can't create the inputMetric object for the class " + CustomViolationInputMetric.class);
            }
        }

        public boolean isCustomType() {
            return true;
        }
    }
}