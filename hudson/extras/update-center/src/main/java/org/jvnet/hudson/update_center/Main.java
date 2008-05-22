package org.jvnet.hudson.update_center;

import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JavaNet;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) throws Exception {
        ConfluencePluginList cpl = new ConfluencePluginList();

        JavaNet jn = JavaNet.connect();
        JNProject p = jn.getProject("hudson");
        JNFileFolder plugins = p.getFolder("/plugins");
        for( JNFileFolder dir : plugins.getSubFolders().values() ) {
            System.out.println(dir.getName());

            VersionedFile latest = VersionedFile.findLatestFrom(dir);
            if(latest==null)
                continue;       // couldn't find it

            Plugin plugin = new Plugin(dir.getName(),latest,cpl);
            if(plugin.page!=null)
                System.out.println("=> "+plugin.page.getTitle());
            System.out.println("=> "+latest.toJSON(dir.getName()));
        }

        JNFileFolder release = p.getFolder("/releases");
        VersionedFile latest = VersionedFile.findLatestFrom(release);
        System.out.println("core\n=> "+latest.toJSON("core"));
    }
}
