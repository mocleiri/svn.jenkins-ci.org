package hudson.plugins.cppunit;

import com.thalesgroup.dtkit.junit.model.JUnitModel;
import com.thalesgroup.dtkit.metrics.model.InputMetricXSL;
import com.thalesgroup.dtkit.metrics.model.InputType;
import com.thalesgroup.dtkit.metrics.model.OutputMetric;


public class CppUnitInputMetric extends InputMetricXSL {

    @Override
    public InputType getToolType() {
        return InputType.TEST;
    }

    @Override
    public String getToolName() {
        return "CppUnit";
    }

    @Override
    public String getToolVersion() {
        return "1.x";
    }

    @Override
    public String getXslName() {
        return "cppunit-1.0-to-junit-1.0.xsl";
    }

    @Override
    public String getInputXsd() {
        return "cppunit-1.0.xsd";
    }


    @Override
    public OutputMetric getOutputFormatType() {
        return JUnitModel.OUTPUT_JUNIT_1_0;
    }
}
