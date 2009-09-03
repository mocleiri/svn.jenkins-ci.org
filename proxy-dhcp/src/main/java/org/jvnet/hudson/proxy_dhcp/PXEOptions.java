package org.jvnet.hudson.proxy_dhcp;

/**
 * @author Kohsuke Kawaguchi
 */
public class PXEOptions {
    public static DHCPOption createDiscoveryControl() {
        return new DHCPOption(DHCPOption.OPTION_VENDOR_ENCAPSULATED_OPTIONS,new byte[]{PXE_DISCOVERY_CONTROL,1,8});
    }

    public static final byte PXE_DISCOVERY_CONTROL = 6;
}
