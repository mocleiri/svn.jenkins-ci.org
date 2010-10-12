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
import com.thalesgroup.dtkit.util.validator.ValidationError;
import com.thalesgroup.dtkit.util.validator.ValidationException;
import com.thalesgroup.dtkit.util.validator.ValidationService;
import com.thalesgroup.dtkit.ws.rs.dao.InputMetricDAO;
import com.thalesgroup.dtkit.ws.rs.model.InputMetricSelector;
import com.thalesgroup.dtkit.ws.rs.vo.InputMetricValidationResult;
import com.thalesgroup.dtkit.ws.rs.vo.InputMetricVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


@Path(InputMetricsValidation.PATH)
@RequestScoped
public class InputMetricsValidation {

    public static final String PATH = "/inputMetricsValidation";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private List<InputMetricDAO> inputMetricDAOList;

    private ValidationService validationService;

    @Inject
    @SuppressWarnings("unused")
    public void load(List<InputMetricDAO> inputMetricDAOList, ValidationService validationService) {
        this.inputMetricDAOList = inputMetricDAOList;
        this.validationService = validationService;
    }

    private InputMetricValidationResult validateCustom(File inputXmlLFile, File inputXsdFile) throws ValidationException {

        if (inputXmlLFile == null) {
            throw new NullPointerException("For a custom validation, the input file is mandatory");
        }
        if (inputXsdFile == null) {
            throw new NullPointerException("For a custom validation, the input XSD file is mandatory");
        }

        InputMetricValidationResult inputMetricValidationResult = new InputMetricValidationResult();
        List<ValidationError> validationErrors = validationService.processValidation(inputXsdFile, inputXmlLFile);
        inputMetricValidationResult.setValid(validationErrors.size() == 0);
        inputMetricValidationResult.setValidationErrors(validationErrors);
        return inputMetricValidationResult;
    }


    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @SuppressWarnings("unused")
    public Response validateInputFile(
            @MatrixParam("name") String name, @MatrixParam("version") String version, @MatrixParam("type") String type, @MatrixParam("format") String format,
            @FormDataParam("file") File inputXmlLFile, @FormDataParam("xsd") File inputXsdFile) {


        InputMetricSelector inputMetricSelector = new InputMetricSelector(name, version, type, format);
        if (inputMetricSelector.isNoCriteria()) {
            InputMetricValidationResult inputMetricValidationResult = validateCustom(inputXmlLFile, inputXsdFile);
            return Response.ok(inputMetricValidationResult).build();
        }

        List<InputMetric> metrics = new ArrayList<InputMetric>();
        for (InputMetricDAO inputMetricDAO : inputMetricDAOList) {
            metrics.addAll(inputMetricDAO.getInputMetric(inputMetricSelector));
        }

        if (metrics.size() == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (metrics.size() == 1) {
            InputMetric metric = metrics.get(0);
            InputMetricValidationResult inputMetricValidationResult = new InputMetricValidationResult();
            inputMetricValidationResult.setValid(metric.validateInputFile(inputXmlLFile));
            inputMetricValidationResult.setValidationErrors(metric.getInputValidationErrors());
             return Response.ok(inputMetricValidationResult).header("charset", "utf-8").build();
        }

        InputMetricValidationResult inputMetricValidationResult;
        List<InputMetricValidationResult> results = new ArrayList<InputMetricValidationResult>();
        for (InputMetric metric : metrics) {
            InputMetricVo inputMetricVo = new InputMetricVo(metric.getToolName(), metric.getToolVersion(), metric.getToolType().name(), metric.getOutputFormatType().getKey());
            inputMetricValidationResult = new InputMetricValidationResult();
            inputMetricValidationResult.setValid(metric.validateInputFile(inputXmlLFile));
            inputMetricValidationResult.setValidationErrors(metric.getInputValidationErrors());
            inputMetricValidationResult.setMetric(inputMetricVo);
            results.add(inputMetricValidationResult);
        }

        GenericEntity entity = new GenericEntity<List<InputMetricValidationResult>>(results) {
        };
        return Response.ok(entity).header("charset", "utf-8").build();
    }

}
