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
 * @author Kohsuke Kawaguchi
 */
public class Server extends Closure implements Serializable {
    // this object gets sucked into the serialization graph, but don't let the channel serialized
    private transient Channel channel;

    public Server(Object owner, Channel channel) {
        super(owner);
        this.channel = channel;
    }

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

    public void close() throws IOException, InterruptedException {
        channel.close();
        channel.join();
    }
}
