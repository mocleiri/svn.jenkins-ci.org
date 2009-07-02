package test;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.Closure;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Closeable;

import hudson.remoting.Channel;

/**
 * Represents a connection to a remote server.
 * 
 * @author Kohsuke Kawaguchi
 */
public class Server extends Closure implements Serializable {
    // this object gets sucked into the serialization graph, but don't let the channel serialized
    private transient Channel channel;

    public Server(Object owner, Channel channel) {
        super(owner);
        this.channel = channel;
    }

    /**
     * Executes a groovy closure on this server and returns the result back.
     *
     * <p>
     * From groovy, this can be invoked as if this object is a block, like this:
     *
     * <pre>
     * Server server = droovy.connec("db")
     * server {
     *   println "Hello";
     * }
     * </pre>
     */
    public Object doCall(Closure closure) throws Throwable {
        return channel.call(new ClosureAdapter(closure));
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        channel = Channel.current();
    }

    /**
     * Terminates the remote JVM and shuts down this server.
     */
    public void close() throws IOException, InterruptedException {
        channel.close();
        channel.join();
    }
}
