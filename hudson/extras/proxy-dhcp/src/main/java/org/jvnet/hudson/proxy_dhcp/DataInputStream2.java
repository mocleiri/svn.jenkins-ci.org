package org.jvnet.hudson.proxy_dhcp;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;

/**
 * @author Kohsuke Kawaguchi
 */
class DataInputStream2 extends DataInputStream {
    public DataInputStream2(InputStream in) {
        super(in);
    }

    public DataInputStream2(DatagramPacket packet) {
        this(new ByteArrayInputStream(packet.getData()));
    }

    public Inet4Address readInet4Address() throws IOException {
        return (Inet4Address)Inet4Address.getByAddress(readByteArray(4));
    }

    public byte[] readByteArray(int size) throws IOException {
        byte[] b = new byte[size];
        readFully(b);
        return b;
    }

    public String readFixedLengthNullTerminatedString(int size) throws IOException {
        byte[] b = readByteArray(size);
        // find '\0'
        int i=0;
        while(i<b.length && b[i]!=0)    i++;
        return new String(b,0,i,"US-ASCII"); 
    }

}
