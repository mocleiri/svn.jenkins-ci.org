package hudson.plugins.clearcase.action;

import hudson.scm.ChangeLogSet;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Henrik L. Hansen
 */
public interface ChangeLogAction {
    
    List<? extends ChangeLogSet.Entry> getChanges(Date time, String viewName, String branchName, String vobPaths) throws IOException,InterruptedException;
}
