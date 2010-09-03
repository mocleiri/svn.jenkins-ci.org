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

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.embedded.glassfish.EmbeddedGlassFishTestContainerFactory;
import com.thalesgroup.dtkit.ws.rs.providers.InputMetricJSONProvider;
import com.thalesgroup.dtkit.ws.rs.services.InputMetricsLocator;
import eu.vahlas.json.schema.JSONSchema;
import eu.vahlas.json.schema.JSONSchemaProvider;
import eu.vahlas.json.schema.impl.JacksonSchemaProvider;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.io.*;
import java.util.List;


public abstract class InputMetricsAbstractTest extends JerseyTest {

    private static ClientConfig clientConfig;

    protected WebResource webResource;

    protected InputMetricsLocator inputMetricsLocator;

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new EmbeddedGlassFishTestContainerFactory();
    }

    @BeforeClass
    public static void loadClientConfig() {
        clientConfig = new DefaultClientConfig();
        clientConfig.getClasses().add(JacksonJsonProvider.class);
        clientConfig.getClasses().add(InputMetricJSONProvider.class);
    }

    public InputMetricsAbstractTest() throws TestContainerException {
        super(new WebAppDescriptor.Builder("com.thalesgroup.dtkit.ws.rs;org.codehaus.jackson.jaxrs")
                .clientConfig(clientConfig)
                        //.contextListenerClass(com.thalesgroup.dtkit.ws.rs.services.GuiceConfig.class)
                        //.filterClass(com.google.inject.servlet.GuiceFilter.class)
                .contextPath("dtkit-rs").build());

        inputMetricsLocator = new InputMetricsLocator();//Guice.createInjector().getInstance(InputMetricsLocator.class);
    }

    protected void validateInputMetricJSONXMLSchema(String jsonResult, String jsonSchemaName) {
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

    protected void validateInputMetricJSONXMLSchema(String jsonResult) {
        validateInputMetricJSONXMLSchema(jsonResult, "inputMetric-json-schema.json");
    }

    protected void validateValidateMethodJSONXMLSchema(String jsonResult) {
        validateInputMetricJSONXMLSchema(jsonResult, "validateMethod-json-schema.json");
    }

    protected String readContentReader(Reader reader) throws IOException {
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

    protected String readContentInputStream(InputStream inputStream) throws IOException {
        StringBuffer sb = new StringBuffer(1000);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        int c;
        while ((c = bufferedInputStream.read()) != -1) {
            sb.append((char) c);
        }
        bufferedInputStream.close();
        return sb.toString();
    }
}
