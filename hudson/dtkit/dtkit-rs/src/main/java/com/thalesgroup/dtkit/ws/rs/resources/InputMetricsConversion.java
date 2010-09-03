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
import com.sun.jersey.multipart.FormDataParam;
import com.thalesgroup.dtkit.metrics.model.InputMetric;
import com.thalesgroup.dtkit.util.converter.ConversionException;
import com.thalesgroup.dtkit.util.converter.ConversionService;
import com.thalesgroup.dtkit.util.validator.ValidationException;
import com.thalesgroup.dtkit.ws.rs.services.InputMetricsLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.print.attribute.standard.Media;
import java.io.File;
import java.io.IOException;


@Path(InputMetricsConversion.PATH)
@RequestScoped
public class InputMetricsConversion {

    public static final String PATH = "/inputMetricsConversion";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private InputMetricsLocator inputMetricsLocator;

    private ConversionService conversionService;

    @Inject
    @SuppressWarnings("unused")
    public void load(InputMetricsLocator inputMetricsLocator, ConversionService conversionService) {
        this.inputMetricsLocator = inputMetricsLocator;
        this.conversionService = conversionService;
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

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @SuppressWarnings("unused")
    public Response convertInputFile( @FormDataParam("file") File inputFile, @FormDataParam("xsl") File inputXsl) {
        logger.debug("convertInputMetric() service");
        try {
            File dest = File.createTempFile("toot", "ttt");
            conversionService.convert(inputXsl, inputFile, dest);
            return Response.ok(dest).build();
        } catch (IOException ioe) {
            logger.error("Conversion error", ioe);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        catch (ConversionException ce) {
            logger.error("Conversion error", ce);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/{metric}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @SuppressWarnings("unused")
    public Response convertInputMetric(@PathParam("metric") PathSegment metricSegment, @FormDataParam("file") File inputMetricFile) {
        logger.debug("convertInputMetric() service");
        try {

            //Retrieving the metric
            InputMetric inputMetric = getInputMetricObject(metricSegment);

            //Validating input file
            boolean result;
            try {
                result = inputMetric.validateInputFile(inputMetricFile);
            } catch (ValidationException e) {
                throw new WebApplicationException(Response.Status.PRECONDITION_FAILED);
            }
            if (!result) {
                throw new WebApplicationException(Response.Status.PRECONDITION_FAILED);
            }

            //Converting the input file
            File dest = File.createTempFile("toot", "ttt");
            inputMetric.convert(inputMetricFile, dest);
            return Response.ok(dest).build();

        } catch (IOException ioe) {
            logger.error("Conversion error for " + metricSegment.getPath(), ioe);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        catch (ConversionException ce) {
            logger.error("Conversion error for " + metricSegment.getPath(), ce);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }



}
