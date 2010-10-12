package org.jvnet.hudson.tftpd;

import org.jvnet.hudson.tftpd.impl.TFTP;
import org.jvnet.hudson.tftpd.impl.TFTPAckPacket;
import org.jvnet.hudson.tftpd.impl.TFTPDataPacket;
import org.jvnet.hudson.tftpd.impl.TFTPErrorPacket;
import static org.jvnet.hudson.tftpd.impl.TFTPErrorPacket.FILE_NOT_FOUND;
import org.jvnet.hudson.tftpd.impl.TFTPOAckPacket;
import org.jvnet.hudson.tftpd.impl.TFTPPacket;
import org.jvnet.hudson.tftpd.impl.TFTPPacketException;
import org.jvnet.hudson.tftpd.impl.TFTPReadRequestPacket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;

/**
 * TFTP server.
 *
 * @author Kohsuke Kawaguchi
 */
public class TFTPServer implements Runnable {
    private final PathResolver resolver;
    private final TFTP tftp = new TFTP();

    public TFTPServer(PathResolver resolver) {
        this.resolver = resolver;
        tftp.setDefaultTimeout(0); // infinite
    }

    /**
     * Calls {@link #execute()} but handles the IOException by itself.
     */
    public void run() {
        try {
            execute();
        } catch (IOException e) {
            if("Socket closed".equals(e.getMessage()))
                // there's no reliable way to detect this situation
                LOGGER.fine("TFTP accept thread closed");
            else
                LOGGER.log(INFO, "IOException in the TFTP accept thread",e);
        }
    }

    /**
     * Main loop of the TFTP server.
     *
     * <p>
     * Blocks until {@link #close()} is invoked to initiate the shutdown.
     */
    public void execute() throws IOException {
        tftp.open(TFTP_PORT);
        LOGGER.fine("TFTP server ready for action");

        try {
            while(true) {
                try {
                    TFTPPacket p = tftp.receive();
                    if (p instanceof TFTPReadRequestPacket) {
                        TFTPReadRequestPacket rp = (TFTPReadRequestPacket) p;
                        LOGGER.fine("Starting a new session to transfer "+rp.getFilename());
                        new TFTPSession(rp); // this starts a new thread if necessary
                    } else {
                        LOGGER.fine("Unexpected packet "+p);
                    }
                } catch (TFTPPacketException e) {
                    LOGGER.log(FINE, "Invalid packet received",e);
                }
            }
        } finally {
            close();
        }
    }

    /**
     * Closes the daemon.
     */
    public synchronized void close() {
        if(tftp.isOpen())
            tftp.close();
    }

    /**
     * Each TFTP session is handled by a separate thread.
     */
    final class TFTPSession extends TFTP implements Runnable {
        private final String fileName;
        private /*final*/ Data data;
        private /*final*/ InputStream stream;
        private final InetSocketAddress dest;
        private final TFTPReadRequestPacket session;
        private int blockSize = TFTPPacket.SEGMENT_SIZE;


        TFTPSession(TFTPReadRequestPacket rp) throws IOException {
            open();
            session = rp;
            fileName = rp.getFilename();
            dest = new InetSocketAddress(rp.getAddress(),rp.getPort());
            try {
                data = resolver.open(fileName);
                if(data ==null) // this is against the contract, but be defensive.
                    sendError(FILE_NOT_FOUND, "no such file exists: "+fileName);
                else {
                    stream = data.read();
                    if(stream==null)
                        sendError(FILE_NOT_FOUND, "no such file exists: "+fileName);
                    else
                        new Thread(this).start();
                }
            } catch (IOException e) {
                sendError(FILE_NOT_FOUND, "failed to read "+fileName+" : "+e.getMessage());
                LOGGER.log(Level.INFO,"Failed to read "+fileName,e);
            }
        }

        /**
         * Sends a TFTP error packet.
         */
        private void sendError(int errorCode, String msg) throws IOException {
            LOGGER.info("Error: "+msg+" -> "+dest);
            send(new TFTPErrorPacket(dest.getAddress(),dest.getPort(), errorCode,msg));
        }

