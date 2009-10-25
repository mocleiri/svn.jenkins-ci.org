/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Yahoo! Inc., Seiji Sogabe,
 *                          Andrew Bayer
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.model;

import hudson.ExtensionPoint;
import hudson.Functions;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.Util;
import hudson.ProxyConfiguration;
import hudson.Extension;
import hudson.lifecycle.Lifecycle;
import hudson.model.UpdateCenter.UpdateCenterConfiguration;
import hudson.model.UpdateCenter.Data;
import hudson.model.UpdateCenter.Entry;
import hudson.model.UpdateCenter.Plugin;
import hudson.util.DaemonThreadFactory;
import hudson.util.TextFile;
import hudson.util.VersionNumber;
import hudson.util.IOException2;
import static hudson.util.TimeUnit2.DAYS;
import net.sf.json.JSONObject;
import org.acegisecurity.Authentication;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;



public class UpdateSource {
    /**
     * What's the time stamp of data file?
     */
    private long dataTimestamp = -1;

    /**
     * When was the last time we asked a browser to check the data for us?
     *
     * <p>
     * There's normally some delay between when we send HTML that includes the check code,
     * until we get the data back, so this variable is used to avoid asking too many browseres
     * all at once.
     */
    private volatile long lastAttempt = -1;

    /**
     * Update center configuration data
     */
    private transient UpdateCenterConfiguration config;

    /**
     * ID string for this update source.
     */
    private final String updateId;

    private final String connectionCheckUrl;
    private final String updateCenterUrl;
    private final String pluginRepositoryBaseUrl;
    private final String dataFilePrefix;
    
    public UpdateSource() {
        this("default",
             "http://www.google.com",
             "http://andrewbayer.com/",
             "http://hudson-ci.org/",
             "");
    }

    public UpdateSource(final String updateId,
                        final String updateCenterUrl) {
        this(updateId,
             "http://www.google.com",
             updateCenterUrl,
             updateCenterUrl,
             "");
    }
    
    public UpdateSource(final String updateId,
                        final String connectionCheckUrl,
                        final String updateCenterUrl,
                        final String pluginRepositoryBaseUrl,
                        final String dataFilePrefix) {
        this.connectionCheckUrl = connectionCheckUrl;
        this.updateCenterUrl = updateCenterUrl;
        this.pluginRepositoryBaseUrl = pluginRepositoryBaseUrl;
        this.dataFilePrefix = dataFilePrefix;
        this.updateId = updateId;
        initialize();
    }
    
    public void initialize() {
        configure(new UpdateCenterConfiguration() {
                public String getDataFilePrefix() {
                    return dataFilePrefix;
                }

                public String getConnectionCheckUrl() {
                    return connectionCheckUrl;
                }

                public String getUpdateCenterUrl() {
                    return updateCenterUrl;
                }

                public String getPluginRepositoryBaseUrl() {
                    return pluginRepositoryBaseUrl;
                }
            });
    }

    /**
     * Get ID string.
     */
    public String getId() {
        return updateId;
    }

    public long getDataTimestamp() {
        return dataTimestamp;
    }

    /**
     * Gets the {@link UpdateCenterConfiguration} for this source.
     */
    public UpdateCenterConfiguration getConfiguration() {
        return config;
    }

    /**
     * Configures update center to get plugins/updates from alternate servers,
     * and optionally using alternate strategies for downloading, installing
     * and upgrading.
     * 
     * @param config Configuration data
     * @see UpdateCenterConfiguration
     */
    public void configure(UpdateCenterConfiguration config) {
        if (config!=null) {
            this.config = config;
        }
    }

    /**
     * This is the endpoint that receives the update center data file from the browser.
     */
    public void doPostBack(@QueryParameter String json) throws IOException {
        dataTimestamp = System.currentTimeMillis();
        JSONObject o = JSONObject.fromObject(json);

        int v = o.getInt("updateCenterVersion");
        if(v !=1) {
            LOGGER.warning("Unrecognized update center version: "+v);
            return;
        }

        LOGGER.info("Obtained the latest update center data file");
        getDataFile().write(json);
    }


