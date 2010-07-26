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

package com.thalesgroup.hudson.plugins.xunit.service;

import com.google.inject.Inject;
import com.thalesgroup.dtkit.metrics.api.InputMetric;
import com.thalesgroup.dtkit.util.converter.ConvertException;
import com.thalesgroup.hudson.plugins.xunit.exception.XUnitException;
import com.thalesgroup.hudson.plugins.xunit.transformer.XUnitToolInfo;
import com.thalesgroup.hudson.plugins.xunit.util.XUnitLog;
import hudson.model.BuildListener;

import java.io.File;


public class XUnitConversionService {

    @Inject
    private BuildListener buildListener;

    /**
     * Converts the inputFile into a JUnit output file
     *
     * @param xUnitToolInfo        the xUnit info wrapper object
     * @param inputFile            the input file to be converted
     * @param junitOutputDirectory the output parent directory that contains the JUnit output file
     * @return the converted file
     * @throws com.thalesgroup.hudson.plugins.xunit.exception.XUnitException
     *          an XUnitException is thrown if there is a convertion error.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public File convert(XUnitToolInfo xUnitToolInfo, File inputFile, File junitOutputDirectory) throws XUnitException {

        InputMetric inputMetric = xUnitToolInfo.getTestType().getInputMetric();

        final String JUNIT_FILE_POSTFIX = ".xml";
        final String JUNIT_FILE_PREFIX = "TEST-";
        File parent = new File(junitOutputDirectory, inputMetric.getToolName());
        parent.mkdirs();
        if (!parent.exists()) {
            throw new XUnitException("Can't create " + parent);
        }
        File junitTargetFile = new File(parent, JUNIT_FILE_PREFIX + inputFile.hashCode() + JUNIT_FILE_POSTFIX);
        XUnitLog.log(buildListener, "[INFO] - Converting '" + inputFile + "' .");
        try {
            inputMetric.convert(inputFile, junitTargetFile);
        } catch (ConvertException ce) {
            throw new XUnitException("Conversion error", ce);
        }

        return junitTargetFile;
    }
}
