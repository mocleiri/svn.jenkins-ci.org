package hudson.util;

import hudson.CloseProofOutputStream;
import hudson.model.TaskListener;
import hudson.remoting.RemoteOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;

/**
 * {@link TaskListener} that generates output into a single stream.
 *
 * <p>
 * This object is remotable.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class StreamTaskListener implements TaskListener, Serializable {
    private PrintStream out;

    public StreamTaskListener(PrintStream out) {
        this.out = out;
    }

    public StreamTaskListener(OutputStream out) {
        this(new PrintStream(out));
    }

    public StreamTaskListener(File out) throws FileNotFoundException {
        // don't do buffering so that what's written to the listener
        // gets reflected to the file immediately, which can then be
        // served to the browser immediately
        this(new FileOutputStream(out));
    }

    public StreamTaskListener(Writer w) {
        this(new WriterOutputStream(w));
    }

    public PrintStream getLogger() {
        return out;
    }

    public PrintWriter error(String msg) {
        out.println(msg);
        return new PrintWriter(new OutputStreamWriter(out),true);
    }

    public void error(String format, Object... args) {
        out.printf(format,args);
    }

    public PrintWriter fatalError(String msg) {
        return error(msg);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(new RemoteOutputStream(new CloseProofOutputStream(this.out)));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        out = new PrintStream((OutputStream)in.readObject(),true);
    }

    public void close() {
        out.close();
    }

    private static final long serialVersionUID = 1L;
}
