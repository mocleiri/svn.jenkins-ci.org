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
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.thalesgroup.dtkit.junit.CppTest;
import com.thalesgroup.dtkit.junit.CppUnit;
import com.thalesgroup.dtkit.metrics.api.InputMetric;
import com.thalesgroup.dtkit.metrics.api.InputMetricFactory;
import eu.vahlas.json.schema.JSONSchema;
import eu.vahlas.json.schema.JSONSchemaProvider;
import eu.vahlas.json.schema.impl.JacksonSchemaProvider;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.Iterator;
import java.util.List;


public class InputMetricResourceTest extends JerseyTest {

    private static ClientConfig clientConfig;

    private WebResource webResource;

    private void validateInputMetricJSONXMLSchema(String jsonResult, String jsonSchemaName) {
        ObjectMapper mapper = new ObjectMapper();
        JSONSchemaProvider schemaProvider = new JacksonSchemaProvider(mapper);
        InputStream schemaIS = this.getClass().getResourceAsStream(jsonSchemaName);
        JSONSchema schema = schemaProvider.getSchema(schemaIS);
        List<String> errors = schema.validate(jsonResult);
        for (String error : errors) {
            System.out.println("[JSON Schema Validator Error]" + error);
        }
        Assert.assertTrue(errors.size() == 0);
    }

    private void validateInputMetricJSONXMLSchema(String jsonResult) {
        validateInputMetricJSONXMLSchema(jsonResult, "inputMetric-json-schema.json");
    }

    private void validateValidateMethodJSONXMLSchema(String jsonResult) {
        validateInputMetricJSONXMLSchema(jsonResult, "validateMethod-json-schema.json");
    }


    private String readContentReader(Reader reader) throws IOException {
        StringBuffer sb = new StringBuffer(1000);
        BufferedReader bufferedReader = new BufferedReader(reader);
        char[] buf = new char[1024];
        int numRead;
        while ((numRead = bufferedReader.read(buf)) != -1) {
            sb.append(buf, 0, numRead);
        }
        reader.close();
        return sb.toString();
    }

    private String readContentInputStream(InputStream inputStream) throws IOException {
        StringBuffer sb = new StringBuffer(1000);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        int c;
        while ((c = bufferedInputStream.read()) != -1) {
            sb.append((char) c);
        }
        bufferedInputStream.close();
        return sb.toString();
    }

    @BeforeClass
    public static void loadClientConfig() {
        clientConfig = new DefaultClientConfig();
        clientConfig.getClasses().add(JacksonJsonProvider.class);
        clientConfig.getClasses().add(InputMetricJSONProvider.class);
    }

    public InputMetricResourceTest() throws Exception {
        super(new WebAppDescriptor.Builder("com.thalesgroup.dtkit.ws.rs;org.codehaus.jackson.jaxrs")
                .clientConfig(clientConfig)
                .contextPath("dtkit-rs").build());
    }

    @Before
    public void loadWebResurce() {
        webResource = resource().path(InputMetrics.PATH);
    }


    @Test
    public void getInputMetricsXML() {
        InputMetricsResult inputMetricsResult =
                webResource.accept(MediaType.APPLICATION_XML)
                        .get(InputMetricsResult.class);
        Assert.assertNotNull(inputMetricsResult);
        Assert.assertEquals(InputMetrics.registry.size(), inputMetricsResult.getMetrics().size());
    }


    @Test
    public void getInputMetrics() throws IOException {
        JsonNode node = webResource.accept(MediaType.APPLICATION_JSON).get(JsonNode.class);
        Assert.assertNotNull(node);
        Assert.assertNotNull(node.isArray());
        int count = 0;
        Iterator<JsonNode> nodeIterator = node.getElements();
        while (nodeIterator.hasNext()) {
            ++count;
            nodeIterator.next();
        }
        Assert.assertEquals(InputMetrics.registry.size(), count);
    }

    @Test
    public void getInputMetricCppunitXML() throws IOException {
        String result = webResource.path("/cppunit").accept(MediaType.APPLICATION_XML).get(String.class);
        Assert.assertNotNull(result);
        System.out.println(result);
        Assert.assertEquals(
                readContentInputStream(this.getClass()
                        .getResourceAsStream("cppunit/cppunit-xml-result.xml")), result);
    }

    @Test
    public void getInputMetricCpptestJSON() throws Exception {
        String result = webResource.path("/cpptest").accept(MediaType.APPLICATION_JSON).get(String.class);
        System.out.println(result);
        Assert.assertNotNull(result);
        validateInputMetricJSONXMLSchema(result);
        InputMetric expectedCppTest = InputMetricFactory.getInstance(CppTest.class);
        ObjectMapper objectMapper = new ObjectMapper();
        CppTest actualCppTest = objectMapper.readValue(result, CppTest.class);
        Assert.assertTrue(expectedCppTest.equals(actualCppTest));
    }


    @Test
    public void getInputMetricCppunitJSON() throws Exception {
        String result = webResource.path("/cppunit").accept(MediaType.APPLICATION_JSON).get(String.class);
        Assert.assertNotNull(result);
        validateInputMetricJSONXMLSchema(result);
        Assert.assertNotNull(result);
        validateInputMetricJSONXMLSchema(result);
        InputMetric expectedCppunit = InputMetricFactory.getInstance(CppUnit.class);
        ObjectMapper objectMapper = new ObjectMapper();
        CppUnit actualCppunit = objectMapper.readValue(result, CppUnit.class);
        Assert.assertTrue(expectedCppunit.equals(actualCppunit));
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
        JsonNode node = webResource.path("/cppunit/validate")
                .type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_JSON)
                .post(JsonNode.class, this.getClass().getResourceAsStream("cppunit/cppunit-valid-input.xml"));
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
        FileReader cppunitJunitFileReader = new FileReader(cppunitJunitFile);
        Assert.assertEquals(readContentInputStream(this.getClass().getResourceAsStream("cppunit/cppunit-valid-junit-result.xml")), readContentReader(cppunitJunitFileReader));
        cppunitJunitFileReader.close();
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

}
