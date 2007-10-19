package org.jvnet.hudson.plugins.port_allocator;

import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.remoting.Callable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * Manages ports in use.
 *
 * @author Rama Pulavarthi
 */
final class PortAllocationManager {
    private final Computer node;


    /**
     * Ports currently in use, to the build that uses it.
     */
    private final Map<Integer,AbstractBuild> ports = new HashMap<Integer,AbstractBuild>();

    private static final Map<Computer, WeakReference<PortAllocationManager>> INSTANCES =
            new WeakHashMap<Computer, WeakReference<PortAllocationManager>>();

    private PortAllocationManager(Computer node) {
        this.node = node;
    }

    /**
     * Allocates a random port on the Computer where the jobs gets executed.
     *
     * <p>
     * If the preferred port is not available, assigns a random available port.
     *
     * @param prefPort Preffered Port
     */
    public synchronized int allocateRandom(AbstractBuild owner, int prefPort) throws InterruptedException, IOException {
        int i;
        try {
            // try to allocate preferential port,
            i = allocatePort(prefPort);
        } catch (PortUnavailableException ex) {
            // if not available, assign a random port
            i = allocatePort(0);
        }
        ports.put(i,owner);
        return i;
    }

    /**
     * Assigns the requested port if free or waits on it.
     */
    public synchronized int allocate(AbstractBuild owner, int port) throws InterruptedException, IOException {
        try {
            allocatePort(port);
            ports.put(port,owner);
            return port;
        } catch (IOException ex) {
            //TODO Requested Port is not available, wait for it
            // change this, for now assign a  free port
            throw ex;
        }
    }

    public static PortAllocationManager getManager(Computer node) {
        PortAllocationManager pam;
        WeakReference<PortAllocationManager> ref = INSTANCES.get(node);
        if (ref != null) {
            pam = ref.get();
            if (pam != null)
                return pam;
        }
        pam = new PortAllocationManager(node);
        INSTANCES.put(node, new WeakReference<PortAllocationManager>(pam));
        return pam;
    }

    public synchronized void free(int n) {
        ports.remove(n);
    }


//    private boolean isPortInUse(final int port) throws InterruptedException {
//        try {
//            return node.getChannel().call(new Callable<Boolean, IOException>() {
//                public Boolean call() throws IOException {
//                    new ServerSocket(port).close();
//                    return true;
//                }
//            });
//        } catch (IOException e) {
//            return false;
//        }
//    }

    /**
     * @param port 0 to assign a free port
     * @return port that gets assigned
     * @throws PortUnavailableException
     *      If the specified port is not availabale
     */
    private int allocatePort(final int port) throws InterruptedException, IOException {
        AbstractBuild owner = ports.get(port);
        if(owner!=null)
            throw new PortUnavailableException("Owned by "+owner);

        return node.getChannel().call(new Callable<Integer,IOException>() {
            public Integer call() throws IOException {
                ServerSocket server;
                try {
                    server = new ServerSocket(port);
                } catch (IOException e) {
                    // fail to bind to the port
                    throw new PortUnavailableException(e);
                }
                int localPort = server.getLocalPort();
                server.close();
                return localPort;
            }
        });
    }

    static final class PortUnavailableException extends IOException {
        PortUnavailableException(String msg) {
            super(msg);
        }

        PortUnavailableException(Throwable cause) {
            super(cause);
        }

        private static final long serialVersionUID = 1L;
    }
}
