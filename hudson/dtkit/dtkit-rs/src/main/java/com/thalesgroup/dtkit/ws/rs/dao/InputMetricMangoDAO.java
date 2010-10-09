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

package com.thalesgroup.dtkit.ws.rs.dao;

import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryResults;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.Mongo;
import com.thalesgroup.dtkit.metrics.model.*;
import com.thalesgroup.dtkit.ws.rs.model.InputMetricDB;
import com.thalesgroup.dtkit.ws.rs.model.InputMetricSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;


@Singleton
public class InputMetricMangoDAO implements InputMetricDAO {

    private MangoProxy mangoProxy;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private void load(Mongo mongo, Morphia morphia, String dbName) {
        mangoProxy = new MangoProxy(mongo, morphia, dbName);
    }

    @Override
    public boolean isPersistenceStore() {
        return true;
    }

    @Override
    public void insert(String name, String version, InputType toolType, File xsl, File xsd, OutputMetric outputMetricType) {
        try {

            //Verify that the object not already exists
            InputMetricSelector inputMetricSelector = new InputMetricSelector(name, version, toolType.name(), outputMetricType.getKey());
            Collection<? extends InputMetric> inputMetrics = getInputMetric(inputMetricSelector);
            assert inputMetrics.size() <= 1;
            if (inputMetrics.size() == 1) {
                if (logger.isDebugEnabled()) {
                    logger.debug("The object " + inputMetricSelector + " already exist.");
                }
                return;
            }

            InputMetricDB inputMetricDB = new InputMetricDB();
            inputMetricDB.setToolName(name);
            inputMetricDB.setToolVersion(version);
            inputMetricDB.setToolType(toolType);
            inputMetricDB.setXslContent(getContentTextFile(xsl));
            if (xsd != null) {
                inputMetricDB.setXsdContent(getContentTextFile(xsd));
            } else {
                inputMetricDB.setXsdContent(null);
            }
            inputMetricDB.setInputMetricType(InputMetricType.XSL);
            inputMetricDB.setOutputFormat(outputMetricType.getKey());
            mangoProxy.save(inputMetricDB);
        }
        catch (IOException e) {
            throw new InputMetricException("Can't insert the current object", e);
        }
    }

    @Override
    public void delete(String name, String version, InputType toolType, OutputMetric outputMetric) {
        Query<InputMetricDB> query = mangoProxy.createQuery();
        if (name != null) {
            query = query.field("toolName").startsWithIgnoreCase(name);
            query = query.field("toolName").endsWithIgnoreCase(name);
        }
        if (version != null) {
            query = query.field("toolVersion").containsIgnoreCase(version);
        }
        if (toolType != null) {
            query = query.field("toolType").containsIgnoreCase(toolType.name());
        }
        if (outputMetric != null) {
            query = query.field("outputFormat").containsIgnoreCase(outputMetric.getKey());
        }
        mangoProxy.deleteByQuery(query);
    }


    public long getCount() {
        return mangoProxy.count();
    }

    public Collection<? extends InputMetric> getInputMetric(InputMetricSelector inputMetricSelector) {
        String toolName = inputMetricSelector.getToolName();
        String toolVersion = inputMetricSelector.getToolVersion();
        String toolType = inputMetricSelector.getTooType();
        String outputFormat = inputMetricSelector.getOutputFormat();
        Query<InputMetricDB> query = mangoProxy.createQuery();

        if (toolName != null) {
            query = query.field("toolName").startsWithIgnoreCase(toolName);
            query = query.field("toolName").endsWithIgnoreCase(toolName);
        }
        if (toolVersion != null) {
            query = query.field("toolVersion").containsIgnoreCase(toolVersion);
        }
        if (toolType != null) {
            query = query.field("toolType").containsIgnoreCase(toolType);
        }
        if (outputFormat != null) {
            query = query.field("outputFormat").containsIgnoreCase(outputFormat);
        }
        return mangoProxy.find(query).asList();
    }


    private String getContentTextFile(File path) throws IOException {
        StringBuffer sb = new StringBuffer();
        BufferedReader bf = new BufferedReader(new FileReader(path));
        String line;
        while ((line = bf.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    public Collection<? extends InputMetric> getInputMetrics() {
        QueryResults<InputMetricDB> inputMetricDBQueryResults = mangoProxy.find();
        return inputMetricDBQueryResults.asList();
    }
}