        /**
         * Reads the next data packet out of the {@link #data}, in a way that follows the TFTP protocol.
         */
        private TFTPDataPacket readNextBlock(int blockNumber) throws IOException {
            byte[] buf = new byte[blockSize];
            int read = 0;

            while(true) {
                int chunk = stream.read(buf, read, buf.length-read);
                if(chunk<=0)
                    break;
                read += chunk;
            }
            return new TFTPDataPacket(dest.getAddress(),dest.getPort(),blockNumber,buf,0,read);
        }

        private void sendAndWait(TFTPPacket p, int expected) throws IOException {
            int retry = 0;
            while (true) {
                send(p);
                try {
                    if(waitAck(expected))
                        return; // got an ACK
                } catch (SocketTimeoutException e) {
                    if(retry++ > 5)
                        throw e; // too many retries. abort
                    LOGGER.fine("Retransmitting "+p);
                }
            }
        }

        private boolean waitAck(int expected) throws IOException {
            TFTPPacket p = receive();
            if (!(p instanceof TFTPAckPacket)) {
                if (p instanceof TFTPErrorPacket) {
                    TFTPErrorPacket ep = (TFTPErrorPacket) p;
                    LOGGER.info("Expecting ACK="+expected+" but got "+p+" :"+ep.getError()+":"+ep.getMessage());
                } else {
                    LOGGER.info("Expecting ACK="+expected+" but got "+p);
                    sendError(FILE_NOT_FOUND, "");
                }
                throw new IOException("Aborting");
            }

            TFTPAckPacket ack = (TFTPAckPacket) p;
            if(ack.getBlockNumber()!=expected) {
                LOGGER.info("Expecting ACK="+expected+" but got ACK for "+ack.getBlockNumber()+" instead. Retransmitting");
                return false;
            }

            return true;
        }

        public void run() {
            try {
                boolean hasTsize = session.options.containsKey("tsize");
                boolean hasBlksize = session.options.containsKey("blksize");
                if(hasTsize || hasBlksize) {
                    LOGGER.fine("Got options "+session.options);
                    TFTPOAckPacket oack = new TFTPOAckPacket(dest.getAddress(), dest.getPort());
                    if(hasTsize)
                        oack.options.put("tsize",String.valueOf(data.size()));
                    if(hasBlksize) {
                        blockSize = Integer.parseInt(session.options.get("blksize"));
                        oack.options.put("blksize",String.valueOf(blockSize));
                    }
                    sendAndWait(oack,0);
                }

                for( int blockNumber=1; ; blockNumber++ ) {
                    TFTPDataPacket dp = readNextBlock(blockNumber);
                    LOGGER.fine("Sending block #"+blockNumber+" ("+dp.getDataLength()+")");
                    sendAndWait(dp,blockNumber);

                    if(dp.getDataLength()<blockSize) {
                        LOGGER.fine("Transmission complete");
                        return;
                    }
                }
            } catch(IOException e) {
                LOGGER.log(INFO, "IO exception in TFTP session", e);
            } finally {
                LOGGER.fine("Closing a session");
                close();
                try {
                    if(stream!=null)
                        stream.close();
                } catch (IOException e) {
                    LOGGER.log(FINE, "Failed to close "+ data,e);
                }
            }
        }
    }

    private static final int TFTP_PORT = 69;
    private static final Logger LOGGER = Logger.getLogger(TFTPServer.class.getName());

    /**
     * Debug main method.
     */
    public static void main(String[] args) throws IOException {
        LOGGER.setLevel(Level.ALL);
        LOGGER.setUseParentHandlers(false);
        ConsoleHandler h = new ConsoleHandler();
        h.setLevel(Level.ALL);
        LOGGER.addHandler(h);

        // serve current directory
        TFTPServer server = new TFTPServer(new PathResolver() {
            public Data open(final String fileName) throws IOException {
                return new Data() {
                    public InputStream read() throws IOException {
                        return new FileInputStream(fileName);
                    }
                    public int size() {
                        return (int)new File(fileName).length();
                    }
                };
            }
        });
        new Thread(server).start();
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        server.close();
    }
}
