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

package com.thalesgroup.dtkit.metrics.model;

import com.google.inject.Inject;
import com.thalesgroup.dtkit.util.converter.ConversionException;
import com.thalesgroup.dtkit.util.converter.ConversionService;
import com.thalesgroup.dtkit.util.validator.ValidationException;
import com.thalesgroup.dtkit.util.validator.ValidationService;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings("unused")
public abstract class InputMetricXSL extends InputMetric {


    private String xslName;
    private File xslFile;

    private String inputXsdName;
    private File xsdFile;

    /**
     * -----------------------------------------
     * The business services
     */
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
    @JsonIgnore
    public Class getXslResourceClass() {
        return this.getClass();
    }

    /**
     * Gets the Class (namespace) of the xsd file resource
     *
     * @return the xsd class (for loading)
     */
    @JsonIgnore
    public Class getInputXsdClass() {
        return this.getClass();
    }

    /**
     * the XSL file associated to this tool
     *
     * @return the relative xsl path
     */
    @JsonIgnore
    public String getXslName() {
        return xslName;
    }


    /**
     * Overrides this method if you want to provide your own xsl (independent of its location)
     * Used for custom type where the user specifies its own xsl
     *
     * @return null by default
     */
    @JsonIgnore
    public File getXslFile() {
        return xslFile;
    }

    @JsonIgnore
    public InputStream getXslInputStream() throws IOException {

        if (getXslFile() != null) {
            return new FileInputStream(getXslFile());
        }

        if (this.getXslName() != null) {
            return this.getXslResourceClass().getResourceAsStream(this.getXslName());
        }

        return null;
    }

    /**
     * the XSD file associated to this tool result file
     *
     * @return the xsd name. Can be null if there no XSD for the input file of the current tool type
     */
    @JsonIgnore
    public String getInputXsdName() {
        return inputXsdName;
    }

    @JsonIgnore
    public File getXsdFile() {
        return xsdFile;
    }

    @JsonIgnore
    public InputStream getXsdInputStream() throws IOException {

        if (getXsdFile() != null) {
            return new FileInputStream(getXsdFile());
        }

        if (this.getInputXsdName() != null) {
            return this.getInputXsdClass().getResourceAsStream(this.getInputXsdName());
        }

        return null;
    }

    /**
     * the XSD file associated to this output format
     *
     * @return the relative xsd path. Can be null if there no XSD for the output format
     */
    @JsonIgnore
    public String getOutputXsd() {
        return getOutputFormatType().getXsdName();
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
        if (getXslFile() == null) {
            conversionService.convert(new StreamSource(this.getXslResourceClass().getResourceAsStream(this.getXslName())), inputFile, outFile);
        } else {
            conversionService.convert(getXslFile(), inputFile, outFile);
        }
    }


    /*
     *  Validates the input file against the current grammar of the tool
     */

    @Override
    public boolean validateInputFile(File inputXMLFile) throws ValidationException {

        if (this.getInputXsdName() == null) {
            return true;
        }

        setInputValidationErrors(validationService.processValidation(new StreamSource(this.getInputXsdClass().getResourceAsStream(this.getInputXsdName())), inputXMLFile));
        return getInputValidationErrors().size() == 0;
    }

    /*
     *  Validates the output file against the current grammar of the format
     */
    @Override
    public boolean validateOutputFile(File inputXMLFile) throws ValidationException {

        if (this.getOutputXsd() == null) {
            return true;
        }

        setOutputValidationErrors(validationService.processValidation(new StreamSource(this.getOutputFormatType().getClass().getResourceAsStream(this.getOutputXsd())), inputXMLFile));
        return getOutputValidationErrors().size() == 0;
    }

    /**
     * --------------------------------------------------------
     * <p/>
     * HASHCODE() AND EQUALS() for comparaison
     * <p/>
     * --------------------------------------------------------
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        InputMetricXSL that = (InputMetricXSL) o;

        if (getInputXsdName() != null ? !getInputXsdName().equals(that.getInputXsdName()) : that.getInputXsdName() != null)
            return false;
        if (getXslFile() != null ? !getXslFile().equals(that.getXslFile()) : that.getXslFile() != null) return false;
        if (getXslName() != null ? !getXslName().equals(that.getXslName()) : that.getXslName() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getXslName() != null ? getXslName().hashCode() : 0);
        result = 31 * result + (getXslFile() != null ? getXslFile().hashCode() : 0);
        result = 31 * result + (getInputXsdName() != null ? getInputXsdName().hashCode() : 0);
        return result;
    }
}
