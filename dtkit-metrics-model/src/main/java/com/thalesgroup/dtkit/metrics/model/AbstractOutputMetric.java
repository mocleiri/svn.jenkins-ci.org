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

package com.thalesgroup.dtkit.metrics.model;

import com.google.inject.Inject;
import com.thalesgroup.dtkit.util.validator.ValidationError;
import com.thalesgroup.dtkit.util.validator.ValidationException;
import com.thalesgroup.dtkit.util.validator.ValidationService;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOutputMetric implements OutputMetric {

    private ValidationService validationService;

    @Inject
    void set(ValidationService validationService) {
        this.validationService = validationService;
    }

    static class Adapter extends XmlAdapter<AbstractOutputMetric, OutputMetric> {
        public OutputMetric unmarshal(AbstractOutputMetric v) {
            return v;
        }

        public AbstractOutputMetric marshal(OutputMetric v) {
            return (AbstractOutputMetric) v;
        }
    }

    @Override
    public List<ValidationError> validate(File inputXMLFile) throws ValidationException {

        if (this.getXsdNameList() == null) {
            return new ArrayList<ValidationError>();
        }

        StreamSource[] streamSources = new StreamSource[getXsdNameList().length];
        for (int i = 0; i < streamSources.length; i++) {
            streamSources[i] = new StreamSource(this.getClass().getResourceAsStream(getXsdNameList()[i]));
        }

        return validationService.processValidation(streamSources, inputXMLFile);
    }
}
