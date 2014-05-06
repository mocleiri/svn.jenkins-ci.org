package com.thalesgroup.dtkit.tusar;

import org.junit.Test;

/**
 * @author Gregory Boissinot
 */
public class GoogleTestTest extends AbstractTest {

    @Test
    public void googletest() throws Exception {
        convertAndValidate(JUnit.class, "googletest/testcase1/input.xml", "googletest/testcase1/tusar-result.xml");
    }
}
