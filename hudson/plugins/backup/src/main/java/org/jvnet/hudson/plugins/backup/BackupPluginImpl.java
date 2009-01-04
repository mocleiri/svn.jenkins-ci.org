package org.jvnet.hudson.plugins.backup;

import hudson.Plugin;
import hudson.model.ManagementLink;
import hudson.tasks.BuildStep;

/**
 * Entry point of the Backup plugin
 *
 * @author vsellier
 */
public class BackupPluginImpl extends Plugin {
    public void start() throws Exception {
        ManagementLink.LIST.add(new BackupLink());
    }
}
