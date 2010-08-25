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

package com.thalesgroup.dtkit.util.validator;

import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class ValidationService implements Serializable {


    /**
     * Validate an input file against a XSD
     *
     * @param xsdNamespace the class of the xsd
     * @param xsdName       the xsd name
     * @param inputXML      the input XML file
     * @return true if the validation succeeded, false otherwise
     * @throws ValidationException when there is a validation error
     */
    public List<ValidationError> processValidation(Class xsdNamespace, String xsdName, File inputXML) throws ValidationException {

        if (xsdName == null) {
            return new ArrayList<ValidationError>();
        }

        ValidationHandler handler = new ValidationHandler();
        try {

            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            schemaFactory.setErrorHandler(handler);
            Schema schemaGrammar = schemaFactory.newSchema(xsdNamespace.getResource(xsdName));
            //Resolver resolver = new Resolver();
            Validator schemaValidator = schemaGrammar.newValidator();
            //schemaValidator.setResourceResolver(resolver);
            schemaValidator.setErrorHandler(handler);
            schemaValidator.validate(new StreamSource(inputXML));

            return handler.getErrors();
        }

        catch (SAXException sae) {
            return handler.getErrors();
        }
        catch (IOException ioe) {
            throw new ValidationException("Validation error", ioe);
        }
    }
}