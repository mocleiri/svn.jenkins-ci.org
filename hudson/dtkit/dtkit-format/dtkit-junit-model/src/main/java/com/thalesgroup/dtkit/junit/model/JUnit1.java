/*******************************************************************************
 * Copyright (c) 2010 Thales Corporate Services SAS                             *
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

import com.thalesgroup.dtkit.metrics.model.AbstractOutputMetric;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class JUnit1 extends AbstractOutputMetric implements Serializable {

    @Override
    @XmlElement
    public String getKey() {
        return "junit";
    }

    @Override
    @XmlElement
    public String getDescription() {
        return "JUNIT OUTPUT FORMAT 1.0";
    }

    @Override
    @XmlElement
    public String getVersion() {
        return "1.0";
    }

    @Override
    @XmlElement
    public String[] getXsdNameList() {
        return new String[]{"xsd/junit-1.0.xsd"};
    }
}
