package hudson.model;

import hudson.Util;
import hudson.model.Node.Mode;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;
import hudson.util.OneShotEvent;
import org.acegisecurity.AccessDeniedException;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.management.timer.Timer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Build queue.
 *
 * <p>
 * This class implements the core scheduling logic. {@link Task} represents the executable
 * task that are placed in the queue. While in the queue, it's wrapped into {@link Item}
 * so that we can keep track of additional data used for deciding what to exeucte when. 
 *
 * @author Kohsuke Kawaguchi
 */
public class Queue extends ResourceController {
    /**
     * Items in the queue ordered by {@link Item#timestamp}.
     *
     * <p>
     * This consists of {@link Item}s that cannot be run yet
     * because its time has not yet come.
     */
    private final Set<Item> queue = new TreeSet<Item>();

    /**
     * {@link Project}s that can be built immediately
     * but blocked because another build is in progress,
     * required {@link Resource}s are not available, or otherwise blocked
     * by {@link Task#isBuildBlocked()}.
     */
    private final Set<Task> blockedProjects = new HashSet<Task>();

    /**
     * {@link Project}s that can be built immediately
     * that are waiting for available {@link Executor}.
     */
    private final List<Task> buildables = new LinkedList<Task>();

    /**
     * Data structure created for each idle {@link Executor}.
     * This is an offer from the queue to an executor.
     *
     * <p>
     * It eventually receives a {@link #task} to build.
     */
    private static class JobOffer {
        final Executor executor;

        /**
         * Used to wake up an executor, when it has an offered
         * {@link Project} to build.
         */
        final OneShotEvent event = new OneShotEvent();
        /**
         * The project that this {@link Executor} is going to build.
         * (Or null, in which case event is used to trigger a queue maintenance.)
         */
        Task task;

        public JobOffer(Executor executor) {
            this.executor = executor;
        }

        public void set(Task p) {
            assert this.task ==null;
            this.task = p;
            event.signal();
        }

        public boolean isAvailable() {
            return task ==null && !executor.getOwner().isOffline();
        }

        public Node getNode() {
            return executor.getOwner().getNode();
        }

        public boolean isNotExclusive() {
            return getNode().getMode()== Mode.NORMAL;
        }
    }

    private final Map<Executor,JobOffer> parked = new HashMap<Executor,JobOffer>();

    public Queue() {
        // if all the executors are busy doing something, then the queue won't be maintained in
        // timely fashion, so use another thread to make sure it happens.
        new MaintainTask(this);
    }

