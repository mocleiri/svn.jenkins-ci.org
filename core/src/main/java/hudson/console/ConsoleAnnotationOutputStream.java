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
import hudson.util.ByteArrayOutputStream2;
import org.apache.commons.io.output.ProxyWriter;
import org.kohsuke.stapler.framework.io.WriterOutputStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to convert plain text console output (as byte sequence) + embedded annotations into HTML (as char sequence).
 *
 * @param <T>
 *      Context type.
 * @author Kohsuke Kawaguchi
 */
public class ConsoleAnnotationOutputStream<T> extends OutputStream {
    private final Writer out;
    private final T context;
    private ByteArrayOutputStream2 buf = new ByteArrayOutputStream2();
    private ConsoleAnnotator<T> ann;

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
     */
    public ConsoleAnnotationOutputStream(Writer out, ConsoleAnnotator<? super T> ann, T context, Charset charset) {
        this.out = out;
        this.ann = ConsoleAnnotator.cast(ann);
        this.context = context;
        this.lineOut = new WriterOutputStream(line,charset);
    }

    public ConsoleAnnotator getConsoleAnnotator() {
        return ann;
    }

    /**
     * Called after we read the whole line of plain text, which is stored in {@link #buf}.
     * This method performs annotations and send the result to {@link #out}.
     */
    private void eol() throws IOException {
        final byte[] in = buf.getBuffer();
        final int sz = buf.size();

        line.reset();
        final StringBuffer strBuf = line.getStringBuffer();

        int next = ConsoleAnnotation.findPreamble(in,0,sz);
        if (next<0) {
            // no embedded annotations at all --- by far the most common case.
            buf.writeTo(lineOut);
        } else {
            // if there are embedded annotations, add them as ConsoleAnnotators.

            List<ConsoleAnnotator<T>> annotators = new ArrayList<ConsoleAnnotator<T>>();

            {// perform byte[]->char[] while figuring out the char positions of the BLOBs
                int written = 0;
                while (next>=0) {
                    if (next>written) {
                        lineOut.write(in,written,next-written);
                        lineOut.flush();
                        written = next;
                    } else {
                        assert next==written;
                    }

                    // character position of this annotation in this line
                    final int charPos = strBuf.length();

                    int rest = sz - next;
                    ByteArrayInputStream b = new ByteArrayInputStream(in, next, rest);

                    try {
                        final ConsoleAnnotation a = ConsoleAnnotation.readFrom(new DataInputStream(b));
                        if (a!=null) {
                            annotators.add(new ConsoleAnnotator<T>() {
                                public ConsoleAnnotator annotate(T context, MarkupText text) {
                                    return a.annotate(context,text,charPos);
                                }
                            });
                        }
                    } catch (IOException e) {
                        // if we failed to resurrect an annotation, ignore it.
                        LOGGER.log(Level.FINE,"Failed to resurrect annotation",e);
                    } catch (ClassNotFoundException e) {
                        LOGGER.log(Level.FINE,"Failed to resurrect annotation",e);
                    }

                    int bytesUsed = rest - b.available(); // bytes consumed by annotations
                    written += bytesUsed;


                    next = ConsoleAnnotation.findPreamble(in,written,sz-written);
                }
                // finish the remaining bytes->chars conversion
                lineOut.write(in,written,sz-written);
            }

            // aggregate newly retrieved ConsoleAnnotators into the current one.
            if (ann!=null)      annotators.add(ann);
            ann = ConsoleAnnotator.combine(annotators);
        }

        lineOut.flush();
        MarkupText mt = new MarkupText(strBuf.toString());
        if (ann!=null)
            ann = ann.annotate(context,mt);
        out.write(mt.toString());

        // reuse the buffer under normal circumstances, but don't let the line buffer grow unbounded
        if (buf.size()>4096)
            buf = new ByteArrayOutputStream2();
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
        out.flush();
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
        out.close();
    }

    private static final int LF = 0x0A;

    /**
     * {@link StringWriter} enhancement that's capable of shrinking the buffer size.
     *
     * <p>
     * The problem is that {@link StringBuffer#setLength(int)} doesn't actually release
     * the underlying buffer, so for us to truncate the buffer, we need to create a new {@link StringWriter} instance.
     */
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
