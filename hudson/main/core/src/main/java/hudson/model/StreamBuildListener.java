package hudson.model;

import hudson.remoting.RemoteOutputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;

/**
 * {@link BuildListener} that writes to an {@link OutputStream}.
 *
 * This class is remotable.
 * 
 * @author Kohsuke Kawaguchi
 */
public class StreamBuildListener implements BuildListener, Serializable {
    private PrintWriter w;

    private PrintStream ps;

    public StreamBuildListener(OutputStream w) {
        this(new PrintStream(w));
    }

    public StreamBuildListener(PrintStream w) {
        this.ps = w;
        // unless we auto-flash, PrintStream will use BufferedOutputStream internally,
        // and break ordering
        this.w = new PrintWriter(w,true);
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
        out.writeObject(new RemoteOutputStream(ps));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ps = new PrintStream((OutputStream)in.readObject(),true);
        w = new PrintWriter(ps,true);
    }

    private static final long serialVersionUID = 1L;
}
