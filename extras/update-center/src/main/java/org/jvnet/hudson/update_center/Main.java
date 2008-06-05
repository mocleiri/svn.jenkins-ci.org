package org.jvnet.hudson.update_center;

import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;
import net.sf.json.JSONObject;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) throws Exception {
        JavaNet jn = JavaNet.connect();
        JNProject p = jn.getProject("hudson");

        JSONObject root = new JSONObject();
        root.put("updateCenterVersion","1");
        root.put("plugins", buildPlugins(p));
        root.put("core", buildCore(p));

        Writer w = args.length==0 ? new OutputStreamWriter(System.out) : new FileWriter(args[0]);
        PrintWriter pw = new PrintWriter(w);
        pw.println("updateCenter.post(");
        root.write(pw);
        pw.println(");");
        pw.close();
    }

    /**
     * Build JSON for the plugin list.
     */
    private static JSONObject buildPlugins(JNProject p) throws ProcessingException, IOException, ServiceException {
        JNFileFolder pluginsFolder = p.getFolder("/plugins");
        ConfluencePluginList cpl = new ConfluencePluginList();

        JSONObject plugins = new JSONObject();
        for( JNFileFolder dir : pluginsFolder.getSubFolders().values() ) {
            System.out.println(dir.getName());

            VersionedFile latest = VersionedFile.findLatestFrom(dir);
            if(latest==null)
                continue;       // couldn't find it

            Plugin plugin = new Plugin(dir.getName(),latest,cpl);

            if(plugin.page!=null)
                System.out.println("=> "+plugin.page.getTitle());
            System.out.println("=> "+plugin.toJSON());

            plugins.put(plugin.artifactId,plugin.toJSON());
        }
        return plugins;
    }

    /**
     * Build JSON for the core Hudson.
     */
    private static JSONObject buildCore(JNProject p) throws ProcessingException {
        JNFileFolder release = p.getFolder("/releases");
        VersionedFile latest = VersionedFile.findLatestFrom(release);
        JSONObject core = latest.toJSON("core");
        System.out.println("core\n=> "+ core);
        return core;
    }
}
