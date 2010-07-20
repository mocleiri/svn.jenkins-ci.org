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

import com.thalesgroup.dtkit.util.converter.ConvertException;
import com.thalesgroup.dtkit.util.validator.ValidatorError;
import com.thalesgroup.dtkit.util.validator.ValidatorException;

import java.io.File;
import java.io.Serializable;
import java.util.List;


@SuppressWarnings("unused")
public abstract class InputMetric implements Serializable {

    /**
     * The current input validation errors
     */
    private List<ValidatorError> inputValidationErrors;

    /**
     * The current output validation errors
     */
    private List<ValidatorError> outputValidationErrors;

    /**
     * The  name of the current tool
     *
     * @return the tool name
     */
    public abstract String getToolName();


    /**
     * The version of the current tool
     *
     * @return the tool version
     */
    public abstract String getToolVersion();


    /**
     * The label of the tool
     *
     * @return the label metric
     */
    public String getLabel() {
        return getToolName() + "-" + getToolVersion();
    }

    /**
     * Gives the input metric type (XSL or Other) according the subclass type
     *
     * @return the input metric type
     */
    public abstract InputMetricType getInputMetricType();

    /**
     * Gives the metric tool type (TEST, COVERAGE, MEASURE, VIOLATION)
     *
     * @return the input object
     */
    public abstract InputType getToolType();

    /**
     * Gives the output format type (given by the format model)
     *
     * @return the Output format type (usually retrieved by the format model library as junit-model.jar or tusar-model.jar)
     */
    public abstract OutputMetric getOutputFormatType();

    /**
     * Convert an input file to an output file
     * Give your conversion process
     * Input and Output files are relatives to the filesystem where the process is executed on (like Hudson agent)
     *
     * @param inputFile the input file to convert
     * @param outFile   the output file to convert
     * @throws ConvertException an application Exception to throw when there is an error of conversion
     *                          The exception is catched by the API client (as Hudson plugin)
     */
    public abstract void convert(File inputFile, File outFile) throws ConvertException;


    /*
     *  Gives the validation process for the input file
     *
     * @return true if the input file is valid, false otherwise
     */
    public abstract boolean validateInputFile(File inputXMLFile) throws ValidatorException;

    /*
     *  Gives the validation process for the output file
     *
     * @return true if the input file is valid, false otherwise
     */
    public abstract boolean validateOutputFile(File inputXMLFile) throws ValidatorException;


    /**
     * Gets all input validation errors
     *
     * @return the list of all input validation errors
     */
    public List<ValidatorError> getInputValidationErrors() {
        return inputValidationErrors;
    }

    /**
     * Gets all output validation errors
     *
     * @return the list of all output validation errors
     */
    public List<ValidatorError> getOutputValidationErrors() {
        return outputValidationErrors;
    }

    public void setInputValidationErrors(List<ValidatorError> inputValidationErrors) {
        this.inputValidationErrors = inputValidationErrors;
    }

    public void setOutputValidationErrors(List<ValidatorError> outputValidationErrors) {
        this.outputValidationErrors = outputValidationErrors;
    }
}
