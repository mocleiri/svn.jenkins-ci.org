package com.thalesgroup.dtkit.tusar.model;

import com.thalesgroup.dtkit.metrics.model.AbstractOutputMetric;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class Tusarv10 extends AbstractOutputMetric implements Serializable {

    @Override
    @XmlElement
    public String getKey() {
        return "tusar";
    }

    @Override
    @XmlElement
    public String getDescription() {
        return "TUSAR OUTPUT FORMAT 10.0";
    }

    @Override
    @XmlElement
    public String getVersion() {
        return "10.0";
    }

    @Override
    @XmlElement
    public String[] getXsdNameList() {
        return new String[]{"xsd/design-1.xsd", "xsd/size-1.xsd", "xsd/documentation-1.xsd", "xsd/duplications-1.xsd", "xsd/tests-5.xsd", "xsd/line-coverage-1.xsd", "xsd/branch-coverage-1.xsd", "xsd/coverage-4.xsd", "xsd/violations-4.xsd", "xsd/measures-6.xsd", "xsd/tusar-10.xsd"};
    }
}
