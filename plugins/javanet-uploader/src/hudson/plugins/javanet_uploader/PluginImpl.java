package hudson.plugins.javanet_uploader;

import hudson.tasks.BuildStep;
import hudson.Plugin;

/**
 * @author Kohsuke Kawaguchi
 */
public class PluginImpl extends Plugin {
    public void start() throws Exception {
        BuildStep.PUBLISHERS.add(JNUploaderPublisher.DESCRIPTOR);
    }
}
