package hudson.plugins.clearcase.action;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.plugins.clearcase.ClearTool;

import java.io.IOException;

/**
 * Check out action that will check out files into a UCM snapshot view.
 * Checking out the files will also udpate the load rules in the view.
 */
public class UcmSnapshotCheckoutAction implements CheckOutAction {

    private ClearTool cleartool;
    private String viewName;
    private String stream;
    private String loadRules;
        
    public UcmSnapshotCheckoutAction(ClearTool cleartool, String viewName, String stream, String loadRules) {
        super();
        this.cleartool = cleartool;
        this.viewName = viewName;
        this.stream = stream;
        this.loadRules = loadRules;
    }

    public boolean checkout(Launcher launcher, FilePath workspace) throws IOException, InterruptedException {        
        boolean localViewPathExists = new FilePath(workspace, viewName).exists();
        
        if (localViewPathExists) {
            cleartool.rmview(viewName);
            localViewPathExists = false;
        }

        cleartool.mkview(viewName, stream);

        for (String loadRule : Util.tokenize(loadRules, "\n")) {
            cleartool.update(viewName, loadRule.trim());
        }
        return true;
    }

}
