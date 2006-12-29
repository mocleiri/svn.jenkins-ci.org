package hudson.remoting;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Pipe for the remote {@link Callable} and the local program to talk to each other.
 *
 * <p>
 * There are two kinds of pipes. One is for having a local system write to a remote system,
 * and the other is for having a remote system write to a local system. Use
 * the different versions of the <tt>create</tt> method to create the appropriate kind
 * of pipes.
 *
 * <p>
 * Once created, {@link Pipe} can be sent to the remote system as a part of a serialization of
 * {@link Callable} between {@link Channel}s.
 * Once re-instantiated on the remote {@link Channel}, pipe automatically connects
 * back to the local instance and perform necessary set up.
 *
 * <p>
 * The local and remote system can then call {@link #getIn()} and {@link #getOut()} to
 * read/write bytes.
 *
 * <p>
 * Pipe can be only written by one system and read by the other system. It is an error to
 * send one {@link Pipe} to two remote {@link Channel}s, or send one {@link Pipe} to
 * the same {@link Channel} twice.
 *
 * <h2>Usage</h2>
 * <pre>
 * final Pipe p = Pipe.createLocalToRemote();
 *
 * channel.callAsync(new Callable() {
 *   public Object call() {
 *     InputStream in = p.getIn();
 *     ... read from in ...
 *   }
 * });
 *
 * OutputStream out = p.getOut();
 * ... write to out ...
 * </pre>
 *
 * <h2>Implementation Note</h2>
 * <p>
 * For better performance, {@link Pipe} uses lower-level {@link Command} abstraction
 * to send data, instead of typed proxy object. This allows the writer to send data
 * without blocking until the arrival of the data is confirmed.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Pipe implements Serializable {
    private InputStream in;
    private OutputStream out;

    private Pipe(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    /**
     * Gets the reading end of the pipe.
     */
    public InputStream getIn() {
        return in;
    }

    /**
     * Gets the writing end of the pipe.
     */
    public OutputStream getOut() {
        return out;
    }

    /**
     * Creates a {@link Pipe} that allows remote system to write and local system to read.
     */
    public static Pipe createRemoteToLocal() {
        // OutputStream will be created on the target
        return new Pipe(new PipedInputStream(),null);
    }

    /**
     * Creates a {@link Pipe} that allows local system to write and remote system to read.
     */
    public static Pipe createLocalToRemote() {
        return new Pipe(null,new ProxyOutputStream());
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        if(in!=null && out==null) {
            // remote will write to local
            PipedOutputStream pos = new PipedOutputStream((PipedInputStream)in);
            int oid = Channel.current().export(pos);

            oos.writeBoolean(true); // marker
            oos.writeInt(oid);
        } else {
            // remote will read from local
            int oid = Channel.current().export(out);

            oos.writeBoolean(false);
            oos.writeInt(oid);
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        final Channel channel = Channel.current();
        assert channel !=null;

        if(ois.readBoolean()) {
            // local will write to remote
            in = null;
            out = new BufferedOutputStream(new ProxyOutputStream(channel, ois.readInt()));
        } else {
            // local will read from remote.
            // tell the remote system about this local read pipe

            // this is the OutputStream that wants to send data to us
            final int oidRos = ois.readInt();

            // we want 'oidRos' to send data to this PipedOutputStream
            PipedOutputStream pos = new PipedOutputStream();
            final int oidPos = channel.export(pos);

            // tell 'ros' to connect to our 'pos'.
            channel.send(new ConnectCommand(oidRos, oidPos));

            out = null;
            in = new PipedInputStream(pos);
        }
    }

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(Pipe.class.getName());

    private static class ConnectCommand extends Command {
        private final int oidRos;
        private final int oidPos;

        public ConnectCommand(int oidRos, int oidPos) {
            this.oidRos = oidRos;
            this.oidPos = oidPos;
        }

        protected void execute(Channel channel) {
            try {
                ProxyOutputStream ros = (ProxyOutputStream) channel.getExportedObject(oidRos);
                channel.unexport(oidRos);
                ros.connect(channel, oidPos);
            } catch (IOException e) {
                logger.severe("Failed to connect to pipe");
            }
        }
    }
}
