/*******************************************************************************
 * Copyright (c) 2009 Thales Corporate Services SAS                             *
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

package com.thalesgroup.hudson.plugins.cppcheck.util;

import hudson.model.HealthReport;

import com.thalesgroup.hudson.plugins.cppcheck.CppcheckConfig;
import com.thalesgroup.hudson.plugins.cppcheck.CppcheckMetricUtil;

public class CppcheckBuildHealthEvaluator {

    public HealthReport evaluatBuildHealth(CppcheckConfig cppcheckConfig, int nbErrorForSeverity ) {       
    	
    	if (cppcheckConfig == null) {
            // no thresholds => no report
            return null;
        }

        if (isHealthyReportEnabled(cppcheckConfig)) {
            int percentage;
            int counter =  nbErrorForSeverity;
            
            if (counter < CppcheckMetricUtil.convert(cppcheckConfig.getHealthy())) {
                percentage = 100;
            }
            else if (counter > CppcheckMetricUtil.convert(cppcheckConfig.getUnHealthy())) {
                percentage = 0;
            }
            else {
                percentage = 100 - ((counter - CppcheckMetricUtil.convert(cppcheckConfig.getHealthy())) * 100
                        / (CppcheckMetricUtil.convert(cppcheckConfig.getUnHealthy()) - CppcheckMetricUtil.convert(cppcheckConfig.getHealthy())));
            }
            
            //TODO MALE THE message error
            return new HealthReport(percentage, "Build stability for checked severities severity.");
        }
        return null;
    }
    
    
     private boolean isHealthyReportEnabled(CppcheckConfig cppcheckconfig) {
        if (CppcheckMetricUtil.isValid(cppcheckconfig.getHealthy()) && CppcheckMetricUtil.isValid(cppcheckconfig.getUnHealthy())) {
            int healthyNumber = CppcheckMetricUtil.convert(cppcheckconfig.getHealthy());
            int unHealthyNumber = CppcheckMetricUtil.convert(cppcheckconfig.getUnHealthy());
            return unHealthyNumber > healthyNumber;
        }
        return false;
    }
}
