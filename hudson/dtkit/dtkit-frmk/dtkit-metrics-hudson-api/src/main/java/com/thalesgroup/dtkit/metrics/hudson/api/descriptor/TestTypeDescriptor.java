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
import com.thalesgroup.dtkit.metrics.hudson.api.type.TestType;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.Hudson;


public abstract class TestTypeDescriptor<T extends TestType> extends Descriptor<TestType> {

    protected TestTypeDescriptor(Class<T> classType, final Class<? extends InputMetric> inputMetricClass) {
        super(classType);
        if (inputMetricClass != null) {
            RegistryService.addElement(getId(), inputMetricClass);
        }
    }

    @SuppressWarnings("unused")
    public static DescriptorExtensionList<TestType, TestTypeDescriptor<?>> all() {
        return Hudson.getInstance().getDescriptorList(TestType.class);
    }

    public abstract String getId();

    @SuppressWarnings("unused")
    @Override
    public String getDisplayName() {
        return getInputMetric().getLabel();
    }

    @SuppressWarnings("unused")
    public InputMetric getInputMetric() {        
        final Class<? extends InputMetric> inputMetricClass = RegistryService.getElement(getId());
        /** Can't retrieve the instance with guice due to a
         java.lang.NoClassDefFoundError: com/google/inject/internal/Finalizer$ShutDown
         thrown when used by the DTKIT library is used by a Hudson plugin as the xUnit Hudson plugin.
         The exception is thrown on slave usage (works for master usage).
         **/
//        Injector injector = Guice.createInjector(new AbstractModule() {
//            @Override
//            protected void configure() {
//                //Optional binding, provided by default in Guice)
//                bind(ValidationService.class).in(Singleton.class);
//                bind(ConversionService.class).in(Singleton.class);
//                //Make the instance of inputMetricClass also a Singleton
//                bind(inputMetricClass).in(Singleton.class);
//            }
//        });
//        return injector.getInstance(inputMetricClass);
        try {
            return InputMetricFactory.getInstance(inputMetricClass);
        } catch (InputMetricException e) {
            return null;
        }
    }

}

