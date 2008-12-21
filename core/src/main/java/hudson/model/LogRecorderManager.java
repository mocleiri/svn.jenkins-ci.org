package hudson.model;

import hudson.util.CopyOnWriteMap;
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

/**
 * Owner of {@link LogRecorder}s, bound to "/log".
 *
 * @author Kohsuke Kawaguchi
 */
public class LogRecorderManager extends AbstractModelObject {
    /**
     * {@link LogRecorder}s.
     */
    public transient final Map<String,LogRecorder> logRecorders = new CopyOnWriteMap.Hash<String,LogRecorder>();

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
}