    /**
     * Loads the queue contents that was {@link #save() saved}.
     */
    public synchronized void load() {
        // write out the contents of the queue
        try {
            File queueFile = getQueueFile();
            if(!queueFile.exists())
                return;

            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(queueFile)));
            String line;
            while((line=in.readLine())!=null) {
                AbstractProject j = Hudson.getInstance().getItemByFullName(line,AbstractProject.class);
                if(j!=null)
                    j.scheduleBuild();
            }
            in.close();
            // discard the queue file now that we are done
            queueFile.delete();
        } catch(IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load the queue file "+getQueueFile(),e);
        }
    }

    /**
     * Persists the queue contents to the disk.
     */
    public synchronized void save() {
        // write out the contents of the queue
        try {
            PrintWriter w = new PrintWriter(new FileOutputStream(
                getQueueFile()));
            for (Item i : getItems())
                w.println(i.task.getName());
            w.close();
        } catch(IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write out the queue file "+getQueueFile(),e);
        }
    }

    private File getQueueFile() {
        return new File(Hudson.getInstance().getRootDir(),"queue.txt");
    }

    /**
     * Schedule a new build for this project.
     *
     * @return
     *      true if the project is actually added to the queue.
     *      false if the queue contained it and therefore the add()
     *      was noop
     */
    public boolean add( AbstractProject p ) {
        return add(p,p.getQuietPeriod());
    }

    /**
     * Schedules a new build with a custom quiet period.
     *
     * <p>
     * Left for backward compatibility with &lt;1.114.
     *
     * @since 1.105
     */
    public synchronized boolean add( AbstractProject p, int quietPeriod ) {
        return add((Task)p,quietPeriod);
    }

    /**
     * Schedules an execution of a task.
     *
     * @param quietPeriod
     *      Number of seconds that the task will be placed in queue.
     *      Useful when the same task is likely scheduled for multiple
     *      times.
     * @since 1.114
     */
    public synchronized boolean add( Task p, int quietPeriod ) {
    	Item item = getItem(p);
    	Calendar due = new GregorianCalendar();
    	due.add(Calendar.SECOND, quietPeriod);
        if (item != null) {
            if (item.timestamp.before(due))
                return false; // no double queueing

            // allow the due date to be pulled in
            item.timestamp = due;
        } else {
            LOGGER.fine(p.getName() + " added to queue");

            // put the item in the queue
            queue.add(new Item(due, p));

        }
        scheduleMaintenance();   // let an executor know that a new item is in the queue.
        return true;
    }

    /**
     * Cancels the item in the queue.
     *
     * @return
     *      true if the project was indeed in the queue and was removed.
     *      false if this was no-op.
     */
    public synchronized boolean cancel( AbstractProject<?,?> p ) {
        LOGGER.fine("Cancelling "+p.getName());
        for (Iterator itr = queue.iterator(); itr.hasNext();) {
            Item item = (Item) itr.next();
            if(item.task ==p) {
                itr.remove();
                return true;
            }
        }
        // use bitwise-OR to make sure that both branches get evaluated all the time
        return blockedProjects.remove(p)|buildables.remove(p);
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty() && blockedProjects.isEmpty() && buildables.isEmpty();
    }

    private synchronized Item peek() {
        return queue.iterator().next();
    }

    /**
     * Gets a snapshot of items in the queue.
     */
    public synchronized Item[] getItems() {
        Item[] r = new Item[queue.size()+blockedProjects.size()+buildables.size()];
        queue.toArray(r);
        int idx=queue.size();
        Calendar now = new GregorianCalendar();
        for (Task p : blockedProjects) {
            r[idx++] = new Item(now, p, true, false);
        }
        for (Task p : buildables) {
            r[idx++] = new Item(now, p, false, true);
        }
        return r;
    }

    /**
     * Gets the information about the queue item for the given project.
     *
     * @return null if the project is not in the queue.
     */
    public synchronized Item getItem(Task p) {
        if(blockedProjects.contains(p))
            return new Item(new GregorianCalendar(),p,true,false);
        if(buildables.contains(p))
            return new Item(new GregorianCalendar(),p,false,true); 
        for (Item item : queue) {
            if (item.task == p)
                return item;
        }
        return null;
    }

    /**
     * Left for backward compatibility.
     * 
     * @see #getItem(Task)
     */
    public synchronized Item getItem(AbstractProject p) {
        return getItem((Task)p);
    }

    /**
     * Returns true if this queue contains the said project.
     */
    public synchronized boolean contains(Task p) {
        if(blockedProjects.contains(p) || buildables.contains(p))
            return true;
        for (Item item : queue) {
            if (item.task == p)
                return true;
        }
        return false;
    }

    /**
     * Called by the executor to fetch something to build next.
     *
     * This method blocks until a next project becomes buildable.
     */
    public Task pop() throws InterruptedException {
        final Executor exec = Executor.currentExecutor();

        try {
            while(true) {
                final JobOffer offer = new JobOffer(exec);
                long sleep = -1;

                synchronized(this) {
                    // consider myself parked
                    assert !parked.containsKey(exec);
                    parked.put(exec,offer);

                    // reuse executor thread to do a queue maintenance.
                    // at the end of this we get all the buildable jobs
                    // in the buildables field.
                    maintain();

                    // allocate buildable jobs to executors
                    Iterator<Task> itr = buildables.iterator();
                    while(itr.hasNext()) {
                        Task p = itr.next();

                        // one last check to make sure this build is not blocked.
                        if(isBuildBlocked(p)) {
                            itr.remove();
                            blockedProjects.add(p);
                            continue;
                        }
                        
                        JobOffer runner = choose(p);
                        if(runner==null)
                            // if we couldn't find the executor that fits,
                            // just leave it in the buildables list and
                            // check if we can execute other projects
                            continue;

                        // found a matching executor. use it.
                        runner.set(p);
                        itr.remove();
                    }

                    // we went over all the buildable projects and awaken
                    // all the executors that got work to do. now, go to sleep
                    // until this thread is awakened. If this executor assigned a job to
                    // itself above, the block method will return immediately.

                    if(!queue.isEmpty()) {
                        // wait until the first item in the queue is due
                        sleep = peek().timestamp.getTimeInMillis()-new GregorianCalendar().getTimeInMillis();
                        if(sleep <100)    sleep =100;    // avoid wait(0)
                    }
                }

                // this needs to be done outside synchronized block,
                // so that executors can maintain a queue while others are sleeping
                if(sleep ==-1)
                    offer.event.block();
                else
                    offer.event.block(sleep);

                synchronized(this) {
                    // retract the offer object
                    assert parked.get(exec)==offer;
                    parked.remove(exec);

                    // am I woken up because I have a project to build?
                    if(offer.task !=null) {
                        LOGGER.fine("Pop returning "+offer.task +" for "+exec.getName());
                        // if so, just build it
                        return offer.task;
                    }
                    // otherwise run a queue maintenance
                }
            }
        } finally {
            synchronized(this) {
                // remove myself from the parked list
                JobOffer offer = parked.remove(exec);
                if(offer!=null && offer.task !=null) {
                    // we are already assigned a project,
                    // ask for someone else to build it.
                    // note that while this thread is waiting for CPU
                    // someone else can schedule this build again,
                    // so check the contains method first.
                    if(!contains(offer.task))
                        buildables.add(offer.task);
                }

                // since this executor might have been chosen for
                // maintenance, schedule another one. Worst case
                // we'll just run a pointless maintenance, and that's
                // fine.
                scheduleMaintenance();
            }
        }
    }

    /**
     * Chooses the executor to carry out the build for the given project.
     *
     * @return
     *      null if no {@link Executor} can run it.
     */
    private JobOffer choose(Task p) {
        if(Hudson.getInstance().isQuietingDown()) {
            // if we are quieting down, don't run anything so that
            // all executors will be free.
            return null;
        }

        Label l = p.getAssignedLabel();
        if(l!=null) {
            // if a project has assigned label, it can be only built on it
            for (JobOffer offer : parked.values()) {
                if(offer.isAvailable() && l.contains(offer.getNode()))
                    return offer;
            }
            return null;
        }

        // if we are a large deployment, then we will favor slaves
        boolean isLargeHudson = Hudson.getInstance().getSlaves().size()>10;

        // otherwise let's see if the last node where this project was built is available
        // it has up-to-date workspace, so that's usually preferable.
        // (but we can't use an exclusive node)
        Node n = p.getLastBuiltOn();
        if(n!=null && n.getMode()==Mode.NORMAL) {
            for (JobOffer offer : parked.values()) {
                if(offer.isAvailable() && offer.getNode()==n) {
                    if(isLargeHudson && offer.getNode() instanceof Slave)
                        // but if we are a large Hudson, then we really do want to keep the master free from builds 
                        continue;
                    return offer;
                }
            }
        }

        // duration of a build on a slave tends not to have an impact on
        // the master/slave communication, so that means we should favor
        // running long jobs on slaves.
        // Similarly if we have many slaves, master should be made available
        // for HTTP requests and coordination as much as possible
        if(isLargeHudson || p.getEstimatedDuration()>15*60*1000) {
            // consider a long job to be > 15 mins
            for (JobOffer offer : parked.values()) {
                if(offer.isAvailable() && offer.getNode() instanceof Slave && offer.isNotExclusive())
                    return offer;
            }
        }

        // lastly, just look for any idle executor
        for (JobOffer offer : parked.values()) {
            if(offer.isAvailable() && offer.isNotExclusive())
                return offer;
        }

        // nothing available
        return null;
    }

    /**
     * Checks the queue and runs anything that can be run.
     *
     * <p>
     * When conditions are changed, this method should be invoked.
     *
     * This wakes up one {@link Executor} so that it will maintain a queue.
     */
    public synchronized void scheduleMaintenance() {
        // this code assumes that after this method is called
        // no more executors will be offered job except by
        // the pop() code.
        for (Entry<Executor, JobOffer> av : parked.entrySet()) {
            if(av.getValue().task ==null) {
                av.getValue().event.signal();
                return;
            }
        }
    }

    /**
     * Checks if the given task is blocked.
     */
    private  boolean isBuildBlocked(Task t) {
        return t.isBuildBlocked() || !canRun(t.getResourceList());
    }


    /**
     * Queue maintenance.
     *
     * Move projects between {@link #queue}, {@link #blockedProjects}, and {@link #buildables}
     * appropriately.
     */
    private synchronized void maintain() {
        if(LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Queue maintenance started "+this);

        Iterator<Task> itr = blockedProjects.iterator();
        while(itr.hasNext()) {
            Task p = itr.next();
            if(!isBuildBlocked(p)) {
                // ready to be executed
                LOGGER.fine(p.getName()+" no longer blocked");
                itr.remove();
                buildables.add(p);
            }
        }

        while(!queue.isEmpty()) {
            Item top = peek();

            if(!top.timestamp.before(new GregorianCalendar()))
                return; // finished moving all ready items from queue

            Task p = top.task;
            if(!isBuildBlocked(p)) {
                // ready to be executed immediately
                queue.remove(top);
                LOGGER.fine(p.getName()+" ready to build");
                buildables.add(p);
            } else {
                // this can't be built now because another build is in progress
                // set this project aside.
                queue.remove(top);
                LOGGER.fine(p.getName()+" is blocked");
                blockedProjects.add(p);
            }
        }
    }

    /**
     * Task whose execution is controlled by the queue.
     * <p>
     * {@link #equals(Object) Value equality} of {@link Task}s is used
     * to collapse two tasks into one. This is used to avoid infinite
     * queue backlog.
     */
    public interface Task extends ModelObject, ResourceActivity {
        /**
         * If this task needs to be run on a node with a particular label,
         * return that {@link Label}. Otherwise null, indicating
         * it can run on anywhere.
         */
        Label getAssignedLabel();

        /**
         * If the previous execution of this task run on a certain node
         * and this task prefers to run on the same node, return that.
         * Otherwise null.
         */
        Node getLastBuiltOn();

        /**
         * Returns true if the execution should be blocked
         * for temporary reasons.
         *
         * <p>
         * This can be used to define mutual exclusion that goes beyond
         * {@link #getResourceList()}.
         */
        boolean isBuildBlocked();

        /**
         * When {@link #isBuildBlocked()} is true, this method returns
         * human readable description of why the build is blocked.
         * Used for HTML rendering.
         */
        String getWhyBlocked();

        /**
         * Unique name of this task.
         * @see hudson.model.Item#getName()
         *
         * TODO: this doesn't make sense anymore. remove it.
         */
        String getName();

        /**
         * @see hudson.model.Item#getFullDisplayName()
         */
        String getFullDisplayName();

        /**
         * Estimate of how long will it take to execute this task.
         * Measured in milliseconds.
         *
         * @return
         *      -1 if it's impossible to estimate.
         */
        long getEstimatedDuration();

        /**
         * Creates {@link Executable}, which performs the actual execution of the task.
         */
        Executable createExecutable() throws IOException;

        /**
         * Checks the permission to see if the current user can abort this executable.
         * Returns normally from this method if it's OK.
         *
         * @throws AccessDeniedException if the permission is not granted.
         */
        void checkAbortPermission();

        /**
         * Works just like {@link #checkAbortPermission()} except it indicates the status by a return value,
         * instead of exception.
         */
        boolean hasAbortPermission();
    }

    public interface Executable extends Runnable {
        /**
         * Task from which this executable was created.
         * Never null.
         */
        Task getParent();

        /**
         * Called by {@link Executor} to perform the task
         */
        void run();
    }

    /**
     * Item in a queue.
     */
    @ExportedBean(defaultVisibility=999)
    public final class Item implements Comparable<Item> {
        /**
         * This item can be run after this time.
         */
        @Exported
        public Calendar timestamp;

        /**
         * Project to be built.
         */
        public final Task task;

        /**
         * Unique number of this {@link Item}.
         * Used to differentiate {@link Item}s with the same due date.
         */
        public final int id;

        /**
         * Build is blocked because another build is in progress,
         * required {@link Resource}s are not available, or otherwise blocked
         * by {@link Task#isBuildBlocked()}.
         * 
         * This flag is only used in {@link Queue#getItems()} for
         * 'pseudo' items that are actually not really in the queue.
         */
        @Exported
        public final boolean isBlocked;

        /**
         * Build is waiting the executor to become available.
         * This flag is only used in {@link Queue#getItems()} for
         * 'pseudo' items that are actually not really in the queue.
         */
        @Exported
        public final boolean isBuildable;

        public Item(Calendar timestamp, Task project) {
            this(timestamp,project,false,false);
        }

        public Item(Calendar timestamp, Task project, boolean isBlocked, boolean isBuildable) {
            this.timestamp = timestamp;
            this.task = project;
            this.isBlocked = isBlocked;
            this.isBuildable = isBuildable;
            synchronized(Queue.this) {
                this.id = iota++;
            }
        }

        /**
         * Gets a human-readable status message describing why it's in the queu.
         */
        @Exported
        public String getWhy() {
            if(isBuildable) {
                Label node = task.getAssignedLabel();
                Hudson hudson = Hudson.getInstance();
                if(hudson.getSlaves().isEmpty())
                    node = null;    // no master/slave. pointless to talk about nodes

                String name = null;
                if(node!=null) {
                    name = node.getName();
                    if(node.isOffline()) {
                        if(node.getNodes().size()>1)
                            return "All nodes of label '"+name+"' is offline";
                        else
                            return name+" is offline";
                    }
                }

                return "Waiting for next available executor"+(name==null?"":" on "+name);
            }

            if(isBlocked) {
                ResourceActivity r = getBlockingActivity(task);
                if(r!=null) {
                    if(r==task) // blocked by itself, meaning another build is in progress
                        return Messages.Queue_InProgress();
                    return Messages.Queue_BlockedBy(r.getDisplayName());
                }
                return task.getWhyBlocked();
            }

            long diff = timestamp.getTimeInMillis() - System.currentTimeMillis();
            if(diff>0) {
                return Messages.Queue_InQuietPeriod(Util.getTimeSpanString(diff));
            }

            return Messages.Queue_Unknown();
        }

        public boolean hasCancelPermission() {
            return task.hasAbortPermission();
        }

        public int compareTo(Item that) {
            int r = this.timestamp.getTime().compareTo(that.timestamp.getTime());
            if(r!=0)    return r;

            return this.id-that.id;
        }

    }

    /**
     * Unique number generator
     */
    private int iota=0;

    private static final Logger LOGGER = Logger.getLogger(Queue.class.getName());

    /**
     * Regularly invokes {@link Queue#maintain()} and clean itself up when
     * {@link Queue} gets GC-ed.
     */
    private static class MaintainTask extends SafeTimerTask {
        private final WeakReference<Queue> queue;

        MaintainTask(Queue queue) {
            this.queue = new WeakReference<Queue>(queue);

            long interval = 5 * Timer.ONE_SECOND;
            Trigger.timer.schedule(this, interval, interval);
        }

        protected void doRun() {
            Queue q = queue.get();
            if(q!=null)
                q.maintain();
            else
                cancel();
        }
    }
}
