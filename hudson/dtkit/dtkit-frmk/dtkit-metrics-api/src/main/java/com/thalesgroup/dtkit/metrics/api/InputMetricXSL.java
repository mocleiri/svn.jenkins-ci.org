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

package com.thalesgroup.dtkit.metrics.api;

import com.google.inject.Inject;
import com.thalesgroup.dtkit.util.converter.ConversionException;
import com.thalesgroup.dtkit.util.converter.ConversionService;
import com.thalesgroup.dtkit.util.validator.ValidationException;
import com.thalesgroup.dtkit.util.validator.ValidationService;

import javax.xml.bind.annotation.XmlElement;
import java.io.File;

@SuppressWarnings("unused")
public abstract class InputMetricXSL extends InputMetric {


    private ConversionService conversionService;

    private ValidationService validationService;


    @Inject
    void set(ConversionService conversionService, ValidationService validationService) {
        this.conversionService = conversionService;
        this.validationService = validationService;
    }

    /**
     * Gets the Class (namespace) of the xsl file resource
     *
     * @return the resource class (for loading)
     */
    public Class getXSLResourceClass() {
        return this.getClass();
    }

    /**
     * Gets the Class (namespace) of the xsd file resource
     *
     * @return the xsd class (for loading)
     */
    public Class getInputXSDClass() {
        return this.getClass();
    }

    /**
     * the XSL file associated to this tool
     *
     * @return the relative xsl path
     */
    @XmlElement
    public abstract String getXslName();

    /**
     * the XSD file associated to this tool result file
     *
     * @return the relative xsd path. Can be null if there no XSD for the input file of the current tool type
     */
    @XmlElement
    public abstract String getInputXsd();

    /**
     * the XSD file associated to this output format
     *
     * @return the relative xsd path. Can be null if there no XSD for the output format
     */
    public String getOutputXsd() {
        return getOutputFormatType().getXsd();
    }

    /**
     * All the subclasses will be of XSL
     *
     * @return XSL type
     */
    @Override
    public InputMetricType getInputMetricType() {
        return InputMetricType.XSL;
    }

    /*
     *  Convert the input file against the current xsl of the tool and put the result in the outFile
     */

    @Override
    public void convert(File inputFile, File outFile) throws ConversionException {
        conversionService.convert(this.getXSLResourceClass(), this.getXslName(), inputFile, outFile);
    }


    /*
     *  Validates the input file against the current grammar of the tool
     */

    @Override
    public boolean validateInputFile(File inputXMLFile) throws ValidationException {
        setInputValidationErrors(validationService.processValidation(this.getInputXSDClass(), this.getInputXsd(), inputXMLFile));
        return getInputValidationErrors().size() == 0;
    }

    /*
     *  Validates the output file against the current grammar of the format
     */

    @Override
    public boolean validateOutputFile(File inputXMLFile) throws ValidationException {
        setOutputValidationErrors(validationService.processValidation(this.getOutputFormatType().getClass(), this.getOutputXsd(), inputXMLFile));
        return getOutputValidationErrors().size() == 0;
    }


}
