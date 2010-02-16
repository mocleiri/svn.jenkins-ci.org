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
package org.jvnet.hudson.update_center;

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Information about Hudson plugin and its release history, discovered from Maven repository.
 */
public final class PluginHistory {
    /**
     * ArtifactID equals short name.
     */
    public final String artifactId;

    /**
     * All discovered versions, by the numbers.
     */
    public final TreeMap<VersionNumber,HPI> artifacts = new TreeMap<VersionNumber, HPI>(VersionNumber.DESCENDING);

    final Set<String> groupId = new TreeSet<String>();

    public PluginHistory(String shortName) {
        this.artifactId = shortName;
    }

    public HPI latest() {
        return artifacts.get(artifacts.firstKey());
    }
}
