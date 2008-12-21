package hudson.model;

import com.thoughtworks.xstream.XStream;
import hudson.BulkChange;
import hudson.XmlFile;
import hudson.util.CopyOnWriteList;
import hudson.util.RingBufferLogHandler;
import hudson.util.WeakLogHandler;
import hudson.util.XStream2;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Records a selected set of logs so that the system administrator
 * can diagnose a specific aspect of the system.
 *
 * TODO: still a work in progress.
 *
 * <h3>Access Control</h3>
 * {@link LogRecorder} is only visible for administrators, and this access control happens at
 * {@link Hudson#getLog()}, the sole entry point for binding {@link LogRecorder} to URL.
 *
 * @author Kohsuke Kawaguchi
 * @see LogRecorderManager
 */
public class LogRecorder extends AbstractModelObject implements Saveable {
    private volatile String name;

    public final CopyOnWriteList<Target> targets = new CopyOnWriteList<Target>();

    private transient /*almost final*/ RingBufferLogHandler handler = new RingBufferLogHandler() {
        @Override
        public void publish(LogRecord record) {
            for (Target t : targets) {
                if(t.includes(record)) {
                    super.publish(record);
                    return;
                }
            }
        }
    };

    /**
     * Logger that this recorder monitors, and its log level.
     * Just a pair of (logger name,level) with convenience methods.
     */
    public static final class Target {
        public final String name;
        private final int level;

        public Target(String name, Level level) {
            this(name,level.intValue());
        }

        public Target(String name, int level) {
            this.name = name;
            this.level = level;
        }

        @DataBoundConstructor
        public Target(String name, String level) {
            this(name,Level.parse(level.toUpperCase()));
        }

        public Level getLevel() {
            return Level.parse(String.valueOf(level));
        }

        public boolean includes(LogRecord r) {
            if(r.getLevel().intValue() < level)
                return false;   // below the threshold
            if(!r.getLoggerName().startsWith(name))
                return false;   // not within this logger

            String rest = r.getLoggerName().substring(name.length());
            return rest.startsWith(".") || rest.length()==0;
        }

        public Logger getLogger() {
            return Logger.getLogger(name);
        }

        /**
         * Makes sure that the logger passes through messages at the correct level to us.
         */
        public void enable() {
            Logger l = getLogger();
            if(!lb.isLoggable(getLevel()))
                l.setLevel(getLevel());
        }
    }

    public LogRecorder(String name) {
        this.name = name;
        // register it only once when constructed, and when this object dies
        // WeakLogHandler will remove it
        new WeakLogHandler(handler,Logger.getLogger(""));
    }

    public String getDisplayName() {
        return name;
    }

    public String getSearchUrl() {
        return name;
    }

    public String getName() {
        return name;
    }

    private LogRecorderManager getParent() {
        return Hudson.getInstance().getLog();
    }

    /**
     * Accepts submission from the configuration page.
     */
    public synchronized void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        JSONObject src = req.getSubmittedForm();

        String newName = src.getString("name");
        if(!name.equals(newName)) {
            // rename
            getParent().logRecorders.remove(name);
            this.name = newName;
            getParent().logRecorders.put(name,this);
        }

        List<Target> newTargets = req.bindJSONToList(Target.class, src.get("targets"));
        for (Target t : newTargets)
            t.enable();
        targets.replaceBy(newTargets);

        save();
        rsp.sendRedirect2(".");
    }

    /**
     * Loads the settings from a file.
     */
    public synchronized void load() throws IOException {
        getConfigFile().unmarshal(this);
        for (Target t : targets)
            t.enable();
    }

    /**
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        if(BulkChange.contains(this))   return;
        getConfigFile().write(this);
    }

    /**
     * The file we save our configuration.
     */
    private XmlFile getConfigFile() {
        return new XmlFile(XSTREAM, new File(Hudson.getInstance().getRootDir(),"log/"+name+".xml"));
    }

    /**
     * Gets a view of the log records.
     */
    public List<LogRecord> getLogRecords() {
        return handler.getView();
    }

    /**
     * Thread-safe reusable {@link XStream}.
     */
    public static final XStream XSTREAM = new XStream2();

    static {
        XSTREAM.alias("log",LogRecorder.class);
        XSTREAM.alias("target",Target.class);
    }
}
