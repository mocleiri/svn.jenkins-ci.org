package hudson.plugins.toolautoinst;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolLocationTranslator;
import hudson.util.StreamTaskListener;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Actually runs installations.
 */
@Extension
public class InstallerTranslator extends ToolLocationTranslator {

    public String getToolHome(Node node, ToolInstallation tool) {
        for (ToolInstaller installer : Hudson.getInstance().getPlugin(PluginImpl.class).installers) {
            if (installer.appliesTo(tool) && installer.appliesTo(node)) {
                StringWriter w = new StringWriter();
                StreamTaskListener log = new StreamTaskListener(w);
                try {
                    FilePath result = installer.performInstallation(tool, node, log);
                    log.close();
                    // XXX would be better to log lines of text in real time
                    String logText = w.toString().replaceFirst("\r?\n$", "");
                    if (logText.length() > 0) {
                        // XXX cannot send to project's build log from here
                        Logger.getLogger(InstallerTranslator.class.getName()).log(Level.INFO, logText);
                    }
                    return result.getRemote();
                } catch (Exception x) {
                    log.close();
                    Logger.getLogger(InstallerTranslator.class.getName()).log(Level.WARNING, w.toString(), x);
                    break;
                }
            }
        }
        return null;
    }

}
