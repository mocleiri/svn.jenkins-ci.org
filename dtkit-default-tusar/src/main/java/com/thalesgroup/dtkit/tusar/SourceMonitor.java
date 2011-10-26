package com.thalesgroup.dtkit.tusar;

import com.thalesgroup.dtkit.metrics.model.InputMetricXSL;
import com.thalesgroup.dtkit.metrics.model.InputType;
import com.thalesgroup.dtkit.metrics.model.OutputMetric;
import com.thalesgroup.dtkit.processor.InputMetric;
import com.thalesgroup.dtkit.tusar.model.TusarModel;

import javax.xml.bind.annotation.XmlType;

/**
 * @author Gregory Boissinot
 */
@XmlType(name = "sourceMonitor", namespace = "tusar")
@InputMetric
public class SourceMonitor extends InputMetricXSL {

    @Override
    public InputType getToolType() {
        return InputType.MEASURE;
    }

    @Override
    public String getToolName() {
        return "SourceMonitor";
    }

    @Override
    public String getToolVersion() {
        return "2.6";
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public String getXslName() {
        return "sourcemonitor-2.6-to-tusar-8.0.xsl";
    }

    @Override
    public String[] getInputXsdNameList() {
        return null;
    }

    @Override
    public OutputMetric getOutputFormatType() {
        return TusarModel.OUTPUT_TUSAR_8_0;
    }
}
