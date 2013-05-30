package com.thalesgroup.dtkit.junit.model;

import com.thalesgroup.dtkit.metrics.model.AbstractOutputMetric;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class JUnit6 extends AbstractOutputMetric implements Serializable {

    @Override
    @XmlElement
    public String getKey() {
        return "junit";
    }

    @Override
    @XmlElement
    public String getDescription() {
        return "JUNIT OUTPUT FORMAT 5.0";
    }

    @Override
    @XmlElement
    public String getVersion() {
        return "6.0";
    }

    @Override
    @XmlElement
    public String[] getXsdNameList() {
        return new String[]{"xsd/junit-6.xsd"};
    }
}

