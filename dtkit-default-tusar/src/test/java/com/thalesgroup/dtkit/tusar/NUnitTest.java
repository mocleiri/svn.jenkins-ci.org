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

package com.thalesgroup.dtkit.tusar;

import org.junit.Test;


public class NUnitTest extends AbstractTest {


    @Test
    public void testTransformation() throws Exception {
        convertAndValidate(NUnit.class, "nunit/NUnit-simple.xml", "nunit/tusar-simple.xml");
    }

    @Test
    public void testTransformationFailure() throws Exception {
        convertAndValidate(NUnit.class, "nunit/NUnit-failure.xml", "nunit/tusar-failure.xml");
    }

    @Test
    public void testTransformationMultiNamespace() throws Exception {
        convertAndValidate(NUnit.class, "nunit/NUnit-multinamespace.xml", "nunit/tusar-multinamespace.xml");
    }

    @Test
    public void testTransformedIgnored() throws Exception {
        convertAndValidate(NUnit.class, "nunit/NUnit-ignored.xml", "nunit/tusar-ignored.xml");
    }

    @Test
    public void testTransformedIssue1077() throws Exception {
        convertAndValidate(NUnit.class, "nunit/NUnit-issue1077.xml", "nunit/tusar-issue1077.xml");
    }

}
