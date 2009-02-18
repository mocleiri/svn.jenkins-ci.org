package hudson.os.windows.wmi;

import org.kohsuke.jinterop.JIProxy;

/**
 * @author Kohsuke Kawaguchi
 */
public interface SWbemLocator extends JIProxy {
    SWbemServices ConnectServer(String server, String namespace, String user, String password, String locale, String authority, int securityFlags, Object objwbemNamedValueSet);
}
