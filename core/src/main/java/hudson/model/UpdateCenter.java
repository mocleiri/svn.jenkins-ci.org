/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Yahoo! Inc., Seiji Sogabe
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

import hudson.BulkChange;
import hudson.ExtensionPoint;
import hudson.Functions;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.Util;
import hudson.ProxyConfiguration;
import hudson.Extension;
import hudson.XmlFile;
import hudson.lifecycle.Lifecycle;
import hudson.util.DaemonThreadFactory;
import hudson.util.TextFile;
import hudson.util.VersionNumber;
import hudson.util.IOException2;
import net.sf.json.JSONObject;
import org.acegisecurity.Authentication;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

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

/**
 * Controls update center capability.
 *
 * <p>
 * The main job of this class is to keep track of the latest update center metadata file, and perform installations.
 * Much of the UI about choosing plugins to install is done in {@link PluginManager}.
 * <p>
 * The update center can be configured to contact alternate servers for updates
 * and plugins, and to use alternate strategies for downloading, installing
 * and updating components. See the Javadocs for {@link UpdateCenterConfiguration}
 * for more information.
 * 
 * @author Kohsuke Kawaguchi
 * @since 1.220
 */
public class UpdateCenter extends AbstractModelObject implements Saveable {
    /**
     * What's the time stamp of data file?
     */
    private long dataTimestamp = -1;

