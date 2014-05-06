package com.thalesgroup.dtkit.tusar;

import org.junit.Test;

/**
 * @author Gregory Boissinot
 */
public class SourceMonitorTest extends AbstractTest {

    @Test
    public void testcase1() throws Exception {
        convertAndValidate(SourceMonitor.class, "sourcemonitor/testcase1/testresult.xml", "sourcemonitor/testcase1/tusar-result.xml");
    }
}