package org.jvnet.hudson.update_center;

import hudson.plugins.jira.soap.RemotePageSummary;

import java.net.URL;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.io.IOException;
import java.io.IOError;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import org.dom4j.io.SAXReader;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.Element;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

/**
 * A Hudson plugin.
 *
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

    public final Cache cache;

    public Plugin(String artifactId, VersionedFile file, ConfluencePluginList cpl, Cache cache) {
        this.artifactId = artifactId;
        this.file = file;
        this.page = findPage(cpl);
        this.cache = cache;
    }

    /**
     * Locates the page for this plugin on Wiki.
     *
     * <p>
     * First we'll try to parse POM and obtain the URL.
     * If that fails, find the nearest name from the children list.
     */
    private RemotePageSummary findPage(ConfluencePluginList cpl) {
        try {
            URL pom = new URL(
                MessageFormat.format("http://maven.dyndns.org/2/org/jvnet/hudson/plugins/{0}/{1}/{0}-{1}.pom",artifactId,file.version));
            Document dom = new SAXReader().read(pom);
            Node url = dom.selectSingleNode("/project/url");
            if(url!=null) {
                String wikiPage = ((Element)url).getTextTrim();
                return cpl.getPage(wikiPage); // found the confluence page successfully
            }
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        } catch (DocumentException e) {
            System.err.println("Can't parse POM for "+artifactId);
            e.printStackTrace();
        } catch (RemoteException e) {
            System.err.println("POM points to a non-confluence page for "+artifactId);
            e.printStackTrace();
        }

        try {
            String p = OVERRIDES.getProperty(artifactId);
            if(p!=null)
                return cpl.getPage(p);
        } catch (RemoteException e) {
            System.err.println("Override failed for "+artifactId);
            e.printStackTrace();
        }

        // try to guess the Wiki page
        return cpl.findNearest(artifactId);
    }

    public JSONObject toJSON() throws IOException {
        JSONObject json = file.toJSON(artifactId);
        if(page!=null) {
            json.put("wiki",page.getUrl());
            json.put("title",page.getTitle());
        }

        HpiFile hpi = new HpiFile(cache.obtain(this));
        json.put("requiredCore",hpi.getRequiredHudsonVersion());
        JSONArray deps = new JSONArray();
        for (HpiFile.Dependency d : hpi.getDependencies())
            deps.add(d.toJSON());
        json.put("dependencies",deps);

        return json;
    }


    private static final Properties OVERRIDES = new Properties();

    static {
        try {
            OVERRIDES.load(Plugin.class.getClassLoader().getResourceAsStream("wiki-overrides.properties");
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
