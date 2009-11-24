package org.jvnet.hudson.pxeboot;

import org.jvnet.hudson.proxy_dhcp.ProxyDhcpService;
import static org.jvnet.hudson.proxy_dhcp.ProxyDhcpService.DHCP_SERVER_PORT;

import java.io.File;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for proxy DHCP service for Hudson.
 * 
 * @author Kohsuke Kawaguchi
 */
public class HudsonProxyDHCP {
    public static void main(String[] args) throws Exception {
        final boolean isUnix = File.pathSeparatorChar == ':';

        if(args.length!=1) {
            String prefix="";
            if(isUnix) prefix="sudo ";
            System.out.println("Usage: "+prefix+"java -jar hudson-proxy-dhcp.jar [URL of Hudson]");
            System.exit(-1);
        }

        URL url = new URL(args[0]);

        // verify that this is indeed Hudson
        URLConnection con = url.openConnection();
        con.connect();
        if(con.getHeaderField("X-Hudson")==null) {
            System.out.println(args[0]+" doesn't look like Hudson");
            System.exit(-1);
        }

        // take logging into our own hands
        LOGGER.setLevel(Level.ALL);
        ConsoleHandler h = new ConsoleHandler();
        h.setLevel(Level.ALL);
        LOGGER.addHandler(h);
        LOGGER.setUseParentHandlers(false);

        try {
            new ProxyDhcpService((Inet4Address) InetAddress.getByName(url.getHost()),"pxelinux.0").run();
        } catch (BindException e) {
            if(isUnix)
                System.out.println("Failed to bind to port "+DHCP_SERVER_PORT+". Make sure you are running this as root");
            throw e;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(HudsonProxyDHCP.class.getName());
}
