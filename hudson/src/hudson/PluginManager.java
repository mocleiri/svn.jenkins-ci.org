package hudson;

import hudson.model.Hudson;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages {@link PluginWrapper}s.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PluginManager {
    private final List<PluginWrapper> plugins = new ArrayList<PluginWrapper>();

    /**
     * Plug-in root directory.
     */
    public final File rootDir;

    public final ServletContext context;

    public PluginManager(ServletContext context) {
        this.context = context;
        rootDir = new File(Hudson.getInstance().getRootDir(),"plugins");
        if(!rootDir.exists())
            rootDir.mkdirs();

        File[] archives = rootDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".hudson-plugin");
                    }
                });

        for( File arc : archives ) {
            try {
                plugins.add(new PluginWrapper(this,arc));
            } catch (IOException e) {
                System.err.println("Failed to load a plug-in "+arc);
                e.printStackTrace();
            }
        }
    }
}
