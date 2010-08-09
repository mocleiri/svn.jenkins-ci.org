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

import com.thalesgroup.dtkit.metrics.api.InputMetric;
import com.thalesgroup.dtkit.metrics.api.InputMetricException;
import com.thalesgroup.dtkit.metrics.api.InputMetricFactory;
import com.thalesgroup.dtkit.metrics.api.InputMetricXSL;
import com.thalesgroup.dtkit.util.converter.ConversionException;
import com.thalesgroup.dtkit.util.validator.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;


@Path(InputMetrics.PATH)
public class InputMetrics {

    public static final String PATH = "/inputMetrics";

    private Logger logger = LoggerFactory.getLogger(getClass());

    static List<InputMetric> registry = new ArrayList<InputMetric>();

    static {
        ServiceLoader<InputMetric> metricServiceLoader = ServiceLoader.load(InputMetric.class, Thread.currentThread().getContextClassLoader());
        metricServiceLoader.reload();
        for (InputMetric inputMetric : metricServiceLoader) {
            try {
                registry.add(InputMetricFactory.getInstance(inputMetric.getClass()));
            } catch (InputMetricException e) {
                e.printStackTrace();
            }
        }
    }

    private InputMetric getInputMetric(PathSegment metricSegment) {

        String metricName = metricSegment.getPath();
        String type = metricSegment.getMatrixParameters().getFirst("type");
        String version = metricSegment.getMatrixParameters().getFirst("version");
        String format = metricSegment.getMatrixParameters().getFirst("format");

        for (InputMetric inputMetric : registry) {
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


        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }


    @GET
    @Path("/all")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getInputMetrics() {
        logger.debug("getInputMetrics() service");
        InputMetricResult inputMetricResults = new InputMetricResult();
        inputMetricResults.setMetrics(registry);
        return Response.ok(inputMetricResults).build();
    }


    @GET
    @Path("{metric}/xsd")
    @Produces(MediaType.APPLICATION_XML)
    public InputStream getXSD(@PathParam("metric") PathSegment metricSegment) {
        logger.debug("getXSD() service");
        InputMetric inputMetric = getInputMetric(metricSegment);
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


    @POST
    @Path("/{metric}/validate")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response validateInputFile(@PathParam("metric") PathSegment metricSegment, File inputXMLFile) {
        logger.debug("validateInputFile() service");
        InputMetricValidationResult inputMetricValidationResult = new InputMetricValidationResult();
        try {
            InputMetric inputMetric = getInputMetric(metricSegment);
            inputMetricValidationResult.setValid(inputMetric.validateInputFile(inputXMLFile));
            inputMetricValidationResult.setValidationErrors(inputMetric.getInputValidationErrors());

        } catch (ValidationException ve) {
            logger.error("Validation error for " + metricSegment.getPath(), ve);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return Response.ok(inputMetricValidationResult).build();
    }

    @POST
    @Path("/{metric}/convert")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response convertInputFile(@PathParam("metric") PathSegment metricSegment, File inputMetricFile) {
        logger.debug("convertInputFile() service");
        try {
            //Validating input file
            Response inputMetricValidationResponse = validateInputFile(metricSegment, inputMetricFile);
            InputMetricValidationResult inputMetricValidationResult = (InputMetricValidationResult)inputMetricValidationResponse.getEntity();
            if (!inputMetricValidationResult.isValid()){
                throw new WebApplicationException(Response.Status.PRECONDITION_FAILED);
            }

            //Retrieving the metric
            InputMetric inputMetric = getInputMetric(metricSegment);

            //Converting the input file
            File dest = File.createTempFile("toot", "ttt");
            inputMetric.convert(inputMetricFile, dest);
            return Response.ok(dest).build();

        } catch (IOException ioe) {
            logger.error("Conversion error for " + metricSegment.getPath(), ioe);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);

        } catch (ConversionException ce) {
            logger.error("Conversion error for " + metricSegment.getPath(), ce);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


}