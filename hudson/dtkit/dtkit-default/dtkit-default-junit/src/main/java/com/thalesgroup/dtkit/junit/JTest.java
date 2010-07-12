package com.thalesgroup.dtkit.junit;

import com.thalesgroup.dtkit.metrics.api.InputMetricXSL;
import com.thalesgroup.dtkit.metrics.api.InputType;
import com.thalesgroup.dtkit.metrics.api.OutputMetric;
import com.thalesgroup.dtkit.junit.model.JUnitModel;


public class JTest extends InputMetricXSL {

    @Override
    public InputType getToolType() {
        return InputType.TEST;
    }

    @Override
    public String getToolName() {
        return "JTest";
    }

    @Override
    public String getToolVersion() {
        return "undermined (default)";
    }

    @Override
    public String getXslName() {
        return "cppunit-1.0-to-junit-1.0.xsl";
    }

    @Override
    public String getInputXsd() {
        return "cppunit-1.0.xsd";
    }

    public OutputMetric getOutputFormatType() {
        return JUnitModel.OUTPUT_JUNIT_1_0;
    }
}
