package org.jvnet.hudson.proxy_dhcp;

import static org.jvnet.hudson.proxy_dhcp.DHCPOption.*;
import org.jvnet.hudson.pxeboot.DataInputStream2;
import org.jvnet.hudson.pxeboot.DataOutputStream2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a DHCP packet.
 *
 * @author Kohsuke Kawaguchi
 * @see RFC 2131 (http://www.ietf.org/rfc/rfc2131.txt)
 */
public class DHCPPacket {
    public byte op;        // message opcode
    public byte htype;
    public byte hlen;
    public byte hops;
    public int xid;
    public short secs;
    public short flags;

    public Inet4Address ciaddr; // 4 bytes each
    public Inet4Address yiaddr;
    public Inet4Address siaddr;
    public Inet4Address giaddr;

    public byte[] chaddr;   // 16 bytes
    public String sname;    // null-terminated 64 bytes
    public String file;     // null-terminated 128 bytes

    // 4 magic bytes here. see OPTIONS_COOKIE

    public List<DHCPOption> options = new ArrayList<DHCPOption>();

    /**
     * First 4 bytes of the options field  is this value
     */
    private static final byte[] OPTIONS_COOKIE = {99, (byte)130, 83, 99};

    public static final byte OP_BOOTREQUEST = 1;
    public static final byte OP_BOOTREPLY = 2;

    /**
     * Creates an empty packet.
     */
    public DHCPPacket() {
    }

    /**
     * Populates a packet from {@link DatagramPacket}.
     *
     * @throws IllegalArgumentException
     *      If the packet is not a DHCP packet.
     */
    public DHCPPacket(DatagramPacket p) throws IOException {
        DataInputStream2 di = new DataInputStream2(p);
        op = di.readByte();
        htype = di.readByte();
        hlen = di.readByte();
        hops = di.readByte();

        xid = di.readInt();
        secs = di.readShort();
        flags = di.readShort();

        ciaddr = di.readInet4Address();
        yiaddr = di.readInet4Address();
        siaddr = di.readInet4Address();
        giaddr = di.readInet4Address();

        chaddr = new byte[16];
        di.readFully(chaddr);

        sname = di.readFixedLengthNullTerminatedString(64);
        file = di.readFixedLengthNullTerminatedString(128);

        byte[] magic = di.readByteArray(4);
        if(!Arrays.equals(magic,OPTIONS_COOKIE))
            throw new IllegalArgumentException("Not a valid DHCP packet: incorrect magic value");

        while(di.available()>0) {   // available() isn't guaranteed to work on InputStream level, but ByteArrayInputStream implements it correctly
            DHCPOption opt = new DHCPOption(di);
            if(opt.isEnd())     return;
            options.add(opt);
        }
    }

    /**
     * Finds a DHCP option of the given tag
     */
    public DHCPOption getOption(byte tag) {
        for (DHCPOption option : options)
            if(option.tag==tag)
                return option;
        return null;
    }

    /**
     * Is this a DHCP message of the given type.
     */
    public boolean is(DHCPMessageType type) {
        DHCPOption o = getOption(OPTION_DHCP_MESSAGE_TYPE);
        return o!=null && type.code==o.data[0];
    }

    public String getVendorClassIdentifier() {
        DHCPOption o = getOption(OPTION_VENDOR_CLASS_IDENTIFIER);
        if(o==null) return null;
        return o.getDataAsString();
    }

    public DHCPPacket createResponse() {
        DHCPPacket that = new DHCPPacket();
        that.op = OP_BOOTREPLY;
        that.htype = this.htype;
        that.hlen = this.hlen;
        that.xid = this.xid;
        that.flags = this.flags;

        return that;
    }

    /**
     * Package a packet into {@link DatagramPacket}
     */
    public DatagramPacket pack() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream2 o = new DataOutputStream2(baos);

        o.writeByte(op);
        o.writeByte(htype);
        o.writeByte(hlen);
        o.writeByte(hops);

        o.writeInt(xid);
        o.writeShort(secs);
        o.writeShort(flags);

        o.writeAddress(ciaddr);
        o.writeAddress(yiaddr);
        o.writeAddress(siaddr);
        o.writeAddress(giaddr);

        o.write(chaddr);

        o.writeFixedLengthNullTerminatedString(sname,64);
        o.writeFixedLengthNullTerminatedString(file,128);

        o.write(OPTIONS_COOKIE);

        for (DHCPOption opt : options)
            opt.writeTo(o);

        // end with the end option
        o.writeByte(OPTION_END);


        byte[] buf = baos.toByteArray();
        return new DatagramPacket(buf,buf.length);
    }
}