    /**
     * {@link ExecutorService} that performs installation.
     */
    private final ExecutorService installerService = Executors.newSingleThreadExecutor(
        new DaemonThreadFactory(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("Update center installer thread");
                return t;
            }
        }));

    /**
     * List of created {@link UpdateCenterJob}s. Access needs to be synchronized.
     */
    private final Vector<UpdateCenterJob> jobs = new Vector<UpdateCenterJob>();

    /**
     * List of {@link UpdateSource}s to be used.
     */
    private List<UpdateSource> sources = new ArrayList<UpdateSource>();
    /**
     * Create update center to get plugins/updates from hudson.dev.java.net
     */

    public UpdateCenter(Hudson parent) {
    }

    public void initialize() {
        load();
        // If there aren't already any UpdateSources, add the default one.
        if (sources.size() == 0) {
            sources.add(new UpdateSource());
            save();
        }
    }
    
    /**
     * Returns the list of {@link UpdateCenterJob} representing scheduled installation attempts.
     *
     * @return
     *      can be empty but never null. Oldest entries first.
     */
    public List<UpdateCenterJob> getJobs() {
        synchronized (jobs) {
            return new ArrayList<UpdateCenterJob>(jobs);
        }
    }

    /**
     * Returns the list of {@link UpdateSource}s to be used.
     *
     * @return
     *      can be empty but never null.
     */
    public List<UpdateSource> getSources() {
        return sources;
    }

    public void replaceSources(List<UpdateSource> newSources) {
        sources.clear();
        sources.addAll(newSources);
    }
    
    /**
     * Gets the string representing how long ago the data was obtained.
     * Will be the newest of all {@link UpdateSource}s.
     */
    public String getLastUpdatedString() {
        long newestTs = -1;
        for (UpdateSource s : sources) {
            if (s.getDataTimestamp()>newestTs) {
                newestTs = s.getDataTimestamp();
            }
        }
        if(newestTs<0)     return "N/A";
        return Util.getPastTimeString(System.currentTimeMillis()-newestTs);
    }

    /**
     * Gets {@link UpdateSource} by its ID.
     * Used to bind them to URL.
     */
    public UpdateSource getById(String id) {
        for (UpdateSource s : sources) {
            if (s.getId().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Gets the default {@link UpdateSource}.
     */
    public UpdateSource getDefaultSource() {
        return getById("default");
    }

    /**
     * Gets the default base URL.
     */
    public String getDefaultBaseUrl() {
        return getDefaultSource().getConfiguration().getUpdateCenterUrl();
    }

    /**
     * Gets the plugin with the given name from the first {@link UpdateSource} to contain it.
     */
    public Plugin getPlugin(String artifactId) {
        for (UpdateSource s : sources) {
            Plugin p = s.getPlugin(artifactId);
            if (p!=null) return p;
        }
        return null;
    }
     
    /**
     * Schedules a Hudson upgrade.
     */
    public void doUpgrade(StaplerResponse rsp) throws IOException, ServletException {
        requirePOST();
        Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
        HudsonUpgradeJob job = new HudsonUpgradeJob(getDefaultSource(), Hudson.getAuthentication());
        if(!Lifecycle.get().canRewriteHudsonWar()) {
            sendError("Hudson upgrade not supported in this running mode");
            return;
        }

        LOGGER.info("Scheduling the core upgrade");
        addJob(job);
        rsp.sendRedirect2(".");
    }

    private Future<UpdateCenterJob> addJob(UpdateCenterJob job) {
        // the first job is always the connectivity check
        if(jobs.size()==0)
            new ConnectionCheckJob().submit();
        return job.submit();
    }

    public String getDisplayName() {
        return "Update center";
    }
    
    public String getSearchUrl() {
        return "updateCenter";
    }

    /**
     * Saves the configuration info to the disk.
     */
    public synchronized void save() {
        if(BulkChange.contains(this))   return;
        try {
            getConfigFile().write(sources);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save "+getConfigFile(),e);
        }
    }

    /**
     * Loads the data from the disk into this object.
     *
     * <p>
     * The constructor of the derived class must call this method.
     * (If we do that in the base class, the derived class won't
     * get a chance to set default values.)
     */
    public synchronized void load() {
        XmlFile file = getConfigFile();
        if(file==null||!file.exists())
            return;
        try {
            sources = (List)file.unmarshal(sources);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load "+file, e);
        }
        for (UpdateSource s : sources) {
            s.initialize();
        }
    }
    
    private XmlFile getConfigFile() {
        return new XmlFile(new File(Hudson.getInstance().root,
                                    UpdateCenter.class.getName()+".xml"));
    }

    public List<Plugin> getAvailables() {
        List<Plugin> plugins = new ArrayList<Plugin>();

        for (UpdateSource s : sources) {
            plugins.addAll(s.getAvailables());
        }

        return plugins;
    }

    public List<Plugin> getUpdates() {
        List<Plugin> plugins = new ArrayList<Plugin>();

        for (UpdateSource s : sources) {
            plugins.addAll(s.getUpdates());
        }

        return plugins;
    }

    
    /**
     * {@link AdministrativeMonitor} that checks if there's Hudson update.
     */
    @Extension
    public static final class CoreUpdateMonitor extends AdministrativeMonitor {
        public boolean isActivated() {
            Data data = getData();
            return data!=null && data.hasCoreUpdates();
        }

        public Data getData() {
            return Hudson.getInstance().getUpdateCenter().getDefaultSource().getData();
        }
    }

    /**
     * In-memory representation of the update center data.
     */
    public final class Data {
        /**
         * The {@link UpdateSource} ID.
         */
        public final String sourceId;
        
        /**
         * The latest hudson.war.
         */
        public final Entry core;
        /**
         * Plugins in the official repository, keyed by their artifact IDs.
         */
        public final Map<String,Plugin> plugins = new TreeMap<String,Plugin>(String.CASE_INSENSITIVE_ORDER);
        
        Data(String sourceId, JSONObject o) {
            this.sourceId = sourceId;
            if (sourceId.equals("default")) {
                core = new Entry(sourceId, o.getJSONObject("core"));
            }
            else {
                core = null;
            }
            for(Map.Entry<String,JSONObject> e : (Set<Map.Entry<String,JSONObject>>)o.getJSONObject("plugins").entrySet()) {
                plugins.put(e.getKey(),new Plugin(sourceId, e.getValue()));
            }
        }

        /**
         * Is there a new version of the core?
         */
        public boolean hasCoreUpdates() {
            if (core!=null) {
                return core.isNewerThan(Hudson.VERSION);
            }
            else {
                return false;
            }
        }

        /**
         * Do we support upgrade?
         */
        public boolean canUpgrade() {
            return Lifecycle.get().canRewriteHudsonWar();
        }
    }

    public static class Entry {
        /**
         * {@link UpdateSource} ID.
         */
        public final String sourceId;
        
        /**
         * Artifact ID.
         */
        public final String name;
        /**
         * The version.
         */
        public final String version;
        /**
         * Download URL.
         */
        public final String url;

        public Entry(String sourceId, JSONObject o) {
            this.sourceId = sourceId;
            this.name = o.getString("name");
            this.version = o.getString("version");
            this.url = o.getString("url");
        }

        /**
         * Checks if the specified "current version" is older than the version of this entry.
         *
         * @param currentVersion
         *      The string that represents the version number to be compared.
         * @return
         *      true if the version listed in this entry is newer.
         *      false otherwise, including the situation where the strings couldn't be parsed as version numbers.
         */
        public boolean isNewerThan(String currentVersion) {
	    return isNewerThan(currentVersion, version);
	}

	/**
	 * Compares two versions - returns true if the first version is newer than the second.
	 *
	 * @param firstVersion
	 *      The first version to test against.
	 * @param secondVersion
	 *      The second version to test against.
	 * @return
	 *      True if the first version is newer than the second version. False in all other cases.
	 */
	public boolean isNewerThan(String firstVersion, String secondVersion) {
            try {
                return new VersionNumber(firstVersion).compareTo(new VersionNumber(secondVersion)) < 0;
            } catch (IllegalArgumentException e) {
                // couldn't parse as the version number.
                return false;
            }
        }
    }

    public final class Plugin extends Entry {
        /**
         * Optional URL to the Wiki page that discusses this plugin.
         */
        public final String wiki;
        /**
         * Human readable title of the plugin, taken from Wiki page.
         * Can be null.
         *
         * <p>
         * beware of XSS vulnerability since this data comes from Wiki 
         */
        public final String title;
        /**
         * Optional excerpt string.
         */
        public final String excerpt;
        /**
         * Optional version # from which this plugin release is configuration-compatible.
         */
        public final String compatibleSinceVersion;
	
        @DataBoundConstructor
        public Plugin(String sourceId, JSONObject o) {
            super(sourceId, o);
            this.wiki = get(o,"wiki");
            this.title = get(o,"title");
            this.excerpt = get(o,"excerpt");
            this.compatibleSinceVersion = get(o,"compatibleSinceVersion");
        }

        private String get(JSONObject o, String prop) {
            if(o.has(prop))
                return o.getString(prop);
            else
                return null;
        }

        public String getDisplayName() {
            if(title!=null) return title;
            return name;
        }

        /**
         * If some version of this plugin is currently installed, return {@link PluginWrapper}.
         * Otherwise null.
         */
        public PluginWrapper getInstalled() {
            PluginManager pm = Hudson.getInstance().getPluginManager();
            return pm.getPlugin(name);
        }

        /**
         * If the plugin is already installed, and the new version of the plugin has a "compatibleSinceVersion"
         * value (i.e., it's only directly compatible with that version or later), this will check to
         * see if the installed version is older than the compatible-since version. If it is older, it'll return false.
         * If it's not older, or it's not installed, or it's installed but there's no compatibleSinceVersion
         * specified, it'll return true.
         */
        public boolean isCompatibleWithInstalledVersion() {
            PluginWrapper installedVersion = getInstalled();
            if (installedVersion != null) {
                if (compatibleSinceVersion != null) {
                    if (new VersionNumber(installedVersion.getVersion())
                            .isOlderThan(new VersionNumber(compatibleSinceVersion))) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * @deprecated as of 1.326
         *      Use {@link #deploy()}. 
         */
        public void install() {
            deploy();
        }

        /**
         * Schedules the installation of this plugin.
         *
         * <p>
         * This is mainly intended to be called from the UI. The actual installation work happens
         * asynchronously in another thread.
         */
        public Future<UpdateCenterJob> deploy() {
            Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
            return addJob(new InstallationJob(this, getById(sourceId), Hudson.getAuthentication()));
        }

        /**
         * Making the installation web bound.
         */
        public void doInstall(StaplerResponse rsp) throws IOException {
            install();
            rsp.sendRedirect2("../..");
        }
    }

    /**
     * Configuration data for controlling the update center's behaviors. The update
     * center's defaults will check internet connectivity by trying to connect
     * to www.google.com; will download plugins, the plugin catalog and updates
     * from hudson-ci.org; and will install plugins with file system
     * operations.
     * 
     * @since 1.266
     */
    public static abstract class UpdateCenterConfiguration implements ExtensionPoint {
        /**
         * Creates default update center configuration - uses settings for global update center.
         */
        public UpdateCenterConfiguration() {
        }
        
        /**
         * Check network connectivity by trying to establish a connection to
         * the host in connectionCheckUrl.
         * 
         * @param job The connection checker that is invoking this strategy.
         * @param connectionCheckUrl A string containing the URL of a domain
         *          that is assumed to be always available.
         * @throws IOException if a connection can't be established
         */
        public void checkConnection(ConnectionCheckJob job, String connectionCheckUrl) throws IOException {
            testConnection(new URL(connectionCheckUrl));
        }
        
        /**
         * Check connection to update center server.
         * 
         * @param job The connection checker that is invoking this strategy.
         * @param updateCenterUrl A sting containing the URL of the update center host.
         * @throws IOException if a connection to the update center server can't be established.
         */
        public void checkUpdateCenter(ConnectionCheckJob job, String updateCenterUrl) throws IOException {
            testConnection(new URL(updateCenterUrl + "?uctest"));
        }
        
        /**
         * Validate the URL of the resource before downloading it. The default
         * implementation enforces that the base of the resource URL starts
         * with the string returned by {@link #getPluginRepositoryBaseUrl()}.
         * 
         * @param job The download job that is invoking this strategy. This job is
         *          responsible for managing the status of the download and installation.
         * @param src The location of the resource on the network
         * @throws IOException if the validation fails
         */
        public void preValidate(DownloadJob job, URL src) throws IOException {
            // In the future if we are to open up update center to 3rd party, we need more elaborate scheme
            // like signing to ensure the safety of the bits.
            if(!src.toExternalForm().startsWith(getPluginRepositoryBaseUrl())) {
                throw new IOException("Installation of plugin from "+src+" is not allowed");
            }                    
        }
        
        /**
         * Validate the resource after it has been downloaded, before it is
         * installed. The default implementation does nothing.
         * 
         * @param job The download job that is invoking this strategy. This job is
         *          responsible for managing the status of the download and installation.
         * @param src The location of the downloaded resource.
         * @throws IOException if the validation fails.
         */
        public void postValidate(DownloadJob job, File src) throws IOException {
        }
        
        /**
         * Download a plugin or core upgrade in preparation for installing it
         * into its final location. Implementations will normally download the
         * resource into a temporary location and hand off a reference to this
         * location to the install or upgrade strategy to move into the final location.
         * 
         * @param job The download job that is invoking this strategy. This job is
         *          responsible for managing the status of the download and installation.
         * @param src The URL to the resource to be downloaded.
         * @return A File object that describes the downloaded resource.
         * @throws IOException if there were problems downloading the resource.
         * @see DownloadJob
         */
        public File download(DownloadJob job, URL src) throws IOException {
            URLConnection con = ProxyConfiguration.open(src);
            int total = con.getContentLength();
            CountingInputStream in = new CountingInputStream(con.getInputStream());
            byte[] buf = new byte[8192];
            int len;

            File dst = job.getDestination();
            File tmp = new File(dst.getPath()+".tmp");
            OutputStream out = new FileOutputStream(tmp);

            LOGGER.info("Downloading "+job.getName());
            try {
                while((len=in.read(buf))>=0) {
                    out.write(buf,0,len);
                    job.status = job.new Installing(total==-1 ? -1 : in.getCount()*100/total);
                }
            } catch (IOException e) {
                throw new IOException2("Failed to load "+src+" to "+tmp,e);
            }

            in.close();
            out.close();

            if (total!=-1 && total!=tmp.length()) {
                // don't know exactly how this happens, but report like
                // http://www.ashlux.com/wordpress/2009/08/14/hudson-and-the-sonar-plugin-fail-maveninstallation-nosuchmethoderror/
                // indicates that this kind of inconsistency can happen. So let's be defensive
                throw new IOException("Inconsistent file length: expected "+total+" but only got "+tmp.length());
            }
            
            return tmp;
        }
        
        /**
         * Called after a plugin has been downloaded to move it into its final
         * location. The default implementation is a file rename.
         * 
         * @param job The install job that is invoking this strategy.
         * @param src The temporary location of the plugin.
         * @param dst The final destination to install the plugin to.
         * @throws IOException if there are problems installing the resource.
         */
        public void install(DownloadJob job, File src, File dst) throws IOException {
            job.replace(dst, src);
        }
        
        /**
         * Called after an upgrade has been downloaded to move it into its final
         * location. The default implementation is a file rename.
         * 
         * @param job The upgrade job that is invoking this strategy.
         * @param src The temporary location of the upgrade.
         * @param dst The final destination to install the upgrade to.
         * @throws IOException if there are problems installing the resource.
         */
        public void upgrade(DownloadJob job, File src, File dst) throws IOException {
            job.replace(dst, src);
        }

        /**
         * Returns the prefix to prepend to "update-center.json" for the local datafile.
         */
        public abstract String getDataFilePrefix();

        /**
         * Returns the filename for the data file, including optional prefix if specified.
         */
        public String getDataFileName() {
            if ((getDataFilePrefix()!=null) && (getDataFilePrefix().length() > 0)) {
                return getDataFilePrefix() + "-update-center.json";
            }
            else {
                return "update-center.json";
            }
        }

        /**
         * Returns an "always up" server for Internet connectivity testing
         */
        public abstract String getConnectionCheckUrl();
        
        /**
         * Returns the URL of the server that hosts the update-center.json
         * file.
         *
         * @return
         *      Absolute URL that ends with '/'.
         */
        public abstract String getUpdateCenterUrl();
        
        /**
         * Returns the URL of the server that hosts plugins and core updates.
         */
        public abstract String getPluginRepositoryBaseUrl();
        
        private void testConnection(URL url) throws IOException {
            try {
                InputStream in = ProxyConfiguration.open(url).getInputStream();
                IOUtils.copy(in,new NullOutputStream());
                in.close();
            } catch (SSLHandshakeException e) {
                if (e.getMessage().contains("PKIX path building failed"))
                   // fix up this crappy error message from JDK
                    throw new IOException2("Failed to validate the SSL certificate of "+url,e);
            }
        }                    
    }
    
    /**
     * Things that {@link UpdateCenter#installerService} executes.
     *
     * This object will have the <tt>row.jelly</tt> which renders the job on UI.
     */
    public abstract class UpdateCenterJob implements Runnable {
        /**
         * @deprecated as of 1.326
         *      Use {@link #submit()} instead.
         */
        public void schedule() {
            submit();
        }

        /**
         * Schedules this job for an execution
         * @return
         *      {@link Future} to keeps track of the status of the execution.
         */
        public Future<UpdateCenterJob> submit() {
            LOGGER.fine("Scheduling "+this+" to installerService");
            jobs.add(this);
            return installerService.submit(this,this);
        }
    }

    /**
     * Tests the internet connectivity.
     */
    public final class ConnectionCheckJob extends UpdateCenterJob {
        private final Vector<String> statuses= new Vector<String>();

        public void run() {
            UpdateCenterConfiguration config = getDefaultSource().getConfiguration();
            LOGGER.fine("Doing a connectivity check");
            try {
                String connectionCheckUrl = config.getConnectionCheckUrl();
                
                statuses.add(Messages.UpdateCenter_Status_CheckingInternet());
                try {
                    config.checkConnection(this, connectionCheckUrl);
                } catch (IOException e) {
                    if(e.getMessage().contains("Connection timed out")) {
                        // Google can't be down, so this is probably a proxy issue
                        statuses.add(Messages.UpdateCenter_Status_ConnectionFailed(connectionCheckUrl));
                        return;
                    }
                }

                statuses.add(Messages.UpdateCenter_Status_CheckingJavaNet());
                config.checkUpdateCenter(this, config.getUpdateCenterUrl());

                statuses.add(Messages.UpdateCenter_Status_Success());
            } catch (UnknownHostException e) {
                statuses.add(Messages.UpdateCenter_Status_UnknownHostException(e.getMessage()));
                addStatus(e);
            } catch (IOException e) {
                statuses.add(Functions.printThrowable(e));
            }
        }

        private void addStatus(UnknownHostException e) {
            statuses.add("<pre>"+ Functions.xmlEscape(Functions.printThrowable(e))+"</pre>");
        }

        public String[] getStatuses() {
            synchronized (statuses) {
                return statuses.toArray(new String[statuses.size()]);
            }
        }
    }

    /**
     * Base class for a job that downloads a file from the Hudson project.
     */
    public abstract class DownloadJob extends UpdateCenterJob {
        /**
         * Unique ID that identifies this job.
         */
        public final int id = iota.incrementAndGet();
        /**
         * Immutable object representing the current state of this job.
         */
        public volatile InstallationStatus status = new Pending();

        /**
         * Where to download the file from.
         */
        protected abstract URL getURL() throws MalformedURLException;

        /**
         * Where to download the file to.
         */
        protected abstract File getDestination();

        public abstract String getName();

        /**
         * Called when the whole thing went successfully.
         */
        protected abstract void onSuccess();

        
        private Authentication authentication;

        private UpdateSource source;

        /**
         * Get the {@link UpdateSource} for this job.
         */
        public UpdateSource getSource() {
            return this.source;
        }
        
        /**
         * Get the user that initiated this job
         */
        public Authentication getUser()
        {
            return this.authentication;
        }
        
        protected DownloadJob(UpdateSource source, Authentication authentication)
        {
            this.source = source;;
            this.authentication = authentication;
        }
        
        public void run() {
            try {
                LOGGER.info("Starting the installation of "+getName()+" on behalf of "+getUser().getName());

                _run();
                
                LOGGER.info("Installation successful: "+getName());
                status = new Success();
                onSuccess();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to install "+getName(),e);
                status = new Failure(e);
            }
        }

        protected void _run() throws IOException {
            URL src = getURL();

            source.getConfiguration().preValidate(this, src);

            File dst = getDestination();
            File tmp = source.getConfiguration().download(this, src);

            source.getConfiguration().postValidate(this, tmp);
            source.getConfiguration().install(this, tmp, dst);
        }

        /**
         * Called when the download is completed to overwrite
         * the old file with the new file.
         */
        protected void replace(File dst, File src) throws IOException {
            File bak = Util.changeExtension(dst,".bak");
            bak.delete();
            dst.renameTo(bak);
            dst.delete(); // any failure up to here is no big deal
            if(!src.renameTo(dst)) {
                throw new IOException("Failed to rename "+src+" to "+dst);
            }
        }

        /**
         * Indicates the status or the result of a plugin installation.
         * <p>
         * Instances of this class is immutable.
         */
        public abstract class InstallationStatus {
            public final int id = iota.incrementAndGet();
        }

        /**
         * Indicates that the installation of a plugin failed.
         */
        public class Failure extends InstallationStatus {
            public final Throwable problem;

            public Failure(Throwable problem) {
                this.problem = problem;
            }

            public String getStackTrace() {
                return Functions.printThrowable(problem);
            }
        }

        /**
         * Indicates that the plugin was successfully installed.
         */
        public class Success extends InstallationStatus {
        }

        /**
         * Indicates that the plugin is waiting for its turn for installation.
         */
        public class Pending extends InstallationStatus {
        }

        /**
         * Installation of a plugin is in progress.
         */
        public class Installing extends InstallationStatus {
            /**
             * % completed download, or -1 if the percentage is not known.
             */
            public final int percentage;

            public Installing(int percentage) {
                this.percentage = percentage;
            }
        }
    }

    /**
     * Represents the state of the installation activity of one plugin.
     */
    public final class InstallationJob extends DownloadJob {
        /**
         * What plugin are we trying to install?
         */
        public final Plugin plugin;

        private final PluginManager pm = Hudson.getInstance().getPluginManager();

        public InstallationJob(Plugin plugin, UpdateSource source, Authentication auth) {
            super(source, auth);
            this.plugin = plugin;
        }

        protected URL getURL() throws MalformedURLException {
            return new URL(plugin.url);
        }

        protected File getDestination() {
            File baseDir = pm.rootDir;
            return new File(baseDir, plugin.name + ".hpi");
        }

        public String getName() {
            return plugin.getDisplayName();
        }

        @Override
        public void _run() throws IOException {
            super._run();

            // if this is a bundled plugin, make sure it won't get overwritten
            PluginWrapper pw = plugin.getInstalled();
            if (pw!=null && pw.isBundled())
                pw.doPin();
        }

        protected void onSuccess() {
            pm.pluginUploaded = true;
        }

        @Override
        public String toString() {
            return super.toString()+"[plugin="+plugin.title+"]";
        }
    }

    /**
     * Represents the state of the upgrade activity of Hudson core.
     */
    public final class HudsonUpgradeJob extends DownloadJob {
        public HudsonUpgradeJob(UpdateSource source, Authentication auth) {
            super(source, auth);
        }

        protected URL getURL() throws MalformedURLException {
            return new URL(getSource().getData().core.url);
        }

        protected File getDestination() {
            return Lifecycle.get().getHudsonWar();
        }

        public String getName() {
            return "hudson.war";
        }

        protected void onSuccess() {
            status = new Success();
        }

        @Override
        protected void replace(File dst, File src) throws IOException {
            Lifecycle.get().rewriteHudsonWar(src);
        }
    }

    /**
     * Adds the update center data retriever to HTML.
     */
    @Extension
    public static class PageDecoratorImpl extends PageDecorator {
        public PageDecoratorImpl() {
            super(PageDecoratorImpl.class);
        }
    }

    /**
     * Sequence number generator.
     */
    private static final AtomicInteger iota = new AtomicInteger();

    private static final Logger LOGGER = Logger.getLogger(UpdateCenter.class.getName());

    public static boolean neverUpdate = Boolean.getBoolean(UpdateCenter.class.getName()+".never");
}
