package hudson.plugins.toolautoinst;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolLocationTranslator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Actually runs installations.
 */
@Extension
public class InstallerTranslator extends ToolLocationTranslator {

    private static final Logger LOG = Logger.getLogger(InstallerTranslator.class.getName());
    private static final Map<Node,Map<ToolInstallation,Semaphore>> mutexByNode = new WeakHashMap<Node,Map<ToolInstallation,Semaphore>>();

    public String getToolHome(Node node, ToolInstallation tool) {
        for (ToolInstaller installer : Hudson.getInstance().getPlugin(PluginImpl.class).installers) {
            if (installer.appliesTo(tool) && installer.appliesTo(node)) {
                Map<ToolInstallation, Semaphore> mutexByTool = mutexByNode.get(node);
                if (mutexByTool == null) {
                    mutexByNode.put(node, mutexByTool = new WeakHashMap<ToolInstallation, Semaphore>());
                }
                Semaphore semaphore = mutexByTool.get(tool);
                if (semaphore == null) {
                    mutexByTool.put(tool, semaphore = new Semaphore(1));
                }
                try {
                    semaphore.acquire();
                } catch (InterruptedException x) {
                    LOG.log(Level.WARNING, null, x);
                    break;
                }
                try {
                    FilePath result;
                    // XXX cannot send to project's build log from here
                    LogTaskListener log = new LogTaskListener(LOG, Level.INFO);
                    try {
                        result = installer.performInstallation(tool, node, log);
                    } finally {
                        log.close();
                    }
                    return result.getRemote();
                } catch (Exception x) {
                    LOG.log(Level.WARNING, "Failed to install " + tool.getName() + " on " + node.getDisplayName(), x);
                    break;
                } finally {
                    semaphore.release();
                }
            }
        }
        return null;
    }

}
