package hudson.plugins.jprt;

import hudson.model.Run;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * {@link Run} for {@link JPRTJob}.
 *
 * @author Kohsuke Kawaguchi
 */
public class JPRTRun extends Run<JPRTJob,JPRTRun> {
    private final File archiveDir;

    public JPRTRun(JPRTJob job, JPRTRun prevBuild, File archiveDir) throws ParseException {
        super(job, prevBuild, getTimestamp(archiveDir));
        // TODO: think about a way to give a consistent build number
        this.number = prevBuild!=null ? prevBuild.number+1 : 1;
        this.archiveDir = archiveDir;
    }

    /**
     * Returns the Job ID in the JPRT sense. The archive directory name
     * is the job ID.
     */
    public String getId() {
        return archiveDir.getName();
    }

    /**
     * Computes the timestamp of the build from the JPRT archive directory name format.
     */
    private static Calendar getTimestamp(File dir) throws ParseException {
        String name = dir.getName();
        name = name.substring(0,17);
        synchronized(TIME_FORMATTER) {
            Calendar cal = new GregorianCalendar();
            cal.setTime(TIME_FORMATTER.parse(name));
            return cal;
        }
    }

    protected static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
}
