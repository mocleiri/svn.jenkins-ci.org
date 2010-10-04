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
import javax.xml.transform.Source;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@SuppressWarnings("unused")
public abstract class InputMetricXSL extends InputMetric {


    private String xslName;
    private File xslFile;

    private String[] inputXsdNameList;
    private File[] inputXsdFileList;

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
    public String[] getInputXsdNameList() {
        return inputXsdNameList;
    }

    @JsonIgnore
    public File[] getInputXsdFileList() {
        return inputXsdFileList;
    }

    @JsonIgnore
    public InputStream[] getListXsdInputStream() throws IOException {

        File[] inputXsdFileList = getInputXsdFileList();
        if (inputXsdFileList != null) {
            InputStream[] inputStreams = new InputStream[inputXsdFileList.length];
            for (int i = 0; i < inputXsdFileList.length; i++) {
                inputStreams[i] = new FileInputStream(inputXsdFileList[i]);
            }
            return inputStreams;
        }

        String[] inputXsdNameList = getInputXsdNameList();
        if (inputXsdNameList != null) {
            InputStream[] inputStreams = new InputStream[inputXsdNameList.length];
            for (int i = 0; i < inputXsdNameList.length; i++) {
                inputStreams[i] = this.getInputXsdClass().getResourceAsStream(inputXsdNameList[i]);
            }
            return inputStreams;
        }

        return null;
    }

    /**
     * the XSD file associated to this output format
     *
     * @return the relative xsd path. Can be null if there no XSD for the output format
     */
    @JsonIgnore
    public String[] getOutputXsdNameList() {
        return getOutputFormatType().getXsdNameList();
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

        if (this.getInputXsdNameList() == null) {
            return true;
        }

        StreamSource[] streamSources = new StreamSource[getInputXsdNameList().length];
        for (int i = 0; i < streamSources.length; i++) {
            streamSources[i] = new StreamSource(this.getInputXsdClass().getResourceAsStream(getInputXsdNameList()[i]));
        }

        setInputValidationErrors(validationService.processValidation(streamSources, inputXMLFile));
        
        return getInputValidationErrors().size() == 0;
    }

    /*
     *  Validates the output file against the current grammar of the format
     */
    @Override
    public boolean validateOutputFile(File inputXMLFile) throws ValidationException {

        if (validationService!=null){
            return true;
        }

        if (this.getOutputXsdNameList() == null) {
            return true;
        }

        Source[] sources = new Source[getOutputXsdNameList().length];
        for (int i = 0; i < sources.length; i++) {
            sources[i] = new StreamSource(this.getOutputFormatType().getClass().getResourceAsStream(getOutputXsdNameList()[i]));
        }

        setOutputValidationErrors(validationService.processValidation(sources, inputXMLFile));
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

        if (!Arrays.equals(inputXsdFileList, that.inputXsdFileList)) return false;
        if (!Arrays.equals(inputXsdNameList, that.inputXsdNameList)) return false;
        if (xslFile != null ? !xslFile.equals(that.xslFile) : that.xslFile != null) return false;
        if (xslName != null ? !xslName.equals(that.xslName) : that.xslName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (xslName != null ? xslName.hashCode() : 0);
        result = 31 * result + (xslFile != null ? xslFile.hashCode() : 0);
        result = 31 * result + (inputXsdNameList != null ? Arrays.hashCode(inputXsdNameList) : 0);
        result = 31 * result + (inputXsdFileList != null ? Arrays.hashCode(inputXsdFileList) : 0);
        return result;
    }
}
