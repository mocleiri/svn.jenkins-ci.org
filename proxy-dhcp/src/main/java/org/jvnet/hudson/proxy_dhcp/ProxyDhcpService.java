package org.jvnet.hudson.proxy_dhcp;

import static org.jvnet.hudson.proxy_dhcp.DHCPMessageType.DHCPOFFER;
import static org.jvnet.hudson.proxy_dhcp.DHCPOption.OPTION_DHCP_SERVER_IDENTIFIER;
import static org.jvnet.hudson.proxy_dhcp.DHCPPacket.OP_BOOTREQUEST;

import java.io.IOException;
import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;

/**
 * DHCP 
 *
 * @author Kohsuke Kawaguchi
 */
public class ProxyDhcpService implements Runnable, Closeable {

    public final Inet4Address tftpServer;
    public final String bootFileName;
    private final DatagramSocket server;

    public ProxyDhcpService(Inet4Address tftpServer, String bootFileName) throws SocketException {
        this(tftpServer,bootFileName,null);
    }
    
    public ProxyDhcpService(Inet4Address tftpServer, String bootFileName, InetAddress interfaceToListen) throws SocketException {
        this.tftpServer = tftpServer;
        this.bootFileName = bootFileName;

        server = new DatagramSocket(DHCP_SERVER_PORT,interfaceToListen);
        server.setBroadcast(true);

        LOGGER.info("TFTP server: "+tftpServer.getHostAddress());
        LOGGER.info("Boot file: "+bootFileName);
    }

    public void run() {
        try {
            execute();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,"IO exception in proxy DHCP service",e);
        }
    }

    public void execute() throws IOException {
        DatagramPacket datagram = new DatagramPacket(new byte[8192],8192);
        while(true) {
            server.receive(datagram);
            LOGGER.fine("Got a packet from "+datagram.getSocketAddress());
            try {
                handle(server, datagram);
            } catch (IOException e) {
                LOGGER.log(INFO,"Failed to handle a DHCP packet",e);
            }
        }
    }

    public void close() {
        server.close();
    }

    private void handle(DatagramSocket server, DatagramPacket datagram) throws IOException {
        DHCPPacket packet = new DHCPPacket(datagram);
        if(packet.op!=OP_BOOTREQUEST) {
            LOGGER.fine("Not a BOOT request: "+packet.op);
            return;
        }

        if(!packet.is(DHCPMessageType.DHCPDISCOVER)) {
            LOGGER.fine("Not a DHCPDISCOVER");
            return;
        }

        String vendorClass = packet.getVendorClassIdentifier();
        if(vendorClass==null || !vendorClass.startsWith("PXEClient")) {
            LOGGER.fine("Not a PXEClient: "+vendorClass);
            return;
        }

        // at this point we think someone is PXE booting and we need to tell it where the boot image is
        DHCPPacket reply = packet.createResponse();
        reply.siaddr = tftpServer;
        reply.chaddr = packet.chaddr;
        reply.file = bootFileName;
        reply.options.add(DHCPOFFER.createOption()); // DHCP offer
        reply.options.add(new DHCPOption(OPTION_DHCP_SERVER_IDENTIFIER,tftpServer)); // set server identifier. not sure what to set to.
        reply.options.add(DHCPOption.createVendorClassIdentifier("PXEClient")); // set vendor class
        reply.options.add(PXEOptions.createDiscoveryControl());

        // send back the response
        datagram = reply.pack();
        datagram.setAddress(InetAddress.getByName("255.255.255.255"));
        datagram.setPort(DHCP_CLIENT_PORT);
        server.send(datagram);
        LOGGER.fine("responded");
    }

    private static final int DHCP_CLIENT_PORT = 68;
    private static final int DHCP_SERVER_PORT = 67;

    private static final Logger LOGGER = Logger.getLogger(ProxyDhcpService.class.getName());

    public static void main(String[] args) throws IOException {
        // take logging into our own hands
        LOGGER.setLevel(Level.ALL);
        ConsoleHandler h = new ConsoleHandler();
        h.setLevel(Level.ALL);
        LOGGER.addHandler(h);
        LOGGER.setUseParentHandlers(false);

        new ProxyDhcpService((Inet4Address) InetAddress.getByName(args[0]),args[1]).run();
    }
}
