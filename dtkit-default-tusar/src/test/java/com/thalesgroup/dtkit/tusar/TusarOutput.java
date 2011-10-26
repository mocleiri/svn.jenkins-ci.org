package com.thalesgroup.dtkit.tusar;

import org.junit.Test;

import java.io.File;


public class TusarOutput extends AbstractTest {

    @Test
    public void testTusar1() throws Exception {
        validOutputTusarV5(new File(this.getClass().getResource("tusar/testcase1/input-tusarv5.xml").toURI()));
    }

}
