package hudson.triggers;

import hudson.model.Project;
import hudson.model.Descriptor;
import hudson.scheduler.Scheduler;
import hudson.scheduler.Task;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link Trigger} that runs a job periodically.
 *
 * @author Kohsuke Kawaguchi
 */
public class TimerTrigger implements Trigger, Runnable {
    private Scheduler scheduler;

    private transient Task task;

    private transient Project project;

    public void start(Project project) {
        task = new Task(scheduler,this);
        this.project = project;
    }

    public void stop() {
        task.cancel();
        task = null;
    }

    public void run() {
        project.scheduleBuild();
    }

    public Descriptor<Trigger> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor<Trigger> DESCRIPTOR = new Descriptor<Trigger>(TimerTrigger.class) {
        public String getDisplayName() {
            return "Build periodically";
        }

        public Trigger newInstance(HttpServletRequest req) {
            // TODO
            throw new UnsupportedOperationException();
        }
    };
}
