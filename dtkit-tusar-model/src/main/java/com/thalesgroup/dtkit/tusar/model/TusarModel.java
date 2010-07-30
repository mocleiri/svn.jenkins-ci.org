package com.thalesgroup.dtkit.tusar.model;

import com.thalesgroup.dtkit.metrics.api.OutputMetric;

import java.io.Serializable;


@SuppressWarnings("unused")
public class TusarModel implements OutputMetric, Serializable {

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_1_0 = new TusarModel("1.0", "TUSAR OUTPUT FORMAT 1.0");

    private final String version;

    private final String description;

    public TusarModel(String version, String description) {
        this.version = version;
        this.description = description;
    }

    @SuppressWarnings("unused")
    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unused")
    public String getVersion() {
        return version;
    }

    public String getXsd() {
        return "xsd/tusar-1.0.xsd";
    }
}
