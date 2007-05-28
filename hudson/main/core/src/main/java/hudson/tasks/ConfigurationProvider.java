package hudson.tasks;

import hudson.ExtensionPoint;
import hudson.tasks.autoconf.OSProvider;
import hudson.tasks.autoconf.PathSearcher;
import hudson.model.AutomaticConfiguration;
import hudson.remoting.VirtualChannel;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 *
 * @author connollys
 * @since 25-May-2007 14:30:15
 */
public abstract class ConfigurationProvider implements AutoConfProvider, ExtensionPoint {
    public static final List<ConfigurationProvider> PROVIDERS = new ArrayList<ConfigurationProvider>();
    static {
        PROVIDERS.add(OSProvider.INSTANCE);
        PROVIDERS.add(PathSearcher.INSTANCE);
    }

    /**
     * Find the labels that the node supports.
     *
     * @return a set of labels.
     */
    public Set<AutomaticConfiguration> getAvailableConfigurations(VirtualChannel channel) {
        return new HashSet<AutomaticConfiguration>();
    }
}
