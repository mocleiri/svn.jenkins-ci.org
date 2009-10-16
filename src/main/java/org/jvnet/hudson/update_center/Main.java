package org.jvnet.hudson.update_center;

import net.sf.json.JSONObject;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.jnt.ProcessingException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
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
        MavenRepository repo = new MavenRepository();

        JSONObject root = new JSONObject();
        root.put("updateCenterVersion","1");    // we'll bump the version when we make incompatible changes
        root.put("plugins", buildPlugins(repo));
        root.put("core", buildCore(repo));

        PrintWriter pw = new PrintWriter(new FileWriter(output));
        pw.println("updateCenter.post(");
        pw.println(root.toString(2));
        pw.println(");");
        pw.close();
    }

    /**
     * Build JSON for the plugin list.
     * @param repository
     */
    protected JSONObject buildPlugins(MavenRepository repository) throws Exception {
        ConfluencePluginList cpl = new ConfluencePluginList();

        PrintWriter redirect = new PrintWriter(new FileWriter(htaccess),true);

        JSONObject plugins = new JSONObject();
        for( PluginHistory hpi : repository.listHudsonPlugins() ) {
            System.out.println(hpi.artifactId);
            if(hpi.artifactId.equals("ivy2"))
                continue;       // subsumed into the ivy plugin. Hiding from the update center

            List<HPI> versions = new ArrayList<HPI>(hpi.artifacts.values());
            HPI latest = versions.get(versions.size()-1);
            HPI previous = versions.size()>1 ? versions.get(versions.size()-2) : null;

            Plugin plugin = new Plugin(hpi.artifactId,latest,previous,cpl);

            if(plugin.page!=null)
                System.out.println("=> "+plugin.page.getTitle());
            System.out.println("=> "+plugin.toJSON());

            plugins.put(plugin.artifactId,plugin.toJSON());
            redirect.printf("Redirect 302 /latest/%s.hpi %s\n", plugin.artifactId, latest.getURL());
        }

        redirect.close();

        return plugins;
    }

    /**
     * Build JSON for the core Hudson.
     */
    protected JSONObject buildCore(MavenRepository repository) throws ProcessingException, IOException, AbstractArtifactResolutionException {
        MavenArtifact latest = repository.getLatestHudsonWar();
        JSONObject core = latest.toJSON("core");
        System.out.println("core\n=> "+ core);
        return core;
    }
}
