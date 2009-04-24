package hudson.plugins.toolautoinst;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Node;
import java.io.IOException;
import java.io.OutputStream;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Installs a tool into the Hudson working area by downloading and unpacking a ZIP file.
 */
public class ZipExtraction extends ToolInstaller {

    private final String url;
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

    @Override
    public FilePath ensureInstalled(Node node, OutputStream log) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Extension
    public static class DescriptorImpl extends ToolInstallerDescriptor<ZipExtraction> {

        public String getDisplayName() {
            return "Extract ZIP"; // XXX I18N
        }

    }

}
