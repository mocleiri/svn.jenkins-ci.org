package hudson.plugins.toolautoinst;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.Hudson;

/**
 * Descriptor for a {@link ToolInstaller}.
 */
public abstract class ToolInstallerDescriptor<T extends ToolInstaller> extends Descriptor<ToolInstaller> {

    public static DescriptorExtensionList all() {
        return Hudson.getInstance().getDescriptorList(ToolInstaller.class);
    }

}
