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

package com.thalesgroup.dtkit.ws.rs.services;

import com.google.inject.Singleton;
import com.thalesgroup.dtkit.metrics.model.InputMetric;
import com.thalesgroup.dtkit.metrics.model.InputMetricException;
import com.thalesgroup.dtkit.metrics.model.InputMetricFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

@Singleton
public class InputMetricsLocator {

    private List<InputMetric> allMetrics = new ArrayList<InputMetric>();

    private void loadMetrics() {
        ServiceLoader<InputMetric> metricServiceLoader = ServiceLoader.load(InputMetric.class, Thread.currentThread().getContextClassLoader());
        metricServiceLoader.reload();
        for (InputMetric inputMetric : metricServiceLoader) {
            try {
                allMetrics.add(InputMetricFactory.getInstance(inputMetric.getClass()));
            } catch (InputMetricException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unused")
    public InputMetricsLocator() {
        loadMetrics();
    }

    public InputMetric getInputMetricObject(String metricName, String type, String version, String format) {

        for (InputMetric inputMetric : allMetrics) {
            if (inputMetric.getToolName().toUpperCase().equalsIgnoreCase(metricName)) {

                if ((type != null) && (!(inputMetric.getToolType().toString()).equalsIgnoreCase(type))) {
                    continue;
                }

                if ((version != null) && (!inputMetric.getToolVersion().equalsIgnoreCase(version))) {
                    continue;
                }

                if ((format != null) && (!(inputMetric.getOutputFormatType().getKey()).equalsIgnoreCase(format))) {
                    continue;
                }

                return inputMetric;
            }
        }
        return null;
    }

    public List<InputMetric> getAllMetrics() {
        return allMetrics;
    }
}
