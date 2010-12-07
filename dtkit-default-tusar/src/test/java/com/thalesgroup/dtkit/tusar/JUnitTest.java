package com.thalesgroup.dtkit.tusar;

import org.junit.Test;

public class JUnitTest extends AbstractTest {

    @Test
    public void testcase1() throws Exception {
        convertAndValidate(JUnit.class, "junit/testcase1/input.xml", "junit/testcase1/tusar-result.xml");
    }


}
