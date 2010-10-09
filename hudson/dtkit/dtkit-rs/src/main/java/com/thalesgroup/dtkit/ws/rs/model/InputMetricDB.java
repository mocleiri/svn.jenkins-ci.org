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

package com.thalesgroup.dtkit.ws.rs.model;

import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Serialized;
import com.thalesgroup.dtkit.junit.model.JUnitModel;
import com.thalesgroup.dtkit.metrics.model.InputMetricXSL;
import com.thalesgroup.dtkit.metrics.model.OutputMetric;
import com.thalesgroup.dtkit.tusar.model.TusarModel;
import com.thalesgroup.dtkit.util.converter.ConversionException;
import com.thalesgroup.dtkit.util.converter.ConversionService;
import com.thalesgroup.dtkit.util.validator.ValidationException;
import com.thalesgroup.dtkit.util.validator.ValidationService;
import org.bson.types.ObjectId;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringReader;


public class InputMetricDB extends InputMetricXSL {

    @Id
    private ObjectId id;

    @Serialized
    private Object xslContent;

    @Serialized
    private Object xsdContent;

    private String outputFormat;

    @JsonIgnore
    public ObjectId getId() {
        return id;
    }

    @JsonIgnore
    public Object getXslContent() {
        return xslContent;
    }

    public void setXslContent(Object xslContent) {
        this.xslContent = xslContent;
    }

    @JsonIgnore
    public Object getXsdContent() {
        return xsdContent;
    }

    public void setXsdContent(Object xsdContent) {
        this.xsdContent = xsdContent;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public void setOutputFormatType(OutputMetric outputFormatType) {
        super.setOutputFormatType(outputFormatType);
    }

    @JsonIgnore
    @Override
    public OutputMetric getOutputFormatType() {
        String outputFormat = null;
        if ((outputFormat = getOutputFormat()) != null) {
            if (JUnitModel.OUTPUT_JUNIT_1_0.getKey().equalsIgnoreCase(outputFormat)) {
                setOutputFormatType(JUnitModel.OUTPUT_JUNIT_1_0);
            } else {
                setOutputFormatType(TusarModel.OUTPUT_TUSAR_1_0);
            }
        }

        return super.getOutputFormatType();
    }

    @Override
    public void convert(File inputFile, File outFile) throws ConversionException {
        new ConversionService().convert(new StreamSource(new StringReader(String.valueOf(getXslContent()))), inputFile, outFile);
    }

    @Override
    public boolean validateInputFile(File inputXMLFile) throws ValidationException {
        if (this.getXsdContent() == null) {
            return true;
        }
        setInputValidationErrors(new ValidationService().processValidation(new StreamSource(new StringReader(String.valueOf(getXsdContent()))), inputXMLFile));
        return getInputValidationErrors().size() == 0;
    }

}
