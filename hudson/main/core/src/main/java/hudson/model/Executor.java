package hudson.model;

import hudson.Util;
import hudson.security.ACL;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;

import javax.servlet.ServletException;
import java.io.IOException;


/**
 * Thread that executes builds.
 *
 * @author Kohsuke Kawaguchi
 */
public class Executor extends Thread implements ModelObject {
    private final Computer owner;
    private final Queue queue;

    private long startTime;

    /**
     * Executor number that identifies it among other executors for the same {@link Computer}.
     */
    private int number;
    /**
     * {@link Queue.Executable} being executed right now, or null if the executor is idle.
     */
    private volatile Queue.Executable executable;

    private Throwable causeOfDeath;

    public Executor(Computer owner) {
        super("Executor #"+owner.getExecutors().size()+" for "+owner.getDisplayName());
        this.owner = owner;
        this.queue = Hudson.getInstance().getQueue();
        this.number = owner.getExecutors().size();
        start();
    }

    public void run() {
        // run as the system user. see ACL.SYSTEM for more discussion about why this is somewhat broken
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);

        try {
            while(true) {
                if(Hudson.getInstance().isTerminating())
                    return;

                synchronized(owner) {
                    if(owner.getNumExecutors()<owner.getExecutors().size()) {
                        // we've got too many executors.
                        owner.removeExecutor(this);
                        return;
                    }
                }

                Queue.Task task;
                try {
                    task = queue.pop();
                } catch (InterruptedException e) {
                    continue;
                }

                try {
                    startTime = System.currentTimeMillis();
                    executable = task.createExecutable();
                    queue.execute(executable,task);
                } catch (Throwable e) {
                    // for some reason the executor died. this is really
                    // a bug in the code, but we don't want the executor to die,
                    // so just leave some info and go on to build other things
                    e.printStackTrace();
                }
                executable = null;
            }
        } catch(RuntimeException e) {
            causeOfDeath = e;
            throw e;
        } catch (Error e) {
            causeOfDeath = e;
            throw e;
        }
    }

    /**
     * Returns the current {@link Queue.Task} this executor is running.
     *
     * @return
     *      null if the executor is idle.
     */
    public Queue.Executable getCurrentExecutable() {
        return executable;
    }

    /**
     * Same as {@link #getName()}.
     */
    public String getDisplayName() {
        return "Executor #"+getNumber();
    }

    /**
     * Gets the executor number that uniquely identifies it among
     * other {@link Executor}s for the same computer.
     *
     * @return
     *      a sequential number starting from 0.
     */
    public int getNumber() {
        return number;
    }

    /**
     * Returns true if this {@link Executor} is ready for action.
     */
    public boolean isIdle() {
        return executable==null;
    }

    /**
     * If this thread dies unexpectedly, obtain the cause of the failure.
     *
     * @return null if the death is expected death or the thread is {@link #isAlive() still alive}.
     * @since 1.142
     */
    public Throwable getCauseOfDeath() {
        return causeOfDeath;
    }

    /**
     * Returns the progress of the current build in the number between 0-100.
     *
     * @return -1
     *      if it's impossible to estimate the progress.
     */
    public int getProgress() {
        Queue.Executable e = executable;
        if(e==null)     return -1;
        long d = e.getParent().getEstimatedDuration();
        if(d<0)         return -1;

        int num = (int)((System.currentTimeMillis()-startTime)*100/d);
        if(num>=100)    num=99;
        return num;
    }

    /**
     * Computes a human-readable text that shows the expected remaining time
     * until the build completes.
     */
    public String getEstimatedRemainingTime() {
        Queue.Executable e = executable;
        if(e==null)     return Messages.Executor_NotAvailable();

        long d = e.getParent().getEstimatedDuration();
        if(d<0)         return Messages.Executor_NotAvailable();

        long eta = d-(System.currentTimeMillis()-startTime);
        if(eta<=0)      return Messages.Executor_NotAvailable();

        return Util.getTimeSpanString(eta);
    }

    /**
     * Stops the current build.
     */
    public void doStop( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        executable.getParent().checkAbortPermission();
        interrupt();
        rsp.forwardToPreviousPage(req);
    }

    /**
     * Checks if the current user has a permission to stop this build. 
     */
    public boolean hasStopPermission() {
        Queue.Executable e = executable;
        return e!=null && e.getParent().hasAbortPermission();
    }

    public Computer getOwner() {
        return owner;
    }

    /**
     * Returns the executor of the current thread.
     */
    public static Executor currentExecutor() {
        return (Executor)Thread.currentThread();
    }
}
