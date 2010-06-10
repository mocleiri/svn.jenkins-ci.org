package org.jvnet.hudson.tftpd.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * @author Kohsuke Kawaguchi
 */
public class TFTPOAckPacket extends TFTPPacket {
    public final Map<String,String> options = new TreeMap<String,String>();

    public TFTPOAckPacket(InetAddress destination, int port) {
        super(TFTPPacket.OACK, destination, port);
    }

    TFTPOAckPacket(DatagramPacket datagram) throws IOException {
        super(TFTPPacket.OACK, datagram.getAddress(),
              datagram.getPort());
        byte[] data;

        data = datagram.getData();
        DataInputStream di = new DataInputStream(new ByteArrayInputStream(data));

        if (getType() != di.readShort())
            throw new TFTPPacketException("TFTP operator code does not match type.");

        while(di.available()>0)
            options.put(readString(di),readString(di));
    }

    private String readString(DataInput di) throws IOException {
        StringBuilder buf = new StringBuilder();
        int ch;
        while((ch=di.readByte())!=0)
            buf.append((char)ch);
        return buf.toString();
    }

    @Override
    DatagramPacket _newDatagram(DatagramPacket datagram, byte[] data) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }


    @Override
    public DatagramPacket newDatagram() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeShort(_type);
            for (Entry<String,String> e : options.entrySet()) {
                dos.write(e.getKey().getBytes());
                dos.write(0);
                dos.write(e.getValue().getBytes());
                dos.write(0);
            }

            byte[] data = baos.toByteArray();

            return new DatagramPacket(data, data.length, _address, _port);
        } catch (IOException e) {
            throw new AssertionError(e); // impossible
        }
    }
}
