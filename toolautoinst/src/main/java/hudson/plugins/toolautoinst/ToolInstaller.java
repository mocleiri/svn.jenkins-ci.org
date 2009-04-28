package hudson.plugins.toolautoinst;

import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An object which can ensure that a generic {@link ToolInstallation} in fact exists on a node.
 * The subclass should have a {@link ToolInstallerDescriptor}.
 * A {@code config.jelly} should be provided to customize specific fields;
 * {@code <st:include page="config-base.jelly"/>} to customize {@code toolName} and {@code label}.
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
    public final String getToolName() {
        return toolName;
    }

    /**
     * Names of all registered tools.
     */
    public static List<String> allToolNames() {
        List<String> names = new ArrayList<String>();
        for (ToolDescriptor d : ToolInstallation.all()) {
            for (ToolInstallation t : d.getInstallations()) {
                names.add(t.getName());
            }
        }
        return names;
    }

    /**
     * Label to limit which nodes this installation can be performed on.
     * Can be null to not impose a limit.
     */
    public final String getLabel() {
        return label;
    }

    /**
     * Checks whether this installer can be applied to a given tool.
     * (By default, just checks the tool name.)
     */
    public boolean appliesTo(ToolInstallation tool) {
        return tool.getName().equals(toolName);
    }

    /**
     * Checks whether this installer can be applied to a given node.
     * (By default, just checks the label.)
     */
    public boolean appliesTo(Node node) {
        Label l = Hudson.getInstance().getLabel(label);
        return l == null || l.contains(node);
    }

    /**
     * Ensure that the configured tool is really installed.
     * If it is already installed, do nothing.
     * Called only if {@link #appliesTo(ToolInstallation)} and {@link #appliesTo(Node)} are true.
     * @param tool the tool being installed
     * @param node the computer on which to install the tool
     * @param log any status messages produced by the installation go here
     * @return the (directory) path at which the tool can be found (like {@link ToolInstallation#getHome})
     * @throws IOException if installation fails
     * @throws InterruptedException if communication with a slave is interrupted
     */
    public abstract FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException;

    public ToolInstallerDescriptor<?> getDescriptor() {
        return (ToolInstallerDescriptor) Hudson.getInstance().getDescriptor(getClass());
    }

}
