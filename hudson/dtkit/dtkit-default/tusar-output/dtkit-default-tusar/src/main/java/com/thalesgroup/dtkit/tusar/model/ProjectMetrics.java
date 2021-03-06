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
 * Created on May 19, 2009
 *
 */
package com.thalesgroup.dtkit.tusar.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Metrics for a Visual Studio solution.
 *
 * @author Mohamed Koundoussi
 */
public class ProjectMetrics extends AbstractMeterable {

    private Set<String> namespaces;

    public ProjectMetrics() {
        namespaces = new HashSet<String>();
    }

    @Override
    public void addFile(FileMetrics file) {
        super.addFile(file);
        String namespace = file.getNamespace();
        namespaces.add(namespace);
    }

    public int getCountNamespaces() {
        return namespaces.size();
    }
}
