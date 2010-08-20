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
import com.thalesgroup.dtkit.ws.rs.resources.InputMetricsValidation;
import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


public class InputMetricsValidationTest extends InputMetricsAbstractTest {

    @Before
    public void loadWebResurce() {
        webResource = resource().path(InputMetricsValidation.PATH);
        webResource.addFilter(new LoggingFilter());
    }

    @Test
    public void validateInputFileValidFileForXML() throws Exception {
        String result = webResource.path("/cppunit")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(String.class, this.getClass().getResourceAsStream("cppunit/cppunit-valid-input.xml"));
        Assert.assertNotNull(result);
        String expectedOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><validationResult><valid>true</valid><errors/></validationResult>";
        Assert.assertEquals(expectedOutput, result);
    }

    @Test
    public void validateInputFileValidFileForJSON() throws Exception {

        ClientResponse clientResponse = webResource.path("/cppunit")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, this.getClass().getResourceAsStream("cppunit/cppunit-valid-input.xml"));
        Assert.assertEquals(Response.Status.OK.getStatusCode(), clientResponse.getStatus());
        JsonNode node = clientResponse.getEntity(JsonNode.class);

        Assert.assertNotNull(node);
        //Node valid
        JsonNode jsonNodeValid = node.get("valid");
        Assert.assertTrue(jsonNodeValid.isBoolean());
        Assert.assertTrue(node.get("valid").getBooleanValue());
        //Node validationErrors
        JsonNode jsonNodeValidationErrors = node.get("validationErrors");
        jsonNodeValidationErrors.isArray();
        jsonNodeValidationErrors.getElements();
        Assert.assertFalse(jsonNodeValidationErrors.iterator().hasNext());
        validateValidateMethodJSONXMLSchema(node.toString());
    }

    @Test
    public void validateInputFileWithNoValidFileForXML() throws Exception {
        ClientResponse clientResponse = webResource.path("/cppunit")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class, this.getClass().getResourceAsStream("cppunit/cppunit-novalid-input.xml"));
        Assert.assertEquals(Response.Status.OK.getStatusCode(), clientResponse.getStatus());
        String xmlResult = clientResponse.getEntity(String.class);
        Assert.assertNotNull(xmlResult);
        String expectedOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><validationResult><valid>false</valid><errors><error><line>2</line><message>cvc-elt.1: Cannot find the declaration of element 'TestRun2'.</message><type>ERROR</type></error></errors></validationResult>";
        Assert.assertEquals(expectedOutput, xmlResult);
    }

    @Test
    public void validateInputFileWithNoExistingMetric() throws Exception {
        WebResource webResource = resource();
        ClientResponse clientResponse = webResource.path("/notExistMetric")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), clientResponse.getStatus());
    }

}
