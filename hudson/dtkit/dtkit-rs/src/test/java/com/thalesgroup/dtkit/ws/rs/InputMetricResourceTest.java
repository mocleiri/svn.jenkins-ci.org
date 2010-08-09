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
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.thalesgroup.dtkit.junit.CppUnit;
import com.thalesgroup.dtkit.metrics.api.InputMetric;
import com.thalesgroup.dtkit.metrics.api.InputMetricFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;


public class InputMetricResourceTest extends JerseyTest {

    private WebResource webResource;

    public InputMetricResourceTest() throws Exception {
        super(new WebAppDescriptor.Builder("com.thalesgroup.dtkit.ws.rs").contextPath("dtkit-rs").build());
    }

    @Before
    public void loadWebResurce() {
        webResource = resource().path(InputMetrics.PATH);
    }


    @Test
    public void getInputMetrics() {
        InputMetricResult inputMetricResult = webResource.path("/all")
                .get(InputMetricResult.class);
        Assert.assertNotNull(inputMetricResult);
        Assert.assertEquals(InputMetrics.registry.size(), inputMetricResult.getMetrics().size());
    }

    @Test
    public void getExistXSD() {
        InputStream is = webResource.path("/cppunit/xsd")
                .get(InputStream.class);
        Assert.assertNotNull(is);
    }

    @Test
    public void getXSDNotFound() {
        WebResource webResource = resource();
        ClientResponse clientResponse = webResource.path("/notExistMetric/xsd")
                .get(ClientResponse.class);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), clientResponse.getStatus());
    }

    @Test
    public void validateInputFileValidFileForXML() throws Exception {
        String result = webResource.path("/cppunit/validate")
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .post(String.class, this.getClass().getResourceAsStream("cppunit/cppunit-valid-input.xml"));
        Assert.assertNotNull(result);
        String expectedOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><validationResult><valid>true</valid><errors/></validationResult>";
        Assert.assertEquals(expectedOutput, result);
    }

    @Test
    public void validateInputFileValidFileForJSON() throws Exception {
        String result = webResource.path("/cppunit/validate")
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_JSON)
                .post(String.class, this.getClass().getResourceAsStream("cppunit/cppunit-valid-input.xml"));
        Assert.assertNotNull(result);
        String expectedOutput = "{\"valid\":true,\"validationErrors\":[]}";
        Assert.assertEquals(expectedOutput, result);
    }

    @Test
    public void validateInputFileWithNoValidFileForXML() throws Exception {
        String result = webResource.path("/cppunit/validate")
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .post(String.class, this.getClass().getResourceAsStream("cppunit/cppunit-novalid-input.xml"));
        Assert.assertNotNull(result);
        String expectedOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><validationResult><valid>false</valid><errors><error><line>2</line><message>cvc-elt.1: Cannot find the declaration of element 'TestRun2'.</message><type>ERROR</type></error></errors></validationResult>";
        Assert.assertEquals(expectedOutput, result);
    }

    @Test
    public void validateInputFileWithNoExistingMetric() throws Exception {
        WebResource webResource = resource();
        ClientResponse clientResponse = webResource.path("/notExistMetric/validate")
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML, "application/json")
                .post(ClientResponse.class);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), clientResponse.getStatus());
    }


    @Test
    public void convertInputFileWithValidInputs() throws Exception {
        File cppunitJunitFile = webResource.path("/cppunit;format=junit/convert")
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .post(File.class, this.getClass().getResourceAsStream("cppunit/cppunit-valid-input.xml"));
        Assert.assertNotNull(cppunitJunitFile);
        InputMetric inputMetricCppUnit = InputMetricFactory.getInstance(CppUnit.class);
        Assert.assertTrue(inputMetricCppUnit.validateOutputFile(cppunitJunitFile));

        Assert.assertEquals(readContentFile(new File(this.getClass().getResource("cppunit/cppunit-valid-junit-result.xml").toURI())), readContentFile(cppunitJunitFile));
    }

    @Test
    public void convertInputFileWithNoExistingMetric1() throws Exception {
        WebResource webResource = resource();
        ClientResponse clientResponse = webResource.path("/notExistMetric/convert")
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .post(ClientResponse.class);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), clientResponse.getStatus());
    }

    @Test
    public void convertInputFileWithNoExistingMetric2() throws Exception {
        WebResource webResource = resource();
        ClientResponse clientResponse = webResource.path("/cppunit;format=tusar/convert")
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .post(ClientResponse.class);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), clientResponse.getStatus());
    }

    @Test
    public void convertInputFileNoValidInputFile() throws Exception {
        ClientResponse clientResponse = webResource.path("/cppunit;format=junit/convert")
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .post(ClientResponse.class, this.getClass().getResourceAsStream("cppunit/cppunit-novalid-input.xml"));
        Assert.assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(), clientResponse.getStatus());
    }

    private String readContentFile(File inputFile) throws IOException {
        StringBuffer sb = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        char[] buf = new char[1024];
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            sb.append(buf, 0, numRead);
        }
        reader.close();
        return sb.toString();
    }

}
