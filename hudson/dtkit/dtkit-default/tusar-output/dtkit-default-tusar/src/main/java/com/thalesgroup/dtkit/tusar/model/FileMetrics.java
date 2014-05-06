/**
 * Maven and Sonar plugin for .Net
 * Copyright (C) 2010 Jose Chillan and Alexandre Victoor
 * mailto: jose.chillan@codehaus.org or alexvictoor@codehaus.org
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

/*
 * Created on May 5, 2009
 */
package com.thalesgroup.dtkit.tusar.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A C# file metrics' data.
 *
 * @author Mohamed Koundoussi
 */
public class FileMetrics extends SourceMetric {

    private File projectDirectory;
    private File sourcePath;
    private List<MethodMetric> methods;
    private String className;
    private String namespace;

    /**
     * Constructs a @link{FileMetrics}.
     */
    public FileMetrics() {
        methods = new ArrayList<MethodMetric>();
    }

    public void addMethod(MethodMetric method) {
        methods.add(method);
        // This is the total complexity over the file.
        complexity += method.getComplexity();

        if (method.isAccessor()) {
            countAccessors++;
        }
    }


    /**
     * Returns the path.
     *
     * @return The path to return.
     */
    public File getSourcePath() {
        return this.sourcePath;
    }

    /**
     * Sets the path.
     *
     * @param path The path to set.
     */
    public void setSourcePath(File path) {
        this.sourcePath = path;
    }


    public List<MethodMetric> getMethods() {
        return this.methods;
    }


    @Override
    public String toString() {
        return "File(class=" + className + ", path=" + sourcePath + ")";
    }

    /**
     * Returns the className.
     *
     * @return The className to return.
     */
    public String getClassName() {
        return this.className;
    }

    /**
     * Sets the className.
     *
     * @param className The className to set.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Returns the projectDirectory.
     *
     * @return The projectDirectory to return.
     */
    public File getProjectDirectory() {
        return this.projectDirectory;
    }

    /**
     * Sets the projectDirectory.
     *
     * @param projectDirectory The projectDirectory to set.
     */
    public void setProjectDirectory(File projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    /**
     * Sets the countBlankLines.
     *
     * @param countBlankLines The countBlankLines to set.
     */
    public void setCountBlankLines(int countBlankLines) {
        this.countBlankLines = countBlankLines;
    }

    /**
     * Returns the namespace.
     *
     * @return The namespace to return.
     */
    public String getNamespace() {
        return this.namespace;
    }

    /**
     * Sets the namespace.
     *
     * @param namespace The namespace to set.
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }


}
