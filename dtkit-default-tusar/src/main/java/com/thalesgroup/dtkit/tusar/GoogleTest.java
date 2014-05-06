package com.thalesgroup.dtkit.tusar;

import com.thalesgroup.dtkit.processor.InputMetric;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "googleTest", namespace = "tusar")
@InputMetric
public class GoogleTest extends JUnit {

    @Override
    public String getToolName() {
        return "GoogleTest";
    }

    @Override
    public String getToolVersion() {
        return "1.6";
    }

}
