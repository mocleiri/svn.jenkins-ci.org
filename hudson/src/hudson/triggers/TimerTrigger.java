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

/**
 * {@link Trigger} that runs a job periodically.
 *
 * @author Kohsuke Kawaguchi
 */
public class TimerTrigger implements Trigger {
    private final String cronTabSpec;
    private transient CronTabList tabs;

    private transient Project project;

    public TimerTrigger(String cronTabSpec) throws ANTLRException {
        this.cronTabSpec = cronTabSpec;
        this.tabs = CronTabList.create(cronTabSpec);
    }

    public void start(Project project) {
        this.project = project;
    }

    public void stop() {
        // timer triggered as long as this object is reachable from Project
    }

    private void check(Calendar cal) {
        if(tabs.check(cal))
            project.scheduleBuild();
    }

    public Descriptor<Trigger> getDescriptor() {
        return DESCRIPTOR;
    }

    private Object readResolve() throws ObjectStreamException {
        try {
            tabs = CronTabList.create(cronTabSpec);
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
        timer.scheduleAtFixedRate(new Cron(),0,100*60/*every minute*/);
    }

    /**
     * Runs every minute to check {@link TimerTrigger} and schedules build.
     */
    private static class Cron extends TimerTask {
        private final Calendar cal = new GregorianCalendar();

        public void run() {
            cal.add(Calendar.MINUTE,1);

            Hudson inst = Hudson.getInstance();
            for (Job job : inst.getJobs()) {
                if (job instanceof Project) {
                    Project p = (Project) job;
                    Trigger trigger = p.getTriggers().get(DESCRIPTOR);
                    if(trigger!=null) {
                        ((TimerTrigger)trigger).check(cal);
                    }
                }
            }
        }
    }
}
