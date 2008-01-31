package hudson.util;

import hudson.model.Hudson;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Makes sure that no other Hudson uses our <tt>HUDSON_HOME</tt> directory,
 * to forestall the problem of running multiple instances of Hudson that point to the same data directory.
 *
 * <p>
 * This set up error occasionally happens especialy when the user is trying to reassign the context path of the app,
 * and it results in a hard-to-diagnose error, so we actively check this.
 *
 * <p>
 * The mechanism is simple. This class occasionally updates a known file inside the hudson home directory,
 * and whenever it does so, it monitors the timestamp of the file to make sure no one else is updating
 * this file. In this way, while we cannot detect the problem right away, within a reasonable time frame
 * we can detect the collision.
 *
 * <p>
 * More traditional way of doing this is to use a lock file with PID in it, but unfortunately in Java,
 * there's no reliabe way to obtain PID.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.178
 */
public class DoubleLaunchChecker {
    /**
     * The timestamp of the owner file when we updated it for the last time.
     * 0 to indicate that there was no update before.
     */
    private long lastWriteTime = 0L;

    /**
     * Once the error is reported, the user can choose to ignore and proceed anyway,
     * in which case the flag is set to true.
     */
    private boolean ignore = false;

    private final Random random = new Random();

    public final File home;

    public DoubleLaunchChecker() {
        home = Hudson.getInstance().getRootDir();
    }

    protected void execute() {
        File timestampFile = new File(home,".owner");

        long t = timestampFile.lastModified();
        if(t!=0 && lastWriteTime!=0 && t!=lastWriteTime && !ignore) {
            // we noticed that someone else have updated this file.
            // switch GUI to display this error.
            Hudson.getInstance().servletContext.setAttribute("app",this);
            LOGGER.severe("Collision detected. timestamp="+t+", expected="+lastWriteTime);
            // we need to continue updating this file, so that the other Hudson would notice the problem, too.
        }

        try {
            FileUtils.writeStringToFile(timestampFile,"This file is used to make sure only one Hudson instance uses this directory");
            lastWriteTime = timestampFile.lastModified();
        } catch (IOException e) {
            // if failed to write, err on the safe side and assume things are OK.
            lastWriteTime=0;
        }

        schedule();
    }

    /**
     * Schedules the next execution.
     */
    public void schedule() {
        // randomize the scheduling so that multiple Hudson instances will write at the file at different time
        int MINUTE = 1000*60;
        Trigger.timer.schedule(new SafeTimerTask() {
            protected void doRun() {
                execute();
            }
        },(random.nextInt(30*MINUTE)+60*MINUTE));
    }

    /**
     * Serve all URLs with the index view.
     */
    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        rsp.setStatus(SC_INTERNAL_SERVER_ERROR);
        req.getView(this,"index.jelly").forward(req,rsp);
    }

    /**
     * Ignore the problem and go back to using Hudson.
     */
    public void doIgnore(StaplerRequest req, StaplerResponse rsp) throws IOException {
        ignore = true;
        Hudson.getInstance().servletContext.setAttribute("app",Hudson.getInstance());
        rsp.sendRedirect2(req.getContextPath()+'/');
    }

    private static final Logger LOGGER = Logger.getLogger(DoubleLaunchChecker.class.getName());
}
