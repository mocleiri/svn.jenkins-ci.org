package org.jvnet.hudson.update_center;

import hudson.plugins.jira.soap.ConfluenceSoapService;
import hudson.plugins.jira.soap.RemotePage;
import hudson.plugins.jira.soap.RemotePageSummary;
import org.jvnet.hudson.confluence.Confluence;

import javax.xml.rpc.ServiceException;
import java.net.URL;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import com.sun.xml.internal.bind.v2.util.EditDistance;

/**
 * List of plugins from confluence.
 *
 * @author Kohsuke Kawaguchi
 */
public class ConfluencePluginList {
    private final Map<String,RemotePageSummary> children = new HashMap<String, RemotePageSummary>();

    private final String[] normalizedTitles;

    public ConfluencePluginList() throws IOException, ServiceException {
        ConfluenceSoapService service = Confluence.connect(new URL("http://hudson.gotdns.com/wiki/"));
        RemotePage page = service.getPage("", "HUDSON", "Plugins");

        for (RemotePageSummary child : service.getChildren("", page.getId()))
            children.put(normalize(child.getTitle()),child);

        normalizedTitles = children.keySet().toArray(new String[children.size()]);
    }

    /**
     * Make the page title as close to artifactId as possible.
     */
    private String normalize(String title) {
        title = title.toLowerCase().trim();
        if(title.endsWith("plugin"))    title=title.substring(0,title.length()-6).trim();
        return title.replace(" ","-");
    }

    /**
     * Finds the closest match, if any. Otherwise null.
     */
    public RemotePageSummary findNearest(String pluginArtifactId) {
        // comparison is case insensitive
        pluginArtifactId = pluginArtifactId.toLowerCase();

        String nearest = EditDistance.findNearest(pluginArtifactId, normalizedTitles);
        if(EditDistance.editDistance(nearest,pluginArtifactId)<4)
            return children.get(nearest);
        else
            return null;    // too far
    }
}
