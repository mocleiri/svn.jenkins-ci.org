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
 */
package com.thalesgroup.dtkit.tusar;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

/**
 * Counts the number of blank lines in a source file.
 *
 * @author Mohamed Koundoussi
 */
public class BlankLineCounter {

    /**
     * Counts the number of blank lines.
     *
     * @param file
     * @return the number of blank lines
     */
    public static int countBlankLines(File file) {
        int count = 0;
        // We can have blank lines inside comments, for example:
        // /**
        // this is a comment
        //
        // with one blank line
        // */
        // This then calculates the ncloc wrong because it subtracts the number
        // of comments plus the number of blank lines, i.e.:
        // ncloc = loc - 5 - 1;
        boolean inComment = false;
        try {
            FileInputStream stream = new FileInputStream(file);
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf("/*") != -1) {
                    inComment = true;
                } else if (line.indexOf("*/") != -1) {
                    inComment = false;
                }

                if (StringUtils.isBlank(line)) {
                    if (!inComment) {
                        count++;
                    }
                }
            }
        } catch (Exception e) // NOPMD
        {
            // Do nothing
        }
        return count;
    }
}

