/*******************************************************************************
 * Copyright (c) 2010 Thales Corporate Services SAS                             *
 * Author : Gregory Boissinot, Guillaume Tanier                                 *
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
public class TusarModel implements Serializable {

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_1_0 = Guice.createInjector(new AbstractModule() {
        @Override
        protected void configure() {
            bind(ValidationService.class);
        }
    }).getInstance(TusarModel0.class);

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_1_1 = Guice.createInjector(new AbstractModule() {
        @Override
        protected void configure() {
            bind(ValidationService.class);
        }
    }).getInstance(TusarModel1.class);

    public static class TusarModel0 extends AbstractOutputMetric implements Serializable {

        @Override
        @XmlElement
        public String getKey() {
            return "tusar";
        }

        @Override
        @XmlElement
        public String getDescription() {
            return "TUSAR OUTPUT FORMAT 1.0";
        }

        @Override
        @XmlElement
        public String getVersion() {
            return "1.0";
        }

        @Override
        @XmlElement
        public String[] getXsdNameList() {
            return new String[]{"xsd/tusar-1.0.xsd"};
        }
    }

    public static class TusarModel1 extends AbstractOutputMetric implements Serializable {

        @Override
        @XmlElement
        public String getKey() {
            return "tusar";
        }

        @Override
        @XmlElement
        public String getDescription() {
            return "TUSAR OUTPUT FORMAT 1.1";
        }

        @Override
        @XmlElement
        public String getVersion() {
            return "1.1";
        }

        @Override
        @XmlElement
        public String[] getXsdNameList() {
            return new String[]{"xsd/tests-1.1.xsd", "xsd/coverage-1.1.xsd", "xsd/violations-1.1.xsd", "xsd/measures-1.1.xsd", "xsd/tusar-1.1.xsd"};
        }
    }


}
