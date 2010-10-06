package com.thalesgroup.dtkit.tusar;

import org.junit.Test;


public class CheckstyleTest extends AbstractTest {

    @Test
    public void test() throws Exception {
        convertAndValidate(Checkstyle.class, "checkstyle/input.xml", "checkstyle/tusar-result.xml");
    }
}
