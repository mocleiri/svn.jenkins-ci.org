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

package com.thalesgroup.dtkit.ws.rs;

import com.thalesgroup.dtkit.junit.model.JUnitModel;
import com.thalesgroup.dtkit.metrics.api.InputMetric;
import com.thalesgroup.dtkit.tusar.model.TusarModel;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;


@Provider
@Produces(MediaType.APPLICATION_XML)
public class InputMetricXMLProvider implements ContextResolver<JAXBContext> {

    private JAXBContext ctx;

    public InputMetricXMLProvider() {
        try {
            ServiceLoader<InputMetric> metricServiceLoader = ServiceLoader.load(InputMetric.class, Thread.currentThread().getContextClassLoader());
            metricServiceLoader.reload();

            List<Class> listInputMetricClass = new ArrayList<Class>();
            for (InputMetric inputMetric : metricServiceLoader) {
                listInputMetricClass.add(inputMetric.getClass());
            }
            //Add the object class built by the response of the method
            listInputMetricClass.add(InputMetricsResult.class);
            listInputMetricClass.add(InputMetricResult.class);
            //Add the object implemented class of the interface com.thalesgroup.dtkit.metrics.api.OutputMetric
            listInputMetricClass.add(JUnitModel.class);
            listInputMetricClass.add(TusarModel.class);

            ctx = JAXBContext.newInstance(listInputMetricClass.toArray(new Class[listInputMetricClass.size()]));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public JAXBContext getContext(Class<?> type) {
        // Used a dedicated JAXBContext due to the com.thalesgroup.dtkit.metrics.api.OutputMetric interface.
        // There is an adapter. Nevertheless, all the interface implementations must have an instance and they must be
        // referred by the JAXBContext
        if (type == InputMetricsResult.class || type== InputMetricResult.class) {
            return ctx;
        } else {
            return null;
        }
    }

}