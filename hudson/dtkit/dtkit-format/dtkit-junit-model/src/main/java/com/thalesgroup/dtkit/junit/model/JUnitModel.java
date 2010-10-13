package com.thalesgroup.dtkit.junit.model;

import com.thalesgroup.dtkit.metrics.model.OutputMetric;
import com.thalesgroup.dtkit.util.validator.ValidationService;

import java.io.Serializable;

@SuppressWarnings("unused")
public class JUnitModel implements Serializable {

    @SuppressWarnings("unused")
    public static OutputMetric OUTPUT_TUSAR_1_0 = new JUnit1() {
        {
            set(new ValidationService());
        }
    };
}
