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
package hudson.model;

import hudson.MarkupText;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Kohsuke Kawaguchi
 */
public class ConsoleAnnotationWriter extends Writer {
    private final Writer base;
    private final Run<?,?> build;
    private StringBuilder buf = new StringBuilder();
    private ConsoleAnnotator ann;

    public ConsoleAnnotationWriter(Writer base, ConsoleAnnotator ann, Run<?, ?> build) {
        this.base = base;
        this.ann = ann;
        this.build = build;
    }

    public ConsoleAnnotator getConsoleAnnotator() {
        return ann;
    }

    private void eol() throws IOException {
        MarkupText mt = new MarkupText(buf.toString());
        if (ann!=null)
            ann = ann.annotate(build,mt);
        base.write(mt.toString());

        // reuse the buffer under normal circumstances, but don't let the line buffer grow unbounded
        if (buf.length()>4096)
            buf = new StringBuilder();
        else
            buf.setLength(0);
    }

    public void write(int c) throws IOException {
        buf.append((char)c);
        if (c==LF)  eol();
    }

    public void write(char[] buf, int off, int len) throws IOException {
        int end = off+len;

        for( int i=off; i<end; i++ )
            write(buf[i]);
    }

    public void write(String str, int off, int len) throws IOException {
        int end = off+len;

        for( int i=off; i<end; i++ )
            write(str.charAt(i));
    }

    @Override
    public void flush() throws IOException {
        base.flush();
    }

    @Override
    public void close() throws IOException {
        if (buf.length()>0) {
            /*
                because LargeText cuts output at the line end boundary, this is
                possible only for the very end of the console output, if the output ends without NL.
             */
            eol();
        }
        base.close();
    }

    private static final int LF = 0x0A;
}
