package hudson.util;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * {@link FilterOutputStream} that counts the number of bytes that were written.
 *
 * @author Kohsuke Kawaguchi
 */
public class CountingOutputStream extends FilterOutputStream {
    private int count = 0;

    public int getCount() {
        return count;
    }

    public CountingOutputStream(OutputStream out) {
        super(out);
    }

    public void write(int b) throws IOException {
        out.write(b);
        count++;
    }

    public void write(byte b[]) throws IOException {
        out.write(b);
        count += b.length;
    }

    public void write(byte b[], int off, int len) throws IOException {
        out.write(b, off, len);
        count += len;
    }
}
