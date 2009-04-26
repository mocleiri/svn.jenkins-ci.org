package org.jvnet.hudson.pxeboot;

import org.jvnet.hudson.proxy_dhcp.ProxyDhcpService;

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
        if(args.length!=1) {
            System.out.println("Usage: java -jar hudson-proxy-dhcp.jar [URL of Hudson]");
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

        new ProxyDhcpService((Inet4Address) InetAddress.getByName(url.getHost()),"pxelinux.0").run();
    }

    private static final Logger LOGGER = Logger.getLogger(HudsonProxyDHCP.class.getName());
}
