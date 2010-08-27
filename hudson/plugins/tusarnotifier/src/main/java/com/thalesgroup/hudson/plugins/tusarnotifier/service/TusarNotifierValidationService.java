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

package com.thalesgroup.hudson.plugins.tusarnotifier.service;

import com.google.inject.Inject;
import com.thalesgroup.dtkit.metrics.api.InputMetric;
import com.thalesgroup.dtkit.util.validator.ValidationError;
import com.thalesgroup.dtkit.util.validator.ValidationException;
import com.thalesgroup.hudson.plugins.tusarnotifier.exception.TusarNotifierException;
import com.thalesgroup.hudson.plugins.tusarnotifier.transformer.TusarToolInfo;

import java.io.File;
import java.io.Serializable;


public class TusarNotifierValidationService implements Serializable {

    private TusarNotifierLog tusarNotifierLog;

    @Inject
    @SuppressWarnings("unused")
    void load(TusarNotifierLog xUnitLog) {
        this.tusarNotifierLog = xUnitLog;
    }

    /**
     * Checks if the current input file is not empty
     *
     * @param inputFile the input file
     * @return true if not empty, false otherwise
     */
    public boolean checkFileIsNotEmpty(File inputFile) {
        return inputFile.length() != 0;
    }

    /**
     * Validates an input file
     *
     * @param tusarToolInfo the tusar tool tool info wrapper
     * @param inputFile     the current input file
     * @return true if the validation is success, false otherwise
     * @throws TusarNotifierException com.thalesgroup.hudson.plugins.tusarnotifier.exception.TusarNotifierException
     *                                an XUnitException when there are validation exceptions
     */
    public boolean validateInputFile(TusarToolInfo tusarToolInfo, File inputFile) throws TusarNotifierException {

        InputMetric inputMetric = tusarToolInfo.getMetricsType().getInputMetric();

        //Validates the input file (nom empty)
        try {
            if (!inputMetric.validateInputFile(inputFile)) {

                //Ignores invalid files
                tusarNotifierLog.warning("The file '" + inputFile + "' is an invalid file.");
                for (ValidationError validatorError : inputMetric.getInputValidationErrors()) {
                    tusarNotifierLog.warning(validatorError.toString());
                }

                return false;
            }
        } catch (ValidationException ve) {
            throw new TusarNotifierException("Validation error on input", ve);
        }
        return true;
    }


    /**
     * Validates the converted file against a JUnit format
     *
     * @param tusarToolInfo   the tusar tool info wrapper object
     * @param inputFile       the input metric from the conversion
     * @param junitTargetFile the converted input file
     * @return true if the validation is success, false otherwise
     * @throws TusarNotifierException com.thalesgroup.hudson.plugins.tusarnotifier.exception.TusarNotifierException
     */
    public boolean validateOutputFile(TusarToolInfo tusarToolInfo, File inputFile, File junitTargetFile) throws TusarNotifierException {
        InputMetric inputMetric = tusarToolInfo.getMetricsType().getInputMetric();

        try {
            //Validates the output
            boolean validateOutput = inputMetric.validateOutputFile(junitTargetFile);
            if (!validateOutput) {
                tusarNotifierLog.error("The converted file for the input file '" + inputFile + "' doesn't match the TUSAR format");
                for (ValidationError validatorError : inputMetric.getOutputValidationErrors()) {
                    tusarNotifierLog.error(validatorError.toString());
                }
                return false;
            }

        }
        catch (ValidationException ve) {
            throw new TusarNotifierException("Validation error on output", ve);
        }

        return true;
    }
}
