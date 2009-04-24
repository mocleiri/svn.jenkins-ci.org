package hudson.plugins.toolautoinst;

import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An object which can ensure that a generic {@link ToolInstallation} in fact exists on a node.
 * The subclass should provide a {@code config.jelly}.
 * @see ToolInstallerDescriptor
 */
public abstract class ToolInstaller implements Describable<ToolInstaller>, ExtensionPoint {

    private final String toolName;
    private final String label;

    /**
     * Subclasses should pass these parameters in using {@link DataBoundConstructor}.
     */
    protected ToolInstaller(String toolName, String label) {
        this.toolName = toolName;
        this.label = Util.fixEmptyAndTrim(label);
    }

    /**
     * Associated tool name.
     */
    public String getToolName() {
        return toolName;
    }

    public static List<ToolInstallation> allTools() {
        List<ToolInstallation> tools = new ArrayList<ToolInstallation>();
        for (ToolDescriptor d : ToolInstallation.all()) {
            tools.addAll(Arrays.asList(d.getInstallations()));
        }
        return tools;
    }

    public static List<String> allToolNames() {
        List<String> names = new ArrayList<String>();
        for (ToolInstallation t : allTools()) {
            names.add(t.getName());
        }
        return names;
    }

    /**
     * Associated tool.
     * Its {@link ToolInstallation#getHome} will serve only as a fallback value.
     * @return tool with the configured name, or null if not found
     */
    public ToolInstallation getTool() {
        for (ToolInstallation t : allTools()) {
            if (t.getName().equals(toolName)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Label to limit which nodes this installation can be performed on.
     * Can be null to not impose a limit.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Ensure that the configured tool is really installed.
     * If it is already installed, do nothing.
     * @param node the computer on which to install the tool
     * @param log any status messages produced by the installation go here
     * @return the (directory) path at which the tool can be found (like {@link ToolInstallation#getHome})
     * @throws IOException if installation fails
     */
    public abstract FilePath ensureInstalled(Node node, OutputStream log) throws IOException;

    @SuppressWarnings("unchecked")
    public Descriptor<ToolInstaller> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }

}
