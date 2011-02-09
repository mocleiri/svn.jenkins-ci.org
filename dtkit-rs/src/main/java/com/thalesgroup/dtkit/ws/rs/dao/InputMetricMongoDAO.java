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
import com.google.code.morphia.mapping.MappingException;
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
public class InputMetricMongoDAO implements InputMetricDAO {

    private MongoProxy mongoProxy;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private void load(Mongo mongo, Morphia morphia, String dbName) {
        mongoProxy = new MongoProxy(mongo, morphia, dbName);
    }

    @Override
    public boolean isPersistenceStore() {
        return true;
    }

    private Query<InputMetricDB> makeQuery(InputMetricSelector inputMetricSelector) {
        String toolName = inputMetricSelector.getToolName();
        String toolVersion = inputMetricSelector.getToolVersion();
        String toolType = inputMetricSelector.getTooType();
        String outputFormat = inputMetricSelector.getOutputFormat();
        Query<InputMetricDB> query = mongoProxy.createQuery();

        if (toolName != null) {
            query = query.field("toolName").startsWithIgnoreCase(toolName);
            query = query.field("toolName").endsWithIgnoreCase(toolName);
        }
        if (toolVersion != null) {
            query = query.field("toolVersion").startsWithIgnoreCase(toolVersion);
            query = query.field("toolVersion").endsWithIgnoreCase(toolVersion);
        }
        if (toolType != null) {
            query = query.field("toolType").startsWithIgnoreCase(toolType);
            query = query.field("toolType").endsWithIgnoreCase(toolType);
        }
        if (outputFormat != null) {
            query = query.field("outputFormat").startsWithIgnoreCase(outputFormat);
            query = query.field("outputFormat").endsWithIgnoreCase(outputFormat);                        
        }
        return query;
    }

    @Override
    public void insert(String name, String version, InputType toolType, File xsl, File xsd, OutputMetric outputMetric) {

        InputMetricSelector inputMetricSelector = new InputMetricSelector(name, version, toolType.name(), outputMetric.getKey());
        try {
            //Verify that the object not already exists
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
            inputMetricDB.setOutputFormat(outputMetric.getKey());
            mongoProxy.save(inputMetricDB);
        }
        catch (IOException ioe) {
            throw new InputMetricException("Cannot insert the current netric " + inputMetricSelector.toString(), ioe);
        }
    }

    @Override
    public void delete(String name, String version, InputType toolType, OutputMetric outputMetric) {

        InputMetricSelector inputMetricSelector = new InputMetricSelector(name, version, toolType.name(), outputMetric.getKey());
        Query<InputMetricDB> query = makeQuery(inputMetricSelector);
        Collection<? extends InputMetric> metrics = mongoProxy.find(query).asList();
        if (metrics.size() == 0) {
            throw new MappingException("Cannot get metric for " + inputMetricSelector.toString());
        }
        assert metrics.size() == 1 : "There are more than 2 metrics for " + inputMetricSelector.toString();

        mongoProxy.deleteByQuery(query);
    }


    public long getCount() {
        return mongoProxy.count();
    }

    public Collection<? extends InputMetric> getInputMetric(InputMetricSelector inputMetricSelector) {
        return mongoProxy.find(makeQuery(inputMetricSelector)).asList();
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
        QueryResults<InputMetricDB> inputMetricDBQueryResults = mongoProxy.find();
        return inputMetricDBQueryResults.asList();
    }

    @Override
    public byte[] getXSD(InputMetricSelector inputMetricSelector) {

        //Verify that the object not already exists
        Collection<? extends InputMetric> inputMetrics = getInputMetric(inputMetricSelector);
        assert inputMetrics.size() <= 1;

        if (inputMetrics.size() == 0) {
            return null;
        }

        InputMetric metric = inputMetrics.iterator().next();
        assert  metric instanceof InputMetricDB;
        InputMetricDB inputMetricDB = (InputMetricDB) metric;

        return String.valueOf(inputMetricDB.getXsdContent()).getBytes();
    }
}
