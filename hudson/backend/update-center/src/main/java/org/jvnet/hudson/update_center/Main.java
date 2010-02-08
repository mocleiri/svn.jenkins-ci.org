package org.jvnet.hudson.update_center;

import net.sf.json.JSONObject;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;

import javax.xml.rpc.ServiceException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    @Option(name="-c",metaVar="DIR",usage="Cache directory to store downloaded plugins")
    public File cacheDir = new File("./cache");

    @Option(name="-o",usage="json file")
    public File output = new File("output.json");

    @Option(name="-h",usage="htaccess file")
    public File htaccess = new File(".htaccess.plugins");

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        CmdLineParser p = new CmdLineParser(main);
        p.parseArgument(args);

        main.run();
    }

    public void run() throws Exception {
        JavaNet jn = JavaNet.connect();
        JNProject p = jn.getProject("hudson");
	
        JSONObject root = new JSONObject();
        root.put("updateCenterVersion","1");    // we'll bump the version when we make incompatible changes
        root.put("plugins", buildPlugins(p));
        root.put("core", buildCore(p));

        PrintWriter pw = new PrintWriter(new FileWriter(output));
        pw.println("updateCenter.post(");
        pw.println(root.toString(2));
        pw.println(");");
        pw.close();
    }

    /**
     * Build JSON for the plugin list.
     */
    protected JSONObject buildPlugins(JNProject p) throws ProcessingException, IOException, ServiceException, InterruptedException {
        JNFileFolder pluginsFolder = p.getFolder("/plugins");
        ConfluencePluginList cpl = new ConfluencePluginList();

        Cache cache = new Cache(cacheDir);

        PrintWriter redirect = new PrintWriter(new FileWriter(htaccess),true);

        JSONObject plugins = new JSONObject();
        for( JNFileFolder dir : pluginsFolder.getSubFolders().values() ) {
            System.out.println(dir.getName());
            if(dir.getName().equals("google-desktop-gadget"))
                continue;       // this is not really a Hudson plugin. So excluding for now
            if(dir.getName().equals("ivy2"))
                continue;       // subsumed into the ivy plugin. Hiding from the update center

            // pausing, to give java.net a bit of breath time
            Thread.sleep(5000);

            VersionedFile latest = findLatestPlugin(dir);
            VersionedFile secondLatest = findSecondLatestPlugin(dir);

            if(latest==null)
                continue;       // couldn't find it

            if (secondLatest != null) {
                latest.setPrevModified(secondLatest.getLastModified());
            } else {
                // If there's no previous version of the plugin, set the previous date to epoch.
                latest.setPrevModified(new Date(0));
            }

            Plugin plugin = new Plugin(dir.getName(),latest,cpl,cache);

            if(plugin.page!=null)
                System.out.println("=> "+plugin.page.getTitle());
            System.out.println("=> "+plugin.toJSON());

            plugins.put(plugin.artifactId,plugin.toJSON());
            redirect.printf("Redirect 302 /latest/%s.hpi %s\n", plugin.artifactId, latest.url);
        }

        redirect.close();
        cache.save();

        return plugins;
    }


    
    protected VersionedFile findLatestPlugin(JNFileFolder dir) throws ProcessingException {
        return JavaNetVersionedFile.findLatestFrom(dir);
    }

    protected VersionedFile findSecondLatestPlugin(JNFileFolder dir) throws ProcessingException {
        return JavaNetVersionedFile.findSecondLatestFrom(dir);
    }

    /**
     * Build JSON for the core Hudson.
     */
    protected JSONObject buildCore(JNProject p) throws ProcessingException {
        VersionedFile latest = findLatestCore(p);
        JSONObject core = latest.toJSON("core");
        System.out.println("core\n=> "+ core);
        return core;
    }

    protected VersionedFile findLatestCore(JNProject p) throws ProcessingException {
        JNFileFolder release = p.getFolder("/releases");
        return JavaNetVersionedFile.findLatestFrom(release);
    }
}
