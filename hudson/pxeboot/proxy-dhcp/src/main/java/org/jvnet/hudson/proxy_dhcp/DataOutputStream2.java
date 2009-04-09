package org.jvnet.hudson.proxy_dhcp;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Inet4Address;

/**
 * @author Kohsuke Kawaguchi
 */
class DataOutputStream2 extends DataOutputStream {
    DataOutputStream2(OutputStream out) {
        super(out);
    }

    public void writeAddress(Inet4Address adrs) throws IOException {
        if(adrs==null)
            write(new byte[4]);
        else
            write(adrs.getAddress());
    }

    public void writeFixedLengthNullTerminatedString(String s, int len) throws IOException {
        if(s==null) s="";
        byte[] bytes = s.getBytes("US-ASCII");
        write(bytes);
        for(int i=bytes.length; i<len; i++)
            write(0);
    }
}
