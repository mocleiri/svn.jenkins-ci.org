/*
 * The MIT License
 *
 * Copyright (c) 2009, Sun Microsystems, Inc.
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

package hudson.tools;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tasks.CommandInterpreter;
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
    public CommandExtraction(String label, String command, String toolHome) {
        super(label);
        this.command = command;
        this.toolHome = toolHome;
    }

    public String getCommand() {
        return command;
    }

    public String getToolHome() {
        return toolHome;
    }

    public FilePath performInstallation(ToolInstallation tool, Node node, FilePath expectedLocation, TaskListener log) throws IOException, InterruptedException {
        FilePath tools = node.getRootPath().child("tools");
        // XXX support Windows batch scripts, Unix scripts with interpreter line, etc. (see CommandInterpreter subclasses)
        FilePath script = tools.createTextTempFile("hudson", ".sh", command);
        try {
            String[] cmd = {"sh", "-e", script.getRemote()};
            // XXX it always logs at least: "INFO: [tools] $ sh -e /hudson/tools/hudson8889216416382058262.sh"
            int r = node.createLauncher(log).launch(cmd, Collections.<String,String>emptyMap(), log.getLogger(), tools).join();
            if (r != 0) {
                throw new IOException("Command returned status " + r);
            }
        } finally {
            script.delete();
        }
        return tools.child(toolHome);
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
