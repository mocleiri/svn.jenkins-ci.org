package com.thalesgroup.dtkit.ws.rs.model;

import com.thalesgroup.dtkit.metrics.model.OutputMetric;

public class InputMetricSelector {

    private String toolName;

    private String toolVersion;

    private String tooType;

    private String outputFormat;

    public InputMetricSelector(String toolName, String toolVersion, String tooType, OutputMetric outputFormat) {
        this.toolName = toolName;
        this.toolVersion = toolVersion;
        this.tooType = tooType;
        if (outputFormat != null) {
            this.outputFormat = outputFormat.getKey();
        }
    }

    public InputMetricSelector(String toolName, String toolVersion, String tooType, String outputFormat) {
        this.toolName = toolName;
        this.toolVersion = toolVersion;
        this.tooType = tooType;
        this.outputFormat = outputFormat;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolVersion() {
        return toolVersion;
    }

    public String getTooType() {
        return tooType;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public boolean isNoCriteria() {
        return toolName == null && toolVersion == null && tooType == null && outputFormat == null;
    }

    @Override
    public String toString() {
        return "{" + toolName + "," + toolVersion + "," + tooType + "," + outputFormat + "}";
    }
}
