/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc.
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
package hudson.console;

import hudson.MarkupText;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.io.output.ProxyWriter;
import org.jvnet.hudson.Blob;
import org.jvnet.hudson.BlobTreeReader;
import org.kohsuke.stapler.framework.io.WriterOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static hudson.console.FileAnnotationStore.readBlobFrom;

/**
 *
 * @param <T>
 *      Context type.
 * @author Kohsuke Kawaguchi
 */
public class ConsoleAnnotationOutputStream<T> extends OutputStream {
    private final Writer base;
    private final T context;
    private ByteArrayOutputStream buf = new ByteArrayOutputStream();
    private ConsoleAnnotator<T> ann;
    private BlobTreeReader blobs;
    private long pos;

    /**
     * Reused buffer that stores char representation of a single line.
     */
    private final LineBuffer line = new LineBuffer(256);
    /**
     * {@link OutputStream} that writes to {@link #line}.
     */
    private final WriterOutputStream lineOut;

    /**
     *
     * @param pos
     *      If 'blobs' is non-null, this parameter specifies the initial byte position index.
     */
    public ConsoleAnnotationOutputStream(Writer base, BlobTreeReader blobs, long pos, ConsoleAnnotator<? super T> ann, T context, Charset charset) {
        this.base = base;
        this.ann = ann.cast();
        this.context = context;
        this.blobs = blobs;
        this.pos = pos;
        this.lineOut = new WriterOutputStream(line,charset);
    }

    public ConsoleAnnotator getConsoleAnnotator() {
        return ann;
    }

    private void eol() throws IOException {
        line.reset();
        final StringBuffer strBuf = line.getStringBuffer();

        // retrieve associated annotations
        List<Blob> tags;
        if (blobs!=null)    tags = blobs.range(pos, pos+buf.size());
        else                tags = Collections.emptyList();

        if (tags.isEmpty()) {
            // by far the most case.
            buf.writeTo(lineOut);
        } else {
            // if there are tags, add them as ConsoleAnnotators.

            List<ConsoleAnnotator<T>> annotators = new ArrayList<ConsoleAnnotator<T>>(tags.size());

            {// perform byte[]->char[] while figuring out the char positions of the BLOBs
                byte[] bytes = buf.toByteArray();
                int written = 0;
                for (Blob blob : tags) {
                    try {
                        final ConsoleAnnotation a = readBlobFrom(blob);
                        int len = (int)(blob.tag - pos) - written;
                        lineOut.write(bytes,written,len);
                        lineOut.flush();
                        written += len;
                        final int charPos = strBuf.length();
                        annotators.add(new ConsoleAnnotator<T>() {
                            public ConsoleAnnotator annotate(T context, MarkupText text) {
                                return a.annotate(context,text,charPos);
                            }
                        });
                    } catch (IOException e) {
                        // if we failed to resurrect an annotation, ignore it.
                        LOGGER.log(Level.FINE,"Failed to resurrect annotation",e);
                    }
                }
                // finish the remaining bytes->chars conversion
                lineOut.write(bytes,written,bytes.length-written);
            }

            // aggregate newly retrieved ConsoleAnnotators into the current one.
            if (ann!=null)      annotators.add(ann);
            ann = ConsoleAnnotator.combine(annotators);
        }

        lineOut.flush();
        MarkupText mt = new MarkupText(strBuf.toString());
        if (ann!=null)
            ann = ann.annotate(context,mt);
        base.write(mt.toString());

        // reuse the buffer under normal circumstances, but don't let the line buffer grow unbounded
        if (buf.size()>4096)
            buf = new ByteArrayOutputStream();
        else
            buf.reset();
    }

    public void write(int b) throws IOException {
        buf.write(b);
        if (b==LF)  eol();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int end = off+len;

        for( int i=off; i<end; i++ )
            write(b[i]);
    }

    @Override
    public void flush() throws IOException {
        base.flush();
    }

    @Override
    public void close() throws IOException {
        if (buf.size()>0) {
            /*
                because LargeText cuts output at the line end boundary, this is
                possible only for the very end of the console output, if the output ends without NL.
             */
            eol();
        }
        base.close();
    }

    private static final int LF = 0x0A;

    private static class LineBuffer extends ProxyWriter {
        private LineBuffer(int initialSize) {
            super(new StringWriter(initialSize));
        }

        private void reset() {
            StringBuffer buf = getStringBuffer();
            if (buf.length()>4096)
                out = new StringWriter(256);
            else
                buf.setLength(0);
        }

        private StringBuffer getStringBuffer() {
            StringWriter w = (StringWriter)out;
            return w.getBuffer();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ConsoleAnnotationOutputStream.class.getName());
}
