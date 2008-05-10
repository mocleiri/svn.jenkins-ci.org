package hudson.plugins.clearcase.action;

import static hudson.plugins.clearcase.util.OutputFormat.*;

import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.regex.Matcher;

/**
 * Default action for polling for changes in a repository.
 */
public class DefaultPollAction implements PollAction {
    
    private static final String[] HISTORY_FORMAT = {DATE_NUMERIC,
        NAME_ELEMENTNAME,
        NAME_VERSIONID,
        EVENT, 
        OPERATION
    };
    private ClearToolFormatHandler historyHandler = new ClearToolFormatHandler(HISTORY_FORMAT);    
    private ClearTool cleartool;

    public DefaultPollAction(ClearTool cleartool) {
        this.cleartool = cleartool;
    }

    public boolean getChanges(Date time, String viewName, String[] branchNames, String vobPaths) throws IOException, InterruptedException {
        for (String branchName : branchNames) {            
            Reader lshistoryOutput = cleartool.lshistory(historyHandler.getFormat(), time, viewName, branchName, vobPaths);            
            if (parseHistoryOutputForChanges(new BufferedReader(lshistoryOutput))) {
                return true;
            }
        } 
        return false;
    }
    
    private boolean parseHistoryOutputForChanges(BufferedReader reader) throws IOException, InterruptedException {
        String line = reader.readLine();
        while (line != null) {

            //TODO: better error handling
            if (line.startsWith("cleartool: Error:")) {
                line = reader.readLine();
                continue;
            }
            Matcher matcher = historyHandler.checkLine(line);

            // finder find start of lshistory entry
            if (matcher != null) {
                // read values;
                String dateStr = matcher.group(1);
                String name = matcher.group(2);
                String version = matcher.group(3);
                String event = matcher.group(4);
                String operation = matcher.group(5);

                if (version.endsWith("/0") || version.endsWith("\\0") || event.equalsIgnoreCase("create branch")) {
                    line = reader.readLine();
                    continue;
                }
                return true;
            }
            line = reader.readLine();
        }
        return false;
    }
    
}
