package hudson.triggers;

import antlr.ANTLRException;
import hudson.model.Descriptor;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link Trigger} that runs a job periodically.
 *
 * @author Kohsuke Kawaguchi
 */
public class TimerTrigger extends Trigger {
    public TimerTrigger(String cronTabSpec) throws ANTLRException {
        super(cronTabSpec);
    }

    protected void run() {
        project.scheduleBuild();
    }

    public Descriptor<Trigger> getDescriptor() {
        return DESCRIPTOR;
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


}
