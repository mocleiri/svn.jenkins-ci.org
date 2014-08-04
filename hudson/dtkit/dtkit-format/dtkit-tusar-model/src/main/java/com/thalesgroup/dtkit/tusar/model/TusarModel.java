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

package com.thalesgroup.dtkit.tusar.model;

import com.thalesgroup.dtkit.metrics.model.OutputMetric;
import com.thalesgroup.dtkit.util.validator.ValidationService;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;


@SuppressWarnings("unused")
public class TusarModel implements Serializable {

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_1_0 = new Tusarv1() ;

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_2_0 = new Tusarv2() ;

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_3_0 = new Tusarv3() ;


    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_4_0 = new Tusarv4() ;

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_5_0 = new Tusarv5() ;


    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_6_0 = new Tusarv6() ;

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_7_0 = new Tusarv7() ;

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_8_0 = new Tusarv8() ;

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_9_0 = new Tusarv9() ;

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_10_0 = new Tusarv10() ;


    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_11_0 = new Tusarv11() ;

    public static OutputMetric LATEST = OUTPUT_TUSAR_11_0;

    public static List<OutputMetric> getAllTUSAROutput() {
        return Arrays.asList(new OutputMetric[]{
                OUTPUT_TUSAR_1_0,
                OUTPUT_TUSAR_2_0,
                OUTPUT_TUSAR_3_0,
                OUTPUT_TUSAR_4_0,
                OUTPUT_TUSAR_5_0,
                OUTPUT_TUSAR_6_0,
                OUTPUT_TUSAR_7_0,
                OUTPUT_TUSAR_8_0,
                OUTPUT_TUSAR_9_0,
                OUTPUT_TUSAR_10_0,
                OUTPUT_TUSAR_11_0
        });
    }

}
