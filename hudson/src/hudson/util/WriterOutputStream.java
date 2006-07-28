package hudson.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * {@link OutputStream} that writes to {@link Writer}
 * by assuming the platform default encoding.
 *
 * @author Kohsuke Kawaguchi
 */
public class WriterOutputStream extends OutputStream {
    private final Writer writer;
    private final CharsetDecoder decoder;

    private ByteBuffer buf = ByteBuffer.allocate(1024);
    private CharBuffer out = CharBuffer.allocate(1024);

    public WriterOutputStream(Writer out) {
        this.writer = out;
        decoder = Charset.defaultCharset().newDecoder();
    }

    public void write(int b) throws IOException {
        if(buf.remaining()==0)
            decode(false);
        buf.put((byte)b);
    }

    public void write(byte b[], int off, int len) throws IOException {
        while(len>0) {
            if(buf.remaining()==0)
                decode(false);
            int sz = Math.min(buf.remaining(),len);
            buf.put(b,off,sz);
            off += sz;
            len -= sz;
        }
    }

    public void flush() throws IOException {
        decode(false);
        flushOutput();
        writer.flush();
    }

    private void flushOutput() throws IOException {
        writer.write(out.array(),0,out.position());
        out.clear();
    }

    public void close() throws IOException {
        decode(true);
        flushOutput();
        writer.close();

        buf.compact();
    }

    private void decode(boolean last) throws IOException {
        buf.flip();
        while(true) {
            CoderResult r = decoder.decode(buf, out, last);
            if(r==CoderResult.OVERFLOW) {
                flushOutput();
                continue;
            }
            if(r==CoderResult.UNDERFLOW) {
                buf.compact();
                return;
            }
            // otherwise treat it as an error
            r.throwException();
        }
    }
}
