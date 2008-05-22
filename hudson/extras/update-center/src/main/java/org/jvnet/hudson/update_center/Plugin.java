package org.jvnet.hudson.update_center;

import hudson.plugins.jira.soap.RemotePageSummary;

/**
 * @author Kohsuke Kawaguchi
 */
public class Plugin {
    /**
     * Plugin artifact ID.
     */
    public final String artifactId;
    /**
     * File in the download section.
     */
    public final VersionedFile file;
    /**
     * Confluence page of this plugin in Wiki.
     * Null if we couldn't find it.
     */
    public final RemotePageSummary page;

    public Plugin(String artifactId, VersionedFile file, ConfluencePluginList cpl) {
        this.artifactId = artifactId;
        this.file = file;

        page = cpl.findNearest(artifactId);
    }
}
