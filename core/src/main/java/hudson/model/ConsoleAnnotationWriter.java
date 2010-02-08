package hudson.model;

import hudson.MarkupText;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Kohsuke Kawaguchi
 */
public final class ConsoleAnnotationWriter extends Writer {
    private StringBuilder buf = new StringBuilder();
    private Writer base;

    public ConsoleAnnotationWriter(Writer base) {
        this.base = base;
    }

    private void eol() throws IOException {
        MarkupText mt = new MarkupText(buf.toString());
        {// test
            if (mt.length()>2)
                mt.addMarkup(0,2,"<font color=red>","</font>");
        }
        base.write(mt.toString());

        // reuse the buffer under normal circumstances, but don't let the line buffer grow unbounded
        if (buf.length()>4096)
            buf = new StringBuilder();
        else
            buf.setLength(0);
    }

    public void write(int c) throws IOException {
        buf.append(c);
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
        eol();
        base.close();
    }

    private static final int LF = 0x0A;
}
