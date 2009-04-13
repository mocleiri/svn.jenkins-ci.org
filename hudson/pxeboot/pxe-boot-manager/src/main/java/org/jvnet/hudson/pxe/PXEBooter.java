package org.jvnet.hudson.pxe;

import org.jvnet.hudson.proxy_dhcp.ProxyDhcpService;
import org.jvnet.hudson.tftpd.TFTPServer;
import org.jvnet.hudson.tftpd.PathResolver;
import org.jvnet.hudson.tftpd.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.Enumeration;

/**
 *
 * @author Kohsuke Kawaguchi
 */
public class PXEBooter {
    public static void main(String[] args) throws IOException {
        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers())
            root.removeHandler(h);
        Logger logger = Logger.getLogger("org.jvnet.hudson");
        logger.setLevel(Level.ALL);
        ConsoleHandler h = new ConsoleHandler();
        h.setLevel(Level.ALL);
        logger.addHandler(h);

        ProxyDhcpService dhcp = new ProxyDhcpService((Inet4Address) InetAddress.getByName(args[0]), "pxelinux.0");
        start(dhcp);

        // serve up resources
        TFTPServer tftp = new TFTPServer(new PathResolver() {
            private final ClassLoader classLoader = PXEBooter.class.getClassLoader();
            public Data open(final String fileName) throws IOException {
                if(fileName.equals("pxelinux.cfg/default")) {
                    // combine all pxelinux.cfg.fragment files into one and serve them
                    return new Data() {
                        public InputStream read() throws IOException {
                            InputStream is = classLoader.getResourceAsStream("tftp/"+fileName);
                            // merge all fragments
                            Enumeration<URL> e = classLoader.getResources("pxelinux.cfg.fragment");
                            while (e.hasMoreElements()) {
                                URL url = e.nextElement();
                                is = new SequenceInputStream(is,url.openStream());
                            }
                            return is;
                        }
                    };
                }

                // default
                URL res = classLoader.getResource("tftp/" + fileName.replace('\\', '/'));
                if(res!=null)
                    return Data.fromURL(res);

                throw new IOException("No such file: "+fileName);
            }
        });
        start(tftp);

        new BufferedReader(new InputStreamReader(System.in)).readLine();
        dhcp.close();
        tftp.close();
    }

    private static void start(Runnable task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
