package test;

import hudson.Extension;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import hudson.scm.CVSSCM;
import hudson.tasks.Shell;

import java.io.IOError;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class ItemListenerImpl extends ItemListener {
    @Override
    public void onCreated(Item item) {
        try {
            if (item instanceof FreeStyleProject) {
                FreeStyleProject p = (FreeStyleProject) item;
                p.setScm(new CVSSCM("test","test",null,null,true,false,false,null));
                p.getBuildersList().add(new Shell("echo hi"));
            }
        } catch (IOException e) {
            throw new IOError(e); // lame error handling
        }
    }
}
