package com.thalesgroup.dtkit.junit;

import org.junit.Test;

public class BoostTestTest extends AbstractTest {

    @Test
    public void testAutoTest() throws Exception {
        convertAndValidate(BoostTest.class, "boosttest/autotest/testlog.xml", "boosttest/autotest/junit-result.xml");
    }

    @Test
    public void testAutoTestMultiple() throws Exception {
        convertAndValidate(BoostTest.class, "boosttest/autotest-multiple/testlog.xml", "boosttest/autotest-multiple/junit-result.xml");
    }

    @Test
    public void testTestCase1() throws Exception {
        convertAndValidate(BoostTest.class, "boosttest/testcase1/testlog.xml", "boosttest/testcase1/junit-result.xml");
    }

    @Test
    public void testTestCase2() throws Exception {
        convertAndValidate(BoostTest.class, "boosttest/testcase2/testlog.xml", "boosttest/testcase2/junit-result.xml");
    }

    @Test
    public void testTestCase3() throws Exception {
        convertAndValidate(BoostTest.class, "boosttest/testcase3/testlog.xml", "boosttest/testcase3/junit-result.xml");
    }

    @Test
    public void testTestCase4() throws Exception {
        convertAndValidate(BoostTest.class, "boosttest/testcase4/testlog.xml", "boosttest/testcase4/junit-result.xml");
    }
}
