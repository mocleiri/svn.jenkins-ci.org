package hudson.plugins.toolautoinst;

import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.util.DescribableList;
import java.io.IOException;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public class PluginImpl extends Plugin {

    public DescribableList<ToolInstaller, Descriptor<ToolInstaller>> installers =
            new DescribableList<ToolInstaller, Descriptor<ToolInstaller>>(this);

    @Override
    public void start() throws Exception {
        super.start();
        load();
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, FormException {
        super.configure(req, formData);
        installers.rebuildHetero(req, formData, Hudson.getInstance().getDescriptorList(ToolInstaller.class), "installers");
        save();
    }

}
