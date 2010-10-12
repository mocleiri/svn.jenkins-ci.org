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

package com.thalesgroup.dtkit.metrics.hudson.api.descriptor;

import com.thalesgroup.dtkit.metrics.model.InputMetric;
import com.thalesgroup.dtkit.metrics.model.InputMetricException;
import com.thalesgroup.dtkit.metrics.model.InputMetricFactory;
import com.thalesgroup.dtkit.metrics.hudson.api.registry.RegistryService;
import com.thalesgroup.dtkit.metrics.hudson.api.type.MeasureType;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.Hudson;


public abstract class MeasureTypeDescriptor<T extends MeasureType> extends Descriptor<MeasureType> {

    protected MeasureTypeDescriptor(Class<T> clazz, final Class<? extends InputMetric> inputMetricClass) {
        super(clazz);
        if (inputMetricClass != null) {
            RegistryService.addElement(getId(), inputMetricClass);
        }
    }

    @SuppressWarnings("unused")
    public static DescriptorExtensionList<MeasureType, MeasureTypeDescriptor<?>> all() {
        return Hudson.getInstance().getDescriptorList(MeasureType.class);
    }

    public abstract String getId();

    @SuppressWarnings("unused")
    public String getDisplayName() {
        return getInputMetric().getLabel();
    }

    @SuppressWarnings("unused")
    public InputMetric getInputMetric() {
        Class<? extends InputMetric> inputMetricClass = RegistryService.getElement(getId());
        try {
            return InputMetricFactory.getInstance(inputMetricClass);
        } catch (InputMetricException e) {
            throw new RuntimeException("Can't instanciate the inputMeric object for the class " + inputMetricClass);
        }
    }
}
