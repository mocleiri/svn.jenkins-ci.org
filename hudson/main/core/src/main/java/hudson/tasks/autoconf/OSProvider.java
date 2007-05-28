package hudson.tasks.autoconf;

import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tasks.ConfigurationProvider;
import hudson.tasks.BuildStep;
import hudson.model.AutomaticConfiguration;
import hudson.model.AbstractAutomaticConfiguration;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 *
 * @author connollys
 * @since 25-May-2007 15:25:03
 */
public class OSProvider extends ConfigurationProvider {

    public static OSProvider INSTANCE = new OSProvider();

    private OSProvider() {
    }

    /**
     * Find the labels that the node supports.
     *
     * @return a set of labels.
     */
    public Set<AutomaticConfiguration> getAvailableConfigurations(VirtualChannel channel) {
        try {
            return channel.call(new OSFinder());
        } catch (Exception e) {
            return new HashSet<AutomaticConfiguration>();
        }
    }

    private static class OSFinder implements Callable<Set<AutomaticConfiguration>, Exception> {
        /** Performs computation and returns the result, or throws some exception. */
        public Set<AutomaticConfiguration> call() throws Exception {
            Set<AutomaticConfiguration> result = new HashSet<AutomaticConfiguration>();
            final String os = System.getProperty("os.name").toLowerCase();
            final String version = System.getProperty("os.version");
            final String arch = System.getProperty("os.arch");
            if (os.equals("solaris") || os.equals("SunOS")) {
                result.add(new OperatingSystemConfiguration("solaris"));
                result.add(new OperatingSystemConfiguration("solaris_" + arch));
                result.add(new OperatingSystemConfiguration("solaris_" + arch + "_" + version));
            } else if (os.startsWith("windows")) {
                result.add(new OperatingSystemConfiguration("windows"));
                if (os.startsWith("windows 9")) {
                    // ugh! windows 9x
                    // I have not tested these values
                    result.add(new OperatingSystemConfiguration("windows_9x_family"));
                    if (version.startsWith("4.0")) {
                        result.add(new OperatingSystemConfiguration("windows_95"));
                    } else if (version.startsWith("4.9")) {
                        result.add(new OperatingSystemConfiguration("windows_ME")); // but could be Windows ME
                    } else {
                        assert version.startsWith("4.1");
                        result.add(new OperatingSystemConfiguration("windows_98"));
                    }
                } else {
                    // older Java Runtimes can mis-report newer versions of windows NT
                    result.add(new OperatingSystemConfiguration("windows_nt_family"));
                    if (version.startsWith("4.0")) {
                        // Windows NT 4
                        result.add(new OperatingSystemConfiguration("windows_nt4"));
                    } else if (version.startsWith("5.0")) {
                        result.add(new OperatingSystemConfiguration("windows_2000"));
                    } else if (version.startsWith("5.1")) {
                        result.add(new OperatingSystemConfiguration("windows_xp"));
                    } else if (version.startsWith("5.2")) {
                        result.add(new OperatingSystemConfiguration("windows_2003"));
                    }
                }
            } else if (os.startsWith("linux")) {
                result.add(new OperatingSystemConfiguration("linux"));
            } else if (os.startsWith("mac")) {
                result.add(new OperatingSystemConfiguration("mac"));
            } else {
                // I give up!
                result.add(new OperatingSystemConfiguration(os));
            }
            return result;
        }
    }

    private static class OperatingSystemConfiguration extends AbstractAutomaticConfiguration {
        public OperatingSystemConfiguration(String name) {
            super(name);
        }
    }
}
