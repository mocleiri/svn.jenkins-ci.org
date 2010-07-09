package com.thalesgroup.dtkit.tusar.model;

import com.thalesgroup.dtkit.metrics.api.OutputMetric;

import java.io.Serializable;


public class TusarModel implements OutputMetric, Serializable {

    public static OutputMetric OUTPUT_TUSAR_1_0 = new TusarModel("1.0", "TUSAR OUTPUT FORMAT 1.0");

    private String version;

    private String description;

    public TusarModel(String version, String description) {
        this.version = version;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public String getXsd() {
        return "xsd/tusar-1.0.xsd";
    }
}
