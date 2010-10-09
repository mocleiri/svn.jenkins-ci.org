package com.thalesgroup.dtkit.ws.rs.vo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "metric")
@XmlAccessorType(XmlAccessType.FIELD)
public class InputMetricVo {

    private String toolName;

    private String toolVersion;

    private String tooType;

    private String outputFormat;

    public InputMetricVo() {
    }

    public InputMetricVo(String toolName, String toolVersion, String tooType, String outputFormat) {
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
}
