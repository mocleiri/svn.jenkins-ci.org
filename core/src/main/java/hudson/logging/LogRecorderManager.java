package hudson.logging;

import hudson.util.CopyOnWriteMap;
import hudson.FeedAdapter;
import hudson.Functions;
import hudson.model.AbstractModelObject;
import hudson.model.Hudson;
import hudson.model.RSS;
import hudson.tasks.Mailer;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.File;
import java.io.FileFilter;
import java.text.ParseException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.LogRecord;
import java.util.logging.Level;

/**
 * Owner of {@link LogRecorder}s, bound to "/log".
 *
 * @author Kohsuke Kawaguchi
 */
public class LogRecorderManager extends AbstractModelObject {
    /**
     * {@link LogRecorder}s.
     */
    public transient final Map<String,LogRecorder> logRecorders = new CopyOnWriteMap.Tree<String,LogRecorder>();

    public String getDisplayName() {
        return "log";
    }

    public String getSearchUrl() {
        return "/log";
    }

    public LogRecorder getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        return getLogRecorder(token);
    }

    public LogRecorder getLogRecorder(String token) {
        return logRecorders.get(token);
    }

    public void load() throws IOException {
        File dir = new File(Hudson.getInstance().getRootDir(), "log");
        File[] files = dir.listFiles((FileFilter)new WildcardFileFilter("*.xml"));
        if(files==null)     return;
        for (File child : files) {
            String name = child.getName();
            name = name.substring(0,name.length()-4);   // cut off ".xml"
            LogRecorder lr = new LogRecorder(name);
            lr.load();
            logRecorders.put(name,lr);
        }
    }

    /**
     * Creates a new log recorder.
     */
    public void doNewLogRecorder( StaplerRequest req, StaplerResponse rsp, @QueryParameter String name) throws IOException, ServletException {
        try {
            Hudson.checkGoodName(name);
        } catch (ParseException e) {
            sendError(e, req, rsp);
            return;
        }
        
        logRecorders.put(name,new LogRecorder(name));

        // redirect to the config screen
        rsp.sendRedirect2(name+"/configure");
    }

    /**
     * RSS feed for log entries.
     */
    public void doLogRss( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        List<LogRecord> logs = Hudson.logRecords;

        // filter log records based on the log level
        String level = req.getParameter("level");
        if(level!=null) {
            Level threshold = Level.parse(level);
            List<LogRecord> filtered = new ArrayList<LogRecord>();
            for (LogRecord r : logs) {
                if(r.getLevel().intValue() >= threshold.intValue())
                    filtered.add(r);
            }
            logs = filtered;
        }

        RSS.forwardToRss("Hudson log","", logs, new FeedAdapter<LogRecord>() {
            public String getEntryTitle(LogRecord entry) {
                return entry.getMessage();
            }

            public String getEntryUrl(LogRecord entry) {
                return "log";   // TODO: one URL for one log entry?
            }

            public String getEntryID(LogRecord entry) {
                return String.valueOf(entry.getSequenceNumber());
            }

            public String getEntryDescription(LogRecord entry) {
                return Functions.printLogRecord(entry);
            }

            public Calendar getEntryTimestamp(LogRecord entry) {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeInMillis(entry.getMillis());
                return cal;
            }

            public String getEntryAuthor(LogRecord entry) {
                return Mailer.DESCRIPTOR.getAdminAddress();
            }
        },req,rsp);
    }
}
