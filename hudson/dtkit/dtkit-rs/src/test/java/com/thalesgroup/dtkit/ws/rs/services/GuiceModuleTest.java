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

package com.thalesgroup.dtkit.ws.rs.services;

import com.google.inject.TypeLiteral;
import com.thalesgroup.dtkit.metrics.model.InputMetricException;
import com.thalesgroup.dtkit.ws.rs.dao.InputMetricDAO;
import com.thalesgroup.dtkit.ws.rs.dao.InputMetricEmbeddedDAO;
import com.thalesgroup.dtkit.ws.rs.services.GuiceModule;

import java.util.Arrays;
import java.util.List;

public class GuiceModuleTest extends GuiceModule {

    @Override
    protected void bindDAO() {
        try {
            List<InputMetricDAO> inputMetricDAOs = Arrays.asList(new InputMetricDAO[]{new InputMetricEmbeddedDAO()});
            bind(new TypeLiteral<List<InputMetricDAO>>() {
            }).toInstance(inputMetricDAOs);
        } catch (InputMetricException e) {
            throw new RuntimeException(e);
        }
    }
}