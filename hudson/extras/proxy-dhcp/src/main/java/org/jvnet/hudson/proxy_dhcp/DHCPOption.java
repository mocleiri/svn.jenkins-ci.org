package org.jvnet.hudson.proxy_dhcp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.net.Inet4Address;

/**
 * @author Kohsuke Kawaguchi
 * @see RFC 2132 (http://www.ietf.org/rfc/rfc2132.txt)
 */
public class DHCPOption {
    public byte tag;
    public byte[] data;

    public DHCPOption() {
    }

    public DHCPOption(byte tag, byte[] data) {
        this.tag = tag;
        this.data = data;
    }

    public DHCPOption(byte tag, Inet4Address adrs) {
        this(tag,adrs.getAddress());
    }

    DHCPOption(DataInputStream2 di) throws IOException {
        tag = di.readByte();
        if(tag==OPTION_END || tag==OPTION_PAD)
            return; // these options don't have payload
        byte len = di.readByte();
        data = di.readByteArray(uint(len));
    }

    private static int uint(byte b) {
        if(b>=0)    return b;
        return ((int)b)+256;
    }

    void writeTo(DataOutputStream2 o) throws IOException {
        o.writeByte(tag);
        o.writeByte(data.length); // TODO: handle sign correctly 
        o.write(data);
    }

    public String getDataAsString() {
        try {
            // DHCP spec doesn't say if the string is supposed to be null terminated or not,
            // but the C implementation that I saw assumes that. So handle both cases
            int i=0;
            while(i<data.length && data[i]!=0)
                i++;
            return new String(data,0,i,"US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e); // impossible
        }
    }

    /**
     * Is this the end marker?
     */
    public boolean isEnd() {
        return tag==OPTION_END;
    }

    public boolean isPad() {
        return tag==OPTION_PAD;
    }

    public static DHCPOption createVendorClassIdentifier(String c) {
        // again the DHCP spec isn't clear about whether the string should be null terminated,
        // but C implementations seem to assume that, so I'm following its lead.
        byte[] bytes = c.getBytes();
        byte[] bytesPlusOne = new byte[bytes.length+1];
        System.arraycopy(bytes,0,bytesPlusOne,0,bytes.length);
        return new DHCPOption(OPTION_VENDOR_CLASS_IDENTIFIER,bytesPlusOne);
    }

    public static final byte OPTION_PAD = 0;
    public static final byte OPTION_END = -1;
    public static final byte OPTION_DHCP_MESSAGE_TYPE = 53;
    public static final byte OPTION_DHCP_SERVER_IDENTIFIER = 54;
    public static final byte OPTION_VENDOR_CLASS_IDENTIFIER = 60;
    public static final byte OPTION_VENDOR_ENCAPSULATED_OPTIONS = 43;
}
