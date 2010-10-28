package com.thalesgroup.dtkit.tusar;

import org.junit.Test;

public class AUnitTest extends AbstractTest {

    @Test
    public void testcase1() throws Exception {
        convertAndValidate(AUnit.class, "aunit/testcase1/testresult.xml", "aunit/testcase1/tusar-result.xml");
    }


}
