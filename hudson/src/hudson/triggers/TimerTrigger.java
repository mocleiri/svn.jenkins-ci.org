package hudson.triggers;

import antlr.ANTLRException;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Project;
import hudson.scheduler.CronTabList;

import javax.servlet.http.HttpServletRequest;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Trigger} that runs a job periodically.
 *
 * @author Kohsuke Kawaguchi
 */
public class TimerTrigger implements Trigger {
    private final String spec;
    private transient CronTabList tabs;

    private transient Project project;

    public TimerTrigger(String cronTabSpec) throws ANTLRException {
        this.spec = cronTabSpec;
        this.tabs = CronTabList.create(cronTabSpec);
    }

    public String getSpec() {
        return spec;
    }

    public void start(Project project) {
        this.project = project;
    }

    public void stop() {
        // timer triggered as long as this object is reachable from Project
    }

    private boolean check(Calendar cal) {
        if(tabs.check(cal)) {
            project.scheduleBuild();
            return true;
        } else
            return false;
    }

    public Descriptor<Trigger> getDescriptor() {
        return DESCRIPTOR;
    }

    private Object readResolve() throws ObjectStreamException {
        try {
            tabs = CronTabList.create(spec);
        } catch (ANTLRException e) {
            InvalidObjectException x = new InvalidObjectException(e.getMessage());
            x.initCause(e);
            throw x;
        }
        return this;
    }

    public static final Descriptor<Trigger> DESCRIPTOR = new Descriptor<Trigger>(TimerTrigger.class) {
        public String getDisplayName() {
            return "Build periodically";
        }

        public String getHelpFile() {
            return "/help/project-config/timer.html";
        }

        public Trigger newInstance(HttpServletRequest req) throws InstantiationException {
            try {
                return new TimerTrigger(req.getParameter("timer_spec"));
            } catch (ANTLRException e) {
                throw new InstantiationException(e.toString(),e,"timer_spec");
            }
        }
    };


    public static final Timer timer = new Timer();
    static {
        timer.scheduleAtFixedRate(new Cron(),0,1000*60/*every minute*/);
    }

    /**
     * Runs every minute to check {@link TimerTrigger} and schedules build.
     */
    private static class Cron extends TimerTask {
        private final Calendar cal = new GregorianCalendar();

        public void run() {
            LOGGER.fine("cron checking "+cal.getTime().toLocaleString());

            try {
                Hudson inst = Hudson.getInstance();
                for (Job job : inst.getJobs()) {
                    if (job instanceof Project) {
                        Project p = (Project) job;
                        Trigger trigger = p.getTriggers().get(DESCRIPTOR);
                        if(trigger!=null) {
                            LOGGER.fine("cron checking "+p.getName());
                            if(((TimerTrigger)trigger).check(cal))
                                LOGGER.fine("cron triggered "+p.getName());
                        }
                    }
                }
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING,"Cron thread throw an exception",e);
                // bug in the code. Don't let the thread die.
                e.printStackTrace();
            }

            cal.add(Calendar.MINUTE,1);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(TimerTrigger.class.getName());
}
