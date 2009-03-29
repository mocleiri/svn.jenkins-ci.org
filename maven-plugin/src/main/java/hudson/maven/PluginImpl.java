package hudson.maven;

import hudson.Plugin;
import hudson.model.Items;

/**
 * @author huybrechts
 */
public class PluginImpl extends Plugin {
    @Override
    public void start() throws Exception {
        super.start();

        Items.XSTREAM.alias("maven2", MavenModule.class);
        Items.XSTREAM.alias("dependency", ModuleDependency.class);
        Items.XSTREAM.alias("maven2-module-set", MavenModule.class);  // this was a bug, but now we need to keep it for compatibility
        Items.XSTREAM.alias("maven2-moduleset", MavenModuleSet.class);
    }


    public static ExecutedMojo.Cache createExecutedMojoCache() {
        return new ExecutedMojo.Cache();
    }

}
