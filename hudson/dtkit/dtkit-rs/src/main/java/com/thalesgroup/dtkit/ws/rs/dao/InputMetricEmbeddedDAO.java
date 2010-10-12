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

package com.thalesgroup.dtkit.ws.rs.dao;

import com.thalesgroup.dtkit.metrics.model.*;
import com.thalesgroup.dtkit.ws.rs.model.InputMetricSelector;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InputMetricEmbeddedDAO implements InputMetricDAO {

    private Map<InputMetricSelector, InputMetric> allMetrics = new ConcurrentHashMap<InputMetricSelector, InputMetric>();

    private void loadMetrics() throws InputMetricException {
        ServiceLoader<InputMetric> metricServiceLoader = ServiceLoader.load(InputMetric.class, Thread.currentThread().getContextClassLoader());
        metricServiceLoader.reload();
        for (InputMetric inputMetric : metricServiceLoader) {
            try {
                InputMetricSelector inputMetricSelector = new InputMetricSelector(
                        inputMetric.getToolName(),
                        inputMetric.getToolVersion(),
                        inputMetric.getToolType().name(),
                        inputMetric.getOutputFormatType());
                allMetrics.put(inputMetricSelector, InputMetricFactory.getInstance(inputMetric.getClass()));
            } catch (InputMetricException e) {
                throw new InputMetricException("Can't load all the embedded metrics data");
            }
        }
    }

    @SuppressWarnings("unused")
    public InputMetricEmbeddedDAO() throws InputMetricException {
        loadMetrics();
    }

    @Override
    public boolean isPersistenceStore() {
        return false;
    }

    @Override
    public void insert(String name, String version, InputType toolType, File xsl, File xsd, OutputMetric outputMetric) {
        throw new UnsupportedOperationException("The Insert method is not allowed for embedded data");
    }

    @Override
    public void delete(String name, String version, InputType toolType, OutputMetric outputMetric) {
        throw new UnsupportedOperationException("The Delete method is not allowed for embedded data");
    }

    @Override
    public long getCount() {
        return allMetrics.size();
    }

    @Override
    public Collection<? extends InputMetric> getInputMetric(InputMetricSelector inputMetricSelector) {


        if (inputMetricSelector.isNoCriteria()) {
            return allMetrics.values();
        }

        List<InputMetric> listInputMetrics = new ArrayList<InputMetric>();

        InputMetric metric = allMetrics.get(inputMetricSelector);
        if (metric != null) {
            listInputMetrics.add(metric);
            return listInputMetrics;
        }

        String name = inputMetricSelector.getToolName();
        String version = inputMetricSelector.getToolVersion();
        String format = inputMetricSelector.getOutputFormat();
        String type = inputMetricSelector.getTooType();

        InputMetric[] allMetricsList = new InputMetric[allMetrics.values().size()];
        allMetrics.values().toArray(allMetricsList);
        for (int i = 0; i < allMetricsList.length; i++) {
            metric = allMetricsList[i];
            //Name
            if (name != null) {
                if (!name.equalsIgnoreCase(metric.getToolName())) {
                    continue;
                }
            }

            //Version
            if (version != null) {
                if (!version.equalsIgnoreCase(metric.getToolVersion())) {
                    continue;
                }
            }

            //Format
            if (format != null) {
                if (metric.getOutputFormatType() != null)
                    if (!format.equalsIgnoreCase(metric.getOutputFormatType().getKey())) {
                        continue;
                    }
            }

            //type
            if (type != null) {
                if (!type.equalsIgnoreCase(metric.getToolType().name())) {
                    continue;
                }
            }

            listInputMetrics.add(metric);
        }


        return listInputMetrics;
    }

    @Override
    public Collection<? extends InputMetric> getInputMetrics() {
        return new ArrayList(allMetrics.values());
    }
}
