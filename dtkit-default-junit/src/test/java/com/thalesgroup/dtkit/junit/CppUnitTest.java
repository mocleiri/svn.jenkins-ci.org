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

package com.thalesgroup.dtkit.junit;

import org.junit.Test;


public class CppUnitTest extends AbstractTest {

    @Test
    public void cppunitTestcase1() throws Exception {
        convertAndValidate(CppUnit.class, "cppunit/testcase1/cppunit-successAndFailure.xml", "cppunit/testcase1/junit-result.xml");
    }

    @Test
    public void cppunitTestcase2() throws Exception {
        convertAndValidate(CppUnit.class, "cppunit/testcase2/cppunit-zeroFailure.xml", "cppunit/testcase2/junit-result.xml");
    }

    @Test
    public void cppunitTestcase3() throws Exception {
        convertAndValidate(CppUnit.class, "cppunit/testcase3/cppunit-zeroFailureAndSuccess.xml", "cppunit/testcase3/junit-result.xml");
    }

    @Test
    public void cppunitTestcase4() throws Exception {
        convertAndValidate(CppUnit.class, "cppunit/testcase4/cppunit-zeroSuccess.xml", "cppunit/testcase4/junit-result.xml");
    }
}
