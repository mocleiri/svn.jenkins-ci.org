package org.jvnet.hudson.update_center;

import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Argument;
import net.sf.json.JSONObject;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Properties;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    @Option(name="-c",metaVar="DIR",usage="Cache directory to store downloaded plugins")
    public File cacheDir = new File("./cache");

    @Option(name="-o",usage="json file")
    public File output = new File("output.json");

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
        root.write(pw);
        pw.println(");");
        pw.close();
    }

    /**
     * Build JSON for the plugin list.
     */
    private JSONObject buildPlugins(JNProject p) throws ProcessingException, IOException, ServiceException {
        JNFileFolder pluginsFolder = p.getFolder("/plugins");
        ConfluencePluginList cpl = new ConfluencePluginList();

        Cache cache = new Cache(cacheDir);

        JSONObject plugins = new JSONObject();
        for( JNFileFolder dir : pluginsFolder.getSubFolders().values() ) {
            System.out.println(dir.getName());

            VersionedFile latest = VersionedFile.findLatestFrom(dir);
            if(latest==null)
                continue;       // couldn't find it

            Plugin plugin = new Plugin(dir.getName(),latest,cpl,cache);

            if(plugin.page!=null)
                System.out.println("=> "+plugin.page.getTitle());
            System.out.println("=> "+plugin.toJSON());

            plugins.put(plugin.artifactId,plugin.toJSON());
        }

        cache.save();

        return plugins;
    }

    /**
     * Build JSON for the core Hudson.
     */
    private JSONObject buildCore(JNProject p) throws ProcessingException {
        JNFileFolder release = p.getFolder("/releases");
        VersionedFile latest = VersionedFile.findLatestFrom(release);
        JSONObject core = latest.toJSON("core");
        System.out.println("core\n=> "+ core);
        return core;
    }
}
