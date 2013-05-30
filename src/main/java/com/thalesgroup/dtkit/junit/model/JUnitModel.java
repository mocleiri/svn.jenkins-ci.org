/*******************************************************************************
 * Copyright (c) 2010-2011 Thales Corporate Services SAS                        *
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

package com.thalesgroup.dtkit.junit.model;

import com.thalesgroup.dtkit.metrics.model.OutputMetric;
import com.thalesgroup.dtkit.util.validator.ValidationService;

import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
@SuppressWarnings("unused")
public class JUnitModel implements Serializable {

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_JUNIT_1_0 = new JUnit1() {
        {
            set(new ValidationService());
        }
    };

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_JUNIT_2 = new JUnit2() {
        {
            set(new ValidationService());
        }
    };

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_JUNIT_3 = new JUnit3() {
        {
            set(new ValidationService());
        }
    };

    @SuppressWarnings("unused")
    public static JUnit4 OUTPUT_JUNIT_4 = new JUnit4() {
        {
            set(new ValidationService());
        }
    };

    @SuppressWarnings("unused")
    public static JUnit5 OUTPUT_JUNIT_5 = new JUnit5() {
        {
            set(new ValidationService());
        }
    };

    @SuppressWarnings("unused")
    public static JUnit6 OUTPUT_JUNIT_6 = new JUnit6() {
        {
            set(new ValidationService());
        }
    };

}
