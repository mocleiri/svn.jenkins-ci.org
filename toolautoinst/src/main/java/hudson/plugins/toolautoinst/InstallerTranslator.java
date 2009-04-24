package hudson.plugins.toolautoinst;

import hudson.Extension;
import hudson.model.Node;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolLocationTranslator;

/**
 * Actually runs installations.
 */
@Extension
public class InstallerTranslator extends ToolLocationTranslator {

    public String getToolHome(Node node, ToolInstallation installation) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
