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

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.thalesgroup.dtkit.junit.CppUnit;
import com.thalesgroup.dtkit.metrics.api.InputMetric;
import com.thalesgroup.dtkit.metrics.api.InputMetricFactory;
import com.thalesgroup.dtkit.ws.rs.resources.InputMetricsConversion;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileReader;


public class InputMetricsConversionTest extends InputMetricsAbstractTest {

    @Before
    public void loadWebResurce() {
        webResource = resource().path(InputMetricsConversion.PATH);
        webResource.addFilter(new LoggingFilter());
    }

    @Test
    public void convertInputFileWithValidInputs() throws Exception {

        ClientResponse clientResponse = webResource.path("/cppunit;format=junit")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class, this.getClass().getResourceAsStream("cppunit/cppunit-valid-input.xml"));
        Assert.assertEquals(Response.Status.OK.getStatusCode(), clientResponse.getStatus());
        File cppunitJunitFile = clientResponse.getEntity(File.class);
        Assert.assertNotNull(cppunitJunitFile);
        InputMetric inputMetricCppUnit = InputMetricFactory.getInstance(CppUnit.class);
        Assert.assertTrue(inputMetricCppUnit.validateOutputFile(cppunitJunitFile));
        FileReader cppunitJunitFileReader = new FileReader(cppunitJunitFile);
        Assert.assertEquals(readContentInputStream(this.getClass().getResourceAsStream("cppunit/cppunit-valid-junit-result.xml")), readContentReader(cppunitJunitFileReader));
        cppunitJunitFileReader.close();
    }

    @Test
    public void convertInputFileWithNoExistingMetric1() throws Exception {
        WebResource webResource = resource();
        ClientResponse clientResponse = webResource.path("/notExistMetric")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), clientResponse.getStatus());
    }

    @Test
    public void convertInputFileWithNoExistingMetric2() throws Exception {
        WebResource webResource = resource();
        ClientResponse clientResponse = webResource.path("/cppunit;format=tusar")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), clientResponse.getStatus());
    }

    @Test
    public void convertInputFileNoValidInputFile() throws Exception {
        ClientResponse clientResponse = webResource.path("/cppunit;format=junit")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class, this.getClass().getResourceAsStream("cppunit/cppunit-novalid-input.xml"));
        Assert.assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(), clientResponse.getStatus());
    }


}
