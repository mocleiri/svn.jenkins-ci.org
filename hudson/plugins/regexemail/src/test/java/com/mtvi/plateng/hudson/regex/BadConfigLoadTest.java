/**
 * Copyright (c) 2008, MTV Networks
 */

package com.mtvi.plateng.hudson.regex;

import hudson.model.User;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BadConfigLoadTest {

    @Test
    public void testBadConfig() throws IOException {
        PluginImpl pi = new PluginImpl();
        Configuration config = (Configuration) pi.loadConfiguration();
        Assert.assertFalse(config.isValid());
    }

    @Test
    public void testRegexMailAddressResolver() throws Exception {
        PluginImpl pi = new PluginImpl();
        Configuration config = (Configuration) pi.loadConfiguration();
        RegexMailAddressResolver resolver = new RegexMailAddressResolver(config);
        User u = User.get("username");
        Assert.assertNull(resolver.findMailAddressFor(u));
    }

    @Before
    public void setUp() throws Exception {
        FileUtils.copyFile(new File("src/test/resources/unit/bad-config.xml"), new File(
                HudsonUtil.root, RegexMailAddressResolver.class.getName() + ".xml"));
    }

    @After
    public void tearDown() throws Exception {
        HudsonUtil.hudson.cleanUp();
        FileUtils.deleteDirectory(HudsonUtil.root);
    }
}
