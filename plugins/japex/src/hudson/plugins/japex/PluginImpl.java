package hudson.plugins.japex;

import hudson.tasks.BuildStep;
import hudson.Plugin;

/**
 * @author Kohsuke Kawaguchi
 */
public class PluginImpl extends Plugin {
    public void start() throws Exception {
        BuildStep.PUBLISHERS.add(JapexPublisher.DESCRIPTOR);
    }
}
