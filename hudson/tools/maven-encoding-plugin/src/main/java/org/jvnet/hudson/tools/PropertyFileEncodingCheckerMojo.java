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
package org.jvnet.hudson.tools;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Checks the encoding of the property files, if it contains characters that are illegal in iso-8859-1,
 * flag an error.
 *
 * @author Kohsuke Kawaguchi
 * @goal check-encoding
 */
public class PropertyFileEncodingCheckerMojo extends AbstractMojo {
    
    /**
     * The Maven Project Object.
     *
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Project p = new Project();
            for( Resource res : (List<Resource>)project.getResources() ) {
                FileSet fs = new FileSet();
                fs.setProject(p);
                File baseDir = new File(res.getDirectory());
                fs.setDir(baseDir);
                fs.setIncludes("**/*.properties");
                for( String f : fs.getDirectoryScanner(p).getIncludedFiles() )
                    check(new File(baseDir,f));
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to check encoding of resource files",e);
        }
    }

    private void check(File f) throws IOException, MojoExecutionException {
        // verify that it loads
        InputStream is = new BufferedInputStream(new FileInputStream(f));
        try {
            Properties props = new Properties();
            props.load(is);
        } finally {
            is.close();
        }


        is = new BufferedInputStream(new FileInputStream(f));
        byte[] bytes = IOUtils.toByteArray(is);
        try {
            for (byte b : bytes) {
                int ch = b;
                ch = ch&0xFF;
                if(0x80<=ch && ch<=0x9F)
                    throw new MojoExecutionException(f+" contains illegal iso-8859-1 character");
            }

            // look for EF BF BD. this is UTF-8 representation of U+FFFD, when Java's decoder
            // hits unparseable sequence of byte sequence it puts this character.
            // See CharsetDecoder constructor
            for( int i=0; i<bytes.length-3; i++ ) {
                if(toInt(bytes[i])==0xEF && toInt(bytes[i+1])==0xBF && toInt(bytes[i+2])==0xBD)
                    throw new MojoExecutionException(f+" contains illegal iso-8859-1 character");
            }
        } finally {
            is.close();
        }
    }

    private int toInt(byte b) {
        return ((int)b)&0xFF;
    }
}
