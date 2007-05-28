package hudson.tasks;

import hudson.remoting.VirtualChannel;
import hudson.model.AutomaticConfiguration;
import hudson.tasks.autoconf.OSProvider;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * Support for autoconfiguration of nodes.
 *
 * @author Stephen Connolly
 */
public interface AutoConfProvider {
    /**
     * Find the labels that the node supports.
     * @return a set of labels.
     */
    Set<AutomaticConfiguration> getAvailableConfigurations(VirtualChannel channel);
}
