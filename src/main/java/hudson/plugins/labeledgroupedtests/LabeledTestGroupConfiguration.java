/*
 * The MIT License
 *
 * Copyright (c) 2010, Yahoo!, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.labeledgroupedtests;


import hudson.Util;
import hudson.tasks.junit.JUnitParser;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of the association between a test result location file mask,
 * the parser to invoke, and the label (aka phase) to apply to those results.
 */
public class LabeledTestGroupConfiguration {
    private String parserClassName;
    private String testResultFileMask;
    private String label;

    @DataBoundConstructor
    public LabeledTestGroupConfiguration(String parserClassName, String testResultFileMask, String label) {
        this.parserClassName = parserClassName;
        this.testResultFileMask = testResultFileMask;
        this.label = label;
    }

    public String getParserClassName() {
        return parserClassName;
    }

    public void setParserClassName(String parserClassName) {
        this.parserClassName = parserClassName;
    }

    public String getTestResultFileMask() {
        return testResultFileMask;
    }

    public void setTestResultFileMask(String testResultFileMask) {
        this.testResultFileMask = testResultFileMask;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String toString() {
        return "{label: " + label + ", fileMask: " + testResultFileMask + ", parserClassName: " + parserClassName + "}";
    }

    /**
     * Generate a name for the results that will be stored based on this configuration
     * @return
     */
    public String toNameString() {
        // Try to get a nice name; if that fails, use the parser class name.
        String niceName = DISPLAY_NAME_MAP.get(parserClassName);
        if (niceName==null) niceName = parserClassName;
        return Util.rawEncode(niceName);
    }

    static Map<String, String> DISPLAY_NAME_MAP = new HashMap<String, String>(5);


    /**
     * Ideally, we'd populate this map by asking the parsers how they'd like their
     * names to be displayed in the name map, but I'm coding it up here for
     * expediency. 
     */
    static {
        DISPLAY_NAME_MAP.put(JUnitParser.class.getName(), "junit");
        DISPLAY_NAME_MAP.put("hudson.plugins.cppunitparser.CPPUnitTestResultParser", "cppunit");
    }

}