    /**
     * Returns true if it's time for us to check for new version.
     */
    public boolean isDue() {
        if(neverUpdate)     return false;
        if(dataTimestamp==-1)
            dataTimestamp = getDataFile().file.lastModified();
        long now = System.currentTimeMillis();
        boolean due = now - dataTimestamp > DAY && now - lastAttempt > 15000;
        if(due)     lastAttempt = now;
        return due;
    }

    /**
     * Loads the update center data, if any.
     *
     * @return  null if no data is available.
     */
    public Data getData() {
        TextFile df = getDataFile();
        if(df.exists()) {
            try {
                return Hudson.getInstance().getUpdateCenter().new Data(updateId, JSONObject.fromObject(df.read()));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE,"Failed to parse "+df,e);
                df.delete(); // if we keep this file, it will cause repeated failures
                return null;
            }
        } else {
            return null;
        }
    }
    
    /**
     * Returns a list of plugins that should be shown in the "available" tab.
     * These are "all plugins - installed plugins".
     */
    public List<Plugin> getAvailables() {
        List<Plugin> r = new ArrayList<Plugin>();
        Data data = getData();
        if(data ==null)     return Collections.emptyList();
        for (Plugin p : data.plugins.values()) {
            if(p.getInstalled()==null)
                r.add(p);
        }
        return r;
    }
    
    /**
     * Gets the information about a specific plugin.
     *
     * @param artifactId
     *      The short name of the plugin. Corresponds to {@link PluginWrapper#getShortName()}.
     *
     * @return
     *      null if no such information is found.
     */
    public Plugin getPlugin(String artifactId) {
        Data dt = getData();
        if(dt==null)    return null;
        return dt.plugins.get(artifactId);
    }
    
    /**
     * This is where we store the update center data.
     */
    private TextFile getDataFile() {
        return new TextFile(new File(Hudson.getInstance().getRootDir(),
                                     "updates/" + config.getDataFileName()));
    }
    
    /**
     * Returns the list of plugins that are updates to currently installed ones.
     *
     * @return
     *      can be empty but never null.
     */
    public List<Plugin> getUpdates() {
        Data data = getData();
        if(data==null)      return Collections.emptyList(); // fail to determine
        
        List<Plugin> r = new ArrayList<Plugin>();
        for (PluginWrapper pw : Hudson.getInstance().getPluginManager().getPlugins()) {
            Plugin p = pw.getUpdateInfo();
            if(p!=null) r.add(p);
        }
        
        return r;
    }
    
    /**
     * Does any of the plugin has updates?
     */
    public boolean hasUpdates() {
        Data data = getData();
        if(data==null)      return false;
        
        for (PluginWrapper pw : Hudson.getInstance().getPluginManager().getPlugins()) {
            if(!pw.isBundled() && pw.getUpdateInfo()!=null)
                // do not advertize updates to bundled plugins, since we generally want users to get them
                // as a part of hudson.war updates. This also avoids unnecessary pinning of plugins. 
                return true;
        }
        return false;
    }
    
    
    /**
     * Exposed to get rid of hardcoding of the URL that serves up update-center.json
     * in Javascript.
     */
    public String getUrl() {
        return config.getUpdateCenterUrl() + config.getDataFileName();
    }
    
    /**
     * Exposed to get rid of hardcoding of the URL that serves up update-center.json
     * in Javascript.
     */
    public String getUpdateFileName() {
        return config.getDataFileName();
    }

    private static final long DAY = DAYS.toMillis(1);

    private static final Logger LOGGER = Logger.getLogger(UpdateSource.class.getName());

    public static boolean neverUpdate = Boolean.getBoolean(UpdateCenter.class.getName()+".never");

}
