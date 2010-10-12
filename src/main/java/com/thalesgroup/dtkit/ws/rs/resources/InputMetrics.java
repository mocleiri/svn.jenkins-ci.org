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
import com.thalesgroup.dtkit.metrics.model.*;
import com.thalesgroup.dtkit.ws.rs.dao.InputMetricDAO;
import com.thalesgroup.dtkit.ws.rs.model.InputMetricSelector;
import com.thalesgroup.dtkit.ws.rs.vo.InputMetricResult;
import com.thalesgroup.dtkit.ws.rs.vo.InputMetricsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


@Path(InputMetrics.PATH)
@RequestScoped
public class InputMetrics {

    public static final String PATH = "/inputMetrics";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private List<InputMetricDAO> inputMetricDAOList;


    @Inject
    @SuppressWarnings("unused")
    public void set(List<InputMetricDAO> inputMetricDAOList) {
        this.inputMetricDAOList = inputMetricDAOList;
    }


    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @SuppressWarnings("unused")
    public Response getInputMetricByName(@MatrixParam("name") String name, @MatrixParam("version") String version, @MatrixParam("type") String type, @MatrixParam("format") String format) {

        if (logger.isDebugEnabled()) {
            logger.debug("getInputMetricByName() service");
        }

        List<InputMetric> metrics = new ArrayList<InputMetric>();
        InputMetricSelector inputMetricSelector = new InputMetricSelector(name, version, type, format);
        for (InputMetricDAO inputMetricDAO : inputMetricDAOList) {
            metrics.addAll(inputMetricDAO.getInputMetric(inputMetricSelector));
        }

        if (metrics.size() == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (metrics.size() == 1) {
            InputMetricResult inputMetricResult = new InputMetricResult();
            inputMetricResult.setInputMetric(metrics.get(0));
            return Response.ok(inputMetricResult).build();
        }

        InputMetricsResult inputMetricsResult = new InputMetricsResult();
        inputMetricsResult.setMetrics(metrics);
        return Response.ok(inputMetricsResult).build();
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @SuppressWarnings("unused")
    public Response insert(@FormDataParam("name") String name, @FormDataParam("version") String version,
                           @FormDataParam("type") String type, final @FormDataParam("format") String format,
                           @FormDataParam("xsl") File inputXslLFile, @FormDataParam("xsd") File inputXsdLFile) {

        if (logger.isDebugEnabled()) {
            logger.debug("getInputMetricByName() service");
        }

        //All parameters except xsd must be provided
        if ((name == null)
                || (version == null)
                || (type == null)
                || (format == null)
                || (inputXslLFile == null)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        // Get the toolType
        InputType toolType = InputType.getInputType(type);

        //Get the output metric
        OutputMetric outputMetric = new AbstractOutputMetric() {
            @Override
            public String getKey() {
                return format;
            }

            @Override
            public String getDescription() {
                return "Given format by REST";
            }

            @Override
            public String getVersion() {
                return "N/A";
            }

            @Override
            public String[] getXsdNameList() {
                return null;
            }
        };

        InputMetricSelector inputMetricSelector = new InputMetricSelector(name, version, type, format);
        for (InputMetricDAO inputMetricDAO : inputMetricDAOList) {
            if (inputMetricDAO.isPersistenceStore()) {
                inputMetricDAO.insert(name, version, toolType, inputXslLFile, inputXsdLFile, outputMetric);
            }
        }

        return Response.status(Response.Status.CREATED).build();
    }


    @DELETE
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @SuppressWarnings("unused")
    public Response delete(@FormDataParam("name") String name, @FormDataParam("version") String version,
                           @FormDataParam("type") String type, final @FormDataParam("format") String format) {
        if (logger.isDebugEnabled()) {
            logger.debug("getInputMetricByName() service");
        }

        //All parameters except xsd must be provided
        if ((name == null)
                || (version == null)
                || (type == null)
                || (format == null)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        // Get the toolType
        InputType toolType = InputType.getInputType(type);

        //Get the output metric
        OutputMetric outputMetric = new AbstractOutputMetric() {
            @Override
            public String getKey() {
                return format;
            }

            @Override
            public String getDescription() {
                return "Given format by REST";
            }

            @Override
            public String getVersion() {
                return "N/A";
            }

            @Override
            public String[] getXsdNameList() {
                return null;
            }
        };

        InputMetricSelector inputMetricSelector = new InputMetricSelector(name, version, type, format);
        for (InputMetricDAO inputMetricDAO : inputMetricDAOList) {
            if (inputMetricDAO.isPersistenceStore()) {
                inputMetricDAO.delete(name, version, toolType, outputMetric);
            }
        }

        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path("/xsd")
    @Produces(MediaType.APPLICATION_XML)
    @SuppressWarnings("unused")
    public byte[] getXSD(@MatrixParam("name") String name, @MatrixParam("version") String version, @MatrixParam("type") String type, @MatrixParam("format") String format) {
        if (logger.isDebugEnabled()) {
            logger.debug("getXSD() service");
        }

        List<InputMetric> metrics = new ArrayList<InputMetric>();
        InputMetricSelector inputMetricSelector = new InputMetricSelector(name, version, type, format);
        for (InputMetricDAO inputMetricDAO : inputMetricDAOList) {
            metrics.addAll(inputMetricDAO.getInputMetric(inputMetricSelector));
        }

        if (metrics.size() == 0) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        if (metrics.size() > 1) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }

        InputMetric inputMetric = metrics.get(0);
        if (!(inputMetric instanceof InputMetricXSL)) {
            throw new WebApplicationException(Response.Status.PRECONDITION_FAILED);
        }

        InputMetricXSL inputMetricXSL = (InputMetricXSL) inputMetric;
        String[] xsdPath = inputMetricXSL.getInputXsdNameList();
        if (xsdPath == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < xsdPath.length; i++) {
            try {
                InputStream inputStream = inputMetric.getClass().getResourceAsStream(xsdPath[i]);
                byte[] buffer = new byte[1024];
                int wasRead = 0;
                do {
                    wasRead = inputStream.read(buffer);
                    if (wasRead > 0) {
                        baos.write(buffer, 0, wasRead);
                    }
                }
                while (wasRead > 0);
            }
            catch (IOException ioe) {
                throw new WebApplicationException(ioe, Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        return baos.toByteArray();
    }

}