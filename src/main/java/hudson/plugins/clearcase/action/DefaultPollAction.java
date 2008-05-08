package hudson.plugins.clearcase.action;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolHistoryParser;
import hudson.util.IOException2;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Default action for polling for changes in a repository.
 */
public class DefaultPollAction implements PollAction {

    private ClearTool cleartool;

    public DefaultPollAction(ClearTool cleartool) {
        this.cleartool = cleartool;
    }

    public List<ClearCaseChangeLogEntry> getChanges(Date time, String viewName, String branchName, String vobPaths) throws IOException, InterruptedException {
        Reader reader = cleartool.lshistory(time, viewName, branchName, vobPaths);
        if (reader != null) {
            try {
                ClearToolHistoryParser parser = new ClearToolHistoryParser();
                return parser.parse(reader);
            } catch (ParseException pe) {
                throw new IOException2("There was a problem parsing the history log.", pe);
            }
        } else {
            return new ArrayList<ClearCaseChangeLogEntry>();
        }
    }
}
