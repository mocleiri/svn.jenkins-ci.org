package hudson.plugins.toolautoinst;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tasks.CommandInterpreter;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import java.io.IOException;
import java.util.Collections;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Installs a tool by running an arbitrary shell command.
 */
public class CommandExtraction extends ToolInstaller {

    /**
     * Command to execute, similar to {@link CommandInterpreter#command}.
     */
    private final String command;

    /**
     * Resulting tool home directory.
     */
    private final String toolHome;

    @DataBoundConstructor
    public CommandExtraction(String toolName, String label, String command, String toolHome) {
        super(toolName, label);
        this.command = command;
        this.toolHome = toolHome;
    }

    public String getCommand() {
        return command;
    }

    public String getToolHome() {
        return toolHome;
    }

    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath tools = node.getRootPath().child("tools");
        // XXX support Windows batch scripts, Unix scripts with interpreter line, etc. (see CommandInterpreter subclasses)
        FilePath script = tools.createTextTempFile("hudson", ".sh", command);
        try {
            String[] cmd = {"sh", "-e", script.getRemote()};
            int r = node.createLauncher(log).launch(cmd, Collections.<String,String>emptyMap(), log.getLogger(), tools).join();
            if (r != 0) {
                throw new IOException("Command returned status " + r);
            }
        } finally {
            script.delete();
        }
        return node.createPath(toolHome);
    }

    @Extension
    public static class DescriptorImpl extends ToolInstallerDescriptor<CommandExtraction> {

        public String getDisplayName() {
            return Messages.CommandExtraction_DescriptorImpl_displayName();
        }

        public FormValidation doCheckCommand(@QueryParameter String value) {
            if (value.length() > 0) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.CommandExtraction_no_command());
            }
        }

        public FormValidation doCheckToolHome(@QueryParameter String value) {
            if (value.length() > 0) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.CommandExtraction_no_toolHome());
            }
        }

    }

}
