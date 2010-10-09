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
import com.thalesgroup.dtkit.metrics.model.InputMetricXSL;
import com.thalesgroup.dtkit.util.converter.ConversionException;
import com.thalesgroup.dtkit.util.converter.ConversionService;
import com.thalesgroup.dtkit.util.validator.ValidationException;
import com.thalesgroup.dtkit.ws.rs.dao.InputMetricDAO;
import com.thalesgroup.dtkit.ws.rs.model.InputMetricSelector;
import com.thalesgroup.dtkit.ws.rs.vo.InputMetricValidationResult;
import com.thalesgroup.dtkit.ws.rs.vo.InputMetricVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Path(InputMetricsConversion.PATH)
@RequestScoped
public class InputMetricsConversion {

    public static final String PATH = "/inputMetricsConversion";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private List<InputMetricDAO> inputMetricDAOList;

    private ConversionService conversionService;

    @Inject
    @SuppressWarnings("unused")
    public void load(List<InputMetricDAO> inputMetricDAOList, ConversionService conversionService) {
        this.conversionService = conversionService;
        this.inputMetricDAOList = inputMetricDAOList;
    }

    private File convertCustom(File inputXmlLFile, File inputXslFile) throws ConversionException {

        if (inputXmlLFile == null) {
            throw new NullPointerException("For a custom conversion, the input file is mandatory");
        }
        if (inputXslFile == null) {
            throw new NullPointerException("For a custom conversion, the input XSL file is mandatory");
        }

        try {
            File dest = File.createTempFile("temp", Long.toString(System.nanoTime()));
            conversionService.convert(inputXslFile, inputXmlLFile, dest);
            return dest;
        } catch (IOException ioe) {
            logger.error("Conversion error", ioe);
            throw new ConversionException("Conversion error for " + inputXmlLFile, ioe);
        }
    }


    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @SuppressWarnings("unused")
    public Response convertInputFile(
            @MatrixParam("name") String name, @MatrixParam("version") String version, @MatrixParam("type") String type, @MatrixParam("format") String format,
            @FormDataParam("file") File inputXmlLFile, @FormDataParam("xsl") File inputXslFile) {

        try {

            InputMetricSelector inputMetricSelector = new InputMetricSelector(name, version, type, format);
            if (inputMetricSelector.isNoCriteria()) {
                File convertedFile = convertCustom(inputXmlLFile, inputXslFile);
                return Response.ok(convertedFile).build();
            }

            List<InputMetric> metrics = new ArrayList<InputMetric>();
            for (InputMetricDAO inputMetricDAO : inputMetricDAOList) {
                metrics.addAll(inputMetricDAO.getInputMetric(inputMetricSelector));
            }

            if (metrics.size() == 0) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            if (metrics.size() > 1) {
                return Response.status(Response.Status.CONFLICT).build();
            }

            InputMetric metric = metrics.get(0);
            if (!(metric instanceof InputMetricXSL)) {
                return Response.status(Response.Status.PRECONDITION_FAILED).build();
            }

            //Validating input file
            boolean result = metric.validateInputFile(inputXmlLFile);
            if (!result) {
                InputMetricVo inputMetricVo = new InputMetricVo(metric.getToolName(), metric.getToolVersion(), metric.getToolType().name(), metric.getOutputFormatType().getKey());
                InputMetricValidationResult inputMetricValidationResult = new InputMetricValidationResult();
                inputMetricValidationResult.setValid(metric.validateInputFile(inputXmlLFile));
                inputMetricValidationResult.setValidationErrors(metric.getInputValidationErrors());
                return Response.ok(inputMetricValidationResult).build();
            }

            //Converting the input file
            File dest = File.createTempFile("temp", Long.toString(System.nanoTime()));
            metric.convert(inputXmlLFile, dest);
            return Response.ok(dest).build();
        }
        catch (IOException ioe) {
            throw new WebApplicationException(ioe, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}
