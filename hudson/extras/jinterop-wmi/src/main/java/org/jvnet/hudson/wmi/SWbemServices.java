/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc.
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
package org.jvnet.hudson.wmi;

import org.kohsuke.jinterop.JIProxy;

/**
 * @author Kohsuke Kawaguchi
 */
public interface SWbemServices extends JIProxy {
    SWbemObjectSet InstancesOf(String clazz, int flags, Object unused);

    SWbemObjectSet InstancesOf(String clazz);

    /**
     *
     * @param objectPath
     *      If a class name like "Win32_Service" is specified, you get the class object
     *      (from which you can invoke class methods.)
     *
     *      This parameter also supports the path notation (although I haven't found
     *      the authoritative documentation of the syntax.) Examples I've seen
     *      includes "Win32_Service.Name=\"foo\"" 
     */
    SWbemObject Get(String objectPath, int flags, Object objWbemNamedValueSet);

    SWbemObject Get(String objectPath);
}
