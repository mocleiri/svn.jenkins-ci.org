/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Eric Lefevre-Ardant, Erik Ramfelt, Michael B. Donohue, Alan Harder,
 * Manufacture Française des Pneumatiques Michelin, Romain Seguy
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
package com.michelin.cio.hudson.plugins.copytoslave;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.FilePath.TarCompression;
import hudson.Functions;
import hudson.org.apache.tools.tar.TarInputStream;
import hudson.remoting.Future;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;
import hudson.util.IOException2;
import hudson.util.IOUtils;
import hudson.util.io.Archiver;
import hudson.util.io.ArchiverFactory;
import static hudson.util.jna.GNUCLibrary.LIBC;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Chmod;
import org.apache.tools.tar.TarEntry;

/**
 * Complements Hudson's {@link FilePath} to enhance the {@code copyRecursiveTo()}
 * method so that it offer a possibility to flatten dirs (cf. HUDSON-8220) and
 * to not use Ant's default excludes (cf. HUDSON-7999).
 *
 * <p>This class also fixes HUDSON-8155.</p>
 */
public class MyFilePath implements Serializable {

    private static final long serialVersionUID = 1; // HUDSON-8274

    /**
     * Enhances Hudson's {@link FilePath#copyRecursiveTo} until I patch Hudson
     * core and upgrade the plugin to the corresponding Hudson version.
     *
     * <p>This method supports only local to remote copy.</p>
     */
    public static int copyRecursiveTo(
            final FilePath source,
            final String fileMask, final String excludes,
            final boolean flatten, final boolean includeAntExcludes,
            final FilePath target) throws IOException, InterruptedException {
        // local -> remote copy
        final Pipe pipe = Pipe.createLocalToRemote();

        Future<Void> future = target.actAsync(new FileCallable<Void>() {
            private static final long serialVersionUID = 1; // HUDSON-8274

            public Void invoke(File f, VirtualChannel channel) throws IOException {
                try {
                    readFromTar(source.getRemote() + '/' + fileMask, f, flatten, TarCompression.GZIP.extract(pipe.getIn()));
                    return null;
                } finally {
                    pipe.getIn().close();
                }
            }
        });
        
        int r = writeToTar(new File(source.getRemote()), fileMask, excludes, includeAntExcludes, TarCompression.GZIP.compress(pipe.getOut()));
        try {
            future.get();
        } catch (ExecutionException e) {
            throw new IOException2(e);
        }
        return r;
    }

    /**
     * Full copy/paste of Hudson's {@link FilePath#readFromTar} method with
     * some tweaking (mainly the flatten behavior).
     *
     * @see hudson.FilePath#readFromTar(java.lang.String, java.io.File, java.io.InputStream) 
     */
    public static void readFromTar(String name, File baseDir, boolean flatten, InputStream in) throws IOException {
        Chmod chmodTask = null; // HUDSON-8155

        TarInputStream t = new TarInputStream(in);
        try {
            TarEntry tarEntry;
            while ((tarEntry = t.getNextEntry()) != null) {
                File f = null;

                if(!flatten) {
                    f = new File(baseDir, tarEntry.getName());
                }
                else {
                    String fileName = StringUtils.substringAfterLast(tarEntry.getName(), "/");
                    if(StringUtils.isBlank(fileName)) {
                        fileName = StringUtils.substringAfterLast(tarEntry.getName(), "\\");
                    }
                    f = new File(baseDir, fileName);
                }

                // dir processing
                if(tarEntry.isDirectory() && !flatten) {
                    f.mkdirs();
                }
                // file processing
                else {
                    if (f.getParentFile() != null && !flatten) {
                        f.getParentFile().mkdirs();
                    }

                    IOUtils.copy(t, f);

                    f.setLastModified(tarEntry.getModTime().getTime());

                    // chmod
                    int mode = tarEntry.getMode()&0777;
                    if(mode!=0 && !Functions.isWindows()) // be defensive
                        try {
                            LIBC.chmod(f.getPath(), mode);
                        } catch (NoClassDefFoundError ncdfe) {
                            // be defensive. see http://www.nabble.com/-3.0.6--Site-copy-problem%3A-hudson.util.IOException2%3A--java.lang.NoClassDefFoundError%3A-Could-not-initialize-class--hudson.util.jna.GNUCLibrary-td23588879.html
                        } catch (UnsatisfiedLinkError ule) {
                            // HUDSON-8155: use Ant's chmod task
                            if(chmodTask == null) {
                                chmodTask = new Chmod();
                            }
                            chmodTask.setProject(new Project());
                            chmodTask.setFile(f);
                            chmodTask.setPerm(Integer.toString(mode));
                        }
                }
            }
        } catch(IOException e) {
            throw new IOException2("Failed to extract "+name,e);
        } finally {
            t.close();
        }
    }

    /**
     * Full copy/paste of Hudson's {@link FilePath#writeToTar} method with some
     * tweaking (added an includeAntExcludes parameter).
     *
     * @see hudson.FilePath#writeToTar(java.lang.String, java.io.File, java.io.InputStream)
     */
    public static Integer writeToTar(File baseDir, String fileMask, String excludes, boolean includeAntExcludes, OutputStream out) throws IOException {
        Archiver tw = ArchiverFactory.TAR.create(out);
        try {
            new MyGlobDirScanner(fileMask, excludes, includeAntExcludes).scan(baseDir, tw);  // HUDSON-7999
        } finally {
            tw.close();
        }
        return tw.countEntries();
    }

}
