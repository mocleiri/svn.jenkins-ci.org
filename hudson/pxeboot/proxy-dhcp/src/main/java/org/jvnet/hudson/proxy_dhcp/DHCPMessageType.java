package org.jvnet.hudson.proxy_dhcp;

/**
 * @author Kohsuke Kawaguchi
 */
public enum DHCPMessageType {
    DHCPDISCOVER(1),
    DHCPOFFER(2),
    DHCPREQUEST(3),
    DHCPDECLINE(4),
    DHCPACK(5),
    DHCPNAK(6),
    DHCPRELEASE(7),
    DHCPINFORM(8),
    DHCPLEASEQUERY(10),
    DHCPLEASEUNASSIGNED(11),
    DHCPLEASEUNKNOWN(12),
    DHCPLEASEACTIVE(13);

    DHCPMessageType(int code) {
        this.code = code;
    }

    public final int code;

    public static DHCPMessageType byCode(int code) {
        for (DHCPMessageType t : values())
            if(t.code==code)
                return t;
        return null;
    }

    public DHCPOption createOption() {
        return new DHCPOption(DHCPOption.OPTION_DHCP_MESSAGE_TYPE,new byte[]{(byte)code});
    }
}
