package hudson.os.windows.wmi;

import org.kohsuke.jinterop.JIProxy;

/**
 * @author Kohsuke Kawaguchi
 */
public interface Win32OperatingSystem extends JIProxy {
    void Shutdown();
    void Reboot();
    // 4 = force flag
    // 0 logoff, 1 shutdown, 2 reboot, 8 poweroff
    void Win32Shutdown(int flags, int _);
}
