package org.jvnet.hudson.update_center;

import hudson.plugins.jira.soap.RemotePage;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.DocumentFactory;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public final RemotePage page;

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
    private RemotePage findPage(ConfluencePluginList cpl) {
        try {

            DocumentFactory factory = new DocumentFactory();
            factory.setXPathNamespaceURIs(Collections.singletonMap("m","http://maven.apache.org/POM/4.0.0"));

            URL pom = new URL(
                MessageFormat.format("http://maven.dyndns.org/2/org/jvnet/hudson/plugins/{0}/{1}/{0}-{1}.pom",artifactId,file.version));
            Document dom = new SAXReader(factory).read(pom);
            Node url = dom.selectSingleNode("/project/url");
            if(url==null)
                url = dom.selectSingleNode("/m:project/m:url");
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
        try {
            return cpl.findNearest(artifactId);
        } catch (RemoteException e) {
            System.err.println("Failed to locate nearest");
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Obtains the excerpt of this wiki page in HTML. Otherwise null.
     */
    public String getExcerptInHTML() {
        String content = page.getContent();
        if(content==null)
            return null;

        Matcher m = EXCERPT_PATTERN.matcher(content);
        if(!m.find())
            return null;

        String excerpt = m.group(1);
        return HYPERLINK_PATTERN.matcher(excerpt).replaceAll("<a href='$2'>$1</a>");
    }

    private static final Pattern EXCERPT_PATTERN = Pattern.compile("\\{excerpt(?::hidden)?\\}(.+)\\{excerpt\\}");
    private static final Pattern HYPERLINK_PATTERN = Pattern.compile("\\[([^|\\]]+)\\|([^|\\]]+)(|([^]])+)?\\]");

    public JSONObject toJSON() throws IOException {
        JSONObject json = file.toJSON(artifactId);
        if(page!=null) {
            json.put("wiki",page.getUrl());
            json.put("title",page.getTitle());
            String excerpt = getExcerptInHTML();
            if(excerpt!=null)
                json.put("excerpt",excerpt);
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
            OVERRIDES.load(Plugin.class.getClassLoader().getResourceAsStream("wiki-overrides.properties"));
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}
