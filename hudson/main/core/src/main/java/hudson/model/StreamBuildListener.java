package hudson.model;

import hudson.remoting.RemoteWriter;
import hudson.util.WriterOutputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;

/**
 * {@link BuildListener} that writes to a {@link Writer}.
 *
 * This class is remotable.
 * 
 * @author Kohsuke Kawaguchi
 */
public class StreamBuildListener implements BuildListener, Serializable {
    private PrintWriter w;

    private PrintStream ps;

    public StreamBuildListener(Writer w) {
        this(new PrintWriter(w));
    }

    public StreamBuildListener(PrintWriter w) {
        this.w = w;
        // unless we auto-flash, PrintStream will use BufferedOutputStream internally,
        // and break ordering
        this.ps = new PrintStream(new WriterOutputStream(w),true);
    }

    public void started() {
        w.println("started");
    }

    public PrintStream getLogger() {
        return ps;
    }

    public PrintWriter error(String msg) {
        w.println("ERROR: "+msg);
        return w;
    }

    public PrintWriter fatalError(String msg) {
        w.println("FATAL: "+msg);
        return w;
    }

    public void finished(Result result) {
        w.println("finished: "+result);
    }


    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(new RemoteWriter(w));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        w = new PrintWriter((Writer)in.readObject(),true);
        ps = new PrintStream(new WriterOutputStream(w),true);
    }

    private static final long serialVersionUID = 1L;
}
