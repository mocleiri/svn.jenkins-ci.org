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

package com.thalesgroup.dtkit.junit.model;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.thalesgroup.dtkit.metrics.model.AbstractOutputMetric;
import com.thalesgroup.dtkit.metrics.model.OutputMetric;
import com.thalesgroup.dtkit.util.validator.ValidationService;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;


@SuppressWarnings("unused")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class JUnitModel extends AbstractOutputMetric implements Serializable {

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_JUNIT_1_0 = Guice.createInjector(new AbstractModule() {
        @Override
        protected void configure() {
            bind(ValidationService.class);
        }
    }).getInstance(JUnitModel.class);

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
