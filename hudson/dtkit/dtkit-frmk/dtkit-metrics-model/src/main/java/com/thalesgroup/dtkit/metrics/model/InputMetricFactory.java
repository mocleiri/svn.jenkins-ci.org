/*******************************************************************************
 * Copyright (c) 2010 Thales Corporate Services SAS                             *
 * Author : Gregory Boissinot, Guillaume Tanier                                 *
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

package com.thalesgroup.dtkit.metrics.model;

import com.thalesgroup.dtkit.util.converter.ConversionServiceFactory;
import com.thalesgroup.dtkit.util.validator.ValidationServiceFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class InputMetricFactory {

    private static Map<Class<? extends InputMetric>, InputMetric> instanceDictionnary = new ConcurrentHashMap<Class<? extends InputMetric>, InputMetric>();

    private static void wireInputMetricDependencies(Class<? extends InputMetric> classInputMetric, InputMetric inputMetric) {
        if ((InputMetricXSL.class).isAssignableFrom(classInputMetric)) {
            ((InputMetricXSL) inputMetric).set(ConversionServiceFactory.getInstance(), ValidationServiceFactory.getInstance());
        }
    }

    public static InputMetric getInstance(Class<? extends InputMetric> classInputMetric) throws InputMetricException {

        InputMetric inputMetric;
        if ((inputMetric = instanceDictionnary.get(classInputMetric)) != null) {
            return inputMetric;
        }

        try {
            inputMetric = classInputMetric.newInstance();
            instanceDictionnary.put(classInputMetric, inputMetric);
        }
        catch (InstantiationException ie) {
            throw new InputMetricException(ie);
        }
        catch (IllegalAccessException iae) {
            throw new InputMetricException(iae);
        }

        //Compute inputMetric dependencies
        wireInputMetricDependencies(classInputMetric, inputMetric);

        return inputMetric;
    }


    @SuppressWarnings("unused")
    public static InputMetric getInstanceWithNoDefaultConstructor(Class<? extends InputMetric> classInputMetric, Class<?>[] parameterTypes, Object[] parameters) throws InputMetricException {
        InputMetric inputMetric;
        if ((inputMetric = instanceDictionnary.get(classInputMetric)) != null) {
            return inputMetric;
        }

        try {
            Constructor<? extends InputMetric> constructor = classInputMetric.getDeclaredConstructor(parameterTypes);
            inputMetric = constructor.newInstance(parameters);
            instanceDictionnary.put(classInputMetric, inputMetric);
        }
        catch (InstantiationException ie) {
            throw new InputMetricException(ie);
        }
        catch (IllegalAccessException iae) {
            throw new InputMetricException(iae);
        }
        catch (NoSuchMethodException e) {
            throw new InputMetricException(e);
        }
        catch (InvocationTargetException e) {
            throw new InputMetricException(e);
        }

        //Compute inputMetric dependencies
        wireInputMetricDependencies(classInputMetric, inputMetric);

        return inputMetric;
    }

}
