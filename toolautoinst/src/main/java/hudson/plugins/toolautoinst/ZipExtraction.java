package hudson.plugins.toolautoinst;

import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Util;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Installs a tool into the Hudson working area by downloading and unpacking a ZIP file.
 */
public class ZipExtraction extends ToolInstaller {

    /**
     * URL of a ZIP file which should be downloaded in case the tool is missing.
     */
    private final String url;
    /**
     * Optional subdir to extract.
     */
    private final String subdir;

    @DataBoundConstructor
    public ZipExtraction(String toolName, String label, String url, String subdir) {
        super(toolName, label);
        this.url = url;
        this.subdir = Util.fixEmptyAndTrim(subdir);
    }

    public String getUrl() {
        return url;
    }

    public String getSubdir() {
        return subdir;
    }

    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        String dirname = tool.getName().replaceAll("[^A-Za-z0-9_.-]+", "_");
        FilePath dir = node.getRootPath().child("tools").child(dirname);
        if (dir.installIfNecessaryFrom(new URL(url), log, "Unpacking " + url + " to " + dir + " on " + node.getDisplayName())) {
            dir.act(new ChmodRecAPlusX());
        }
        if (subdir == null) {
            return dir;
        } else {
            return dir.child(subdir);
        }
    }

    @Extension
    public static class DescriptorImpl extends ToolInstallerDescriptor<ZipExtraction> {

        public String getDisplayName() {
            return "Extract *.zip/*.tar.gz"; // XXX I18N
        }

        public FormValidation doCheckUrl(@QueryParameter String value) {
            try {
                URLConnection conn = new URL(value).openConnection();
                conn.connect();
                if (conn instanceof HttpURLConnection) {
                    if (((HttpURLConnection) conn).getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return FormValidation.error("Server rejected connection."); // XXX I18N
                    }
                }
                return FormValidation.ok();
            } catch (MalformedURLException x) {
                return FormValidation.error("Malformed URL."); // XXX I18N
            } catch (IOException x) {
                return FormValidation.error("Could not connect to URL."); // XXX I18N
            }
        }

    }

    /**
     * Sets execute permission on all files, since unzip etc. might not do this.
     * Hackish, is there a better way?
     */
    private static class ChmodRecAPlusX implements FileCallable<Void> {
        private static final long serialVersionUID = 1L;
        public Void invoke(File d, VirtualChannel channel) throws IOException {
            process(d);
            return null;
        }
        private void process(File f) {
            if (f.isFile()) {
                f.setExecutable(true, false); // XXX JDK 6-specific
            } else {
                File[] kids = f.listFiles();
                if (kids != null) {
                    for (File kid : kids) {
                        process(kid);
                    }
                }
            }
        }
    }

}
