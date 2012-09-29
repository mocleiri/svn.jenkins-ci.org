package com.thalesgroup.dtkit.tusar;

import org.junit.Test;

/**
 * @author Gregory Boissinot
 */
public class CPDTest extends AbstractTest {

    @Test
    public void testcase1() throws Exception {
        convertAndValidate(CPD.class, "cpd/testcase1/testresult.xml", "cpd/testcase1/tusar-result.xml");
    }
}
