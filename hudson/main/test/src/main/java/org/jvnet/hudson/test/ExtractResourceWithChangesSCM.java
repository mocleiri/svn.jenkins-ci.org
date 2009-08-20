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
package org.jvnet.hudson.test;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.User;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.digester.Digester;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.export.Exported;
import org.xml.sax.SAXException;

/**
 * {@link SCM} useful for testing that extracts the given resource as a zip file.
 *
 * @author Kohsuke Kawaguchi
 */
public class ExtractResourceWithChangesSCM extends NullSCM {
    private final URL firstZip;
    private final URL secondZip;
    
    public ExtractResourceWithChangesSCM(URL firstZip, URL secondZip) {
        if ((firstZip==null) || (secondZip==null))
            throw new IllegalArgumentException();
        this.firstZip = firstZip;
	this.secondZip = secondZip;
    }

    @Override
    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener, File changeLogFile) throws IOException, InterruptedException {
    	if (workspace.exists()) {
            listener.getLogger().println("Deleting existing workspace " + workspace.getRemote());
    		workspace.deleteRecursive();
    	}
        listener.getLogger().println("Staging first zip: "+firstZip);
        workspace.unzipFrom(firstZip.openStream());
        listener.getLogger().println("Staging second zip: "+secondZip);
        workspace.unzipFrom(secondZip.openStream());

	// Get list of files changed in secondZip.
	ZipInputStream zip = new ZipInputStream(secondZip.openStream());
	ZipEntry e;
	ExtractChangeLogEntry changeLog = new ExtractChangeLogEntry(secondZip.toString());
	
	try {
	    while ((e=zip.getNextEntry())!=null) {
		if (!e.isDirectory()) 
		    changeLog.addFile(new FileInZip(e.getName()));
	    }
	}
	finally {
	    zip.close();
	}
	saveToChangeLog(changeLogFile, changeLog);
	
        return true;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
	return new ExtractChangeLogParser();
    }
    
    public static String escapeForXml(String string) {
        if (string == null) {
            return "";
        }

        // Loop through and replace the special chars.
        int size = string.length();
        char ch = 0;
        StringBuffer escapedString = new StringBuffer(size);
        for (int index = 0; index < size; index++) {
            // Convert special chars.
            ch = string.charAt(index);
            switch (ch) {
            case '&':
                escapedString.append("&amp;");
                break;
            case '<':
                escapedString.append("&lt;");
                break;
            case '>':
                escapedString.append("&gt;");
                break;
            case '\'':
                escapedString.append("&apos;");
                break;
            case '\"':
                escapedString.append("&quot;");
                break;
            default:
                escapedString.append(ch);
            }
        }

        return escapedString.toString().trim();
    }
    
    public void saveToChangeLog(File changeLogFile, ExtractChangeLogEntry changeLog) throws IOException {
	FileOutputStream outputStream = new FileOutputStream(changeLogFile);
	
	PrintStream stream = new PrintStream(outputStream, false, "UTF-8");

	stream.println("<?xml version='1.0' encoding='UTF-8'?>");
	stream.println("<extractChanges>");
	stream.println("<entry>");
	stream.println("<zipFile>" + escapeForXml(changeLog.getZipFile()) + "</zipFile>");
	stream.println("<file>");
	
	for (String fileName : changeLog.getAffectedPaths()) {
	    stream.println("<fileName>" + escapeForXml(fileName) + "</fileName>");
	}

	stream.println("</file>");
	stream.println("</entry>");
	stream.println("</extractChanges>");

	stream.close();
    }

    

}
