/*
 * The MIT License
 *
 * Copyright (c) 2010, InfraDNA, Inc.
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

package hudson.util.io;

import hudson.util.FileVisitor;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link FileVisitor} that creates a zip archive.
 *
 * @see ArchiverFactory#ZIP
 */
final class ZipArchiver extends Archiver {
    private final byte[] buf = new byte[8192];
    private final ZipOutputStream zip;

    ZipArchiver(OutputStream out) {
        zip = new ZipOutputStream(out);
        zip.setEncoding(System.getProperty("file.encoding"));
    }

    public void visit(File f, String relativePath) throws IOException {
        if(f.isDirectory()) {
            ZipEntry dirZipEntry = new ZipEntry(relativePath+'/');
            // Setting this bit explicitly is needed by some unzipping applications (see HUDSON-3294).
            dirZipEntry.setExternalAttributes(BITMASK_IS_DIRECTORY);
            zip.putNextEntry(dirZipEntry);
            zip.closeEntry();
        } else {
            zip.putNextEntry(new ZipEntry(relativePath));
            FileInputStream in = new FileInputStream(f);
            int len;
            while((len=in.read(buf))>0)
                zip.write(buf,0,len);
            in.close();
            zip.closeEntry();
        }
        entriesWritten++;
    }

    public void close() throws IOException {
        zip.close();
    }

    // Bitmask indicating directories in 'external attributes' of a ZIP archive entry.
    private static final long BITMASK_IS_DIRECTORY = 1<<4;
}
