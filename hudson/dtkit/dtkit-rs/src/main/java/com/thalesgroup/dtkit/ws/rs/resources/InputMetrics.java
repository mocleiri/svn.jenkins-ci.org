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

package com.thalesgroup.dtkit.ws.rs.resources;

import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;
import com.thalesgroup.dtkit.metrics.model.InputMetric;
import com.thalesgroup.dtkit.metrics.model.InputMetricXSL;
import com.thalesgroup.dtkit.ws.rs.services.InputMetricsLocator;
import com.thalesgroup.dtkit.ws.rs.vo.InputMetricResult;
import com.thalesgroup.dtkit.ws.rs.vo.InputMetricsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.io.InputStream;


@Path(InputMetrics.PATH)
@RequestScoped
public class InputMetrics {

    public static final String PATH = "/inputMetrics";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private InputMetricsLocator inputMetricsLocator;

    @Inject
    @SuppressWarnings("unused")
    public void setInputMetricsLocator(InputMetricsLocator inputMetricsLocator) {
        this.inputMetricsLocator = inputMetricsLocator;
    }

    private InputMetric getInputMetricObject(PathSegment metricSegment) {

        String metricName = metricSegment.getPath();
        String type = metricSegment.getMatrixParameters().getFirst("type");
        String version = metricSegment.getMatrixParameters().getFirst("version");
        String format = metricSegment.getMatrixParameters().getFirst("format");

        InputMetric inputMetric = inputMetricsLocator.getInputMetricObject(metricName, type, version, format);
        if (inputMetric == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return inputMetric;
    }


    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @SuppressWarnings("unused")
    public Response getInputMetrics() {
        logger.debug("getInputMetrics() service");
        InputMetricsResult inputMetricsResult = new InputMetricsResult();
        inputMetricsResult.setMetrics(inputMetricsLocator.getAllMetrics());
        return Response.ok(inputMetricsResult).build();
    }

    @GET
    @Path("{metric}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @SuppressWarnings("unused")
    public Response getInputMetric(@PathParam("metric") PathSegment metricSegment) {
        logger.debug("getInputMetric() service");

        //Retrieving the metric
        InputMetric inputMetric = getInputMetricObject(metricSegment);

        InputMetricResult inputMetricResult = new InputMetricResult();
        inputMetricResult.setInputMetric(inputMetric);
        return Response.ok(inputMetricResult).build();
    }


    @GET
    @Path("{metric}/xsd")
    @Produces(MediaType.APPLICATION_XML)
    @SuppressWarnings("unused")
    public InputStream getXSD(@PathParam("metric") PathSegment metricSegment) {
        logger.debug("getXSD() service");
        InputMetric inputMetric = getInputMetricObject(metricSegment);
        if (!(inputMetric instanceof InputMetricXSL)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        InputMetricXSL inputMetricXSL = (InputMetricXSL) inputMetric;
        String xsdPath = inputMetricXSL.getInputXsd();
        if (xsdPath == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return inputMetric.getClass().getResourceAsStream(xsdPath);
    }

}