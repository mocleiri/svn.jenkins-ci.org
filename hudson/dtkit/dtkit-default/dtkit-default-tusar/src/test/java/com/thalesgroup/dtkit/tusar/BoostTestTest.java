package com.thalesgroup.dtkit.tusar;

import org.junit.Test;

public class BoostTestTest extends AbstractTest {

    @Test
    public void testAutoTest() throws Exception {
        convertAndValidate(BoostTest.class, "boosttest/autotest/testlog.xml", "boosttest/autotest/tusar-result.xml");
    }

    @Test
    public void testAutoTestMultiple() throws Exception {
        convertAndValidate(BoostTest.class, "boosttest/autotest-multiple/testlog.xml", "boosttest/autotest-multiple/tusar-result.xml");
    }

    @Test
    public void testTestCase1() throws Exception {
        convertAndValidate(BoostTest.class, "boosttest/testcase1/testlog.xml", "boosttest/testcase1/tusar-result.xml");
    }

    @Test
    public void testTestCase2() throws Exception {
        convertAndValidate(BoostTest.class, "boosttest/testcase2/testlog.xml", "boosttest/testcase2/tusar-result.xml");
    }

    @Test
    public void testTestCase3() throws Exception {
        convertAndValidate(BoostTest.class, "boosttest/testcase3/testlog.xml", "boosttest/testcase3/tusar-result.xml");
    }

    @Test
    public void testTestCase4() throws Exception {
        convertAndValidate(BoostTest.class, "boosttest/testcase4/testlog.xml", "boosttest/testcase4/tusar-result.xml");
    }
}
