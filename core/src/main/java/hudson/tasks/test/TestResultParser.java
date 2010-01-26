/*
 * The MIT License
 * 
 * Copyright (c) 2009, Yahoo!, Inc.
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
package hudson.tasks.test;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.TaskListener;

import java.io.IOException;

/**
 * An extension point by which various parsers can register to parse
 * results files and produce some subclass of TestResult.
 */
public abstract class TestResultParser implements ExtensionPoint {

    public String getParserName() {
        return "Unknown Parser"; 
    }

    public String getTestResultLocationMessage() {
        return "Paths to results files to parse:";
    }

    /**
     * All registered {@link TestResultParser}s
     */
    public static ExtensionList<TestResultParser> all() {
        return Hudson.getInstance().getExtensionList(TestResultParser.class);
    }

    /** Despite the fact that there's no data involved in parsing, and it might as well
     * be a static method, we have to make it an instance method to be able to use
     * runtime method dispatch from just the name of the class. 
     */
    public abstract TestResult parse(String testResultLocations,
                                       AbstractBuild build, Launcher launcher,
                                       TaskListener listener)
            throws InterruptedException, IOException;
}
