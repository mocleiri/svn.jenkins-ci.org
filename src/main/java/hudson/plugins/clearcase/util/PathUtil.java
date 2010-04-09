/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
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
package hudson.plugins.clearcase.util;

import hudson.Launcher;

public abstract class PathUtil {
    
    private static final char COMMENT_SEPARATOR = '#';

    public static String convertPathForOS(String path, Launcher launcher) {
        return convertPathForOS(path, launcher.isUnix());
    }

    public static String convertPathForOS(String path,
                                          boolean isUnix) {
        String tempPath = path;
        String rightSep = fileSepForOSAsRegexp(isUnix);
        String wrongSep = fileSepForOSAsRegexp(!isUnix);
        tempPath = tempPath.replaceAll(newLineForOS(!isUnix), newLineForOS(isUnix));
        tempPath = tempPath.replaceAll("\r\r", "\r");
        StringBuilder finalPath = new StringBuilder();
        String[] rows = tempPath.split(newLineForOS(isUnix));
        for(int i = 0; i < rows.length; i++) {
            if (i > 0) {
                finalPath.append(newLineForOS(isUnix));
            }
            int indexOfDash = rows[i].indexOf(COMMENT_SEPARATOR);
            if (indexOfDash > -1) {
                finalPath.append(rows[i].substring(0, indexOfDash).replaceAll(wrongSep, rightSep));
                finalPath.append(rows[i].substring(indexOfDash));
            } else {
                finalPath.append(rows[i].replaceAll(wrongSep, rightSep));
            }
        }
        if (tempPath.endsWith(newLineForOS(isUnix))) {
            finalPath.append(newLineForOS(isUnix));
        }
        
        return finalPath.toString();
    }
    
    public static String newLineForOS(boolean isUnix) {
        if (isUnix) {
            return "\n";
        } else {
            return "\r\n";
        }
    }
    
    public static String fileSepForOS(boolean isUnix) {
        if (isUnix) {
            return "/";
        }
        else {
            return "\\";
        }
    }
    
    public static String fileSepForOSAsRegexp(boolean isUnix) {
        if (isUnix) {
            return "/";
        }
        else {
            return "\\\\";
        }
    }

}
