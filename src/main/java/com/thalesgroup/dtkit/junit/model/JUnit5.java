package com.thalesgroup.dtkit.junit.model;

import com.thalesgroup.dtkit.metrics.model.AbstractOutputMetric;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class JUnit5 extends AbstractOutputMetric implements Serializable {

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
        return "5.0";
    }

    @Override
    @XmlElement
    public String[] getXsdNameList() {
        return new String[]{"xsd/junit-5.xsd"};
    }
}

