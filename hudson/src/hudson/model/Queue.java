package hudson.model;

import hudson.util.OneShotEvent;

import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Build queue.
 *
 * @author Kohsuke Kawaguchi
 */
public class Queue {

    private static final Comparator<Item> itemComparator = new Comparator<Item>() {
        public int compare(Item lhs, Item rhs) {
            int r = lhs.timestamp.getTime().compareTo(rhs.timestamp.getTime());
            if(r!=0)    return r;

            return lhs.id-rhs.id;
        }
    };

    /**
     * Items in the queue ordered by {@link Item#timestamp}.
     *
     * <p>
     * This consists of {@link Item}s that cannot be run yet
     * because its time has not yet come.
     */
    private final Set<Item> queue = new TreeSet<Item>(itemComparator);

    /**
     * {@link Project}s that can be built immediately
     * but blocked because another build is in progress.
     */
    private final Set<Project> blockedProjects = new HashSet<Project>();

    /**
     * {@link Project}s that can be built immediately
     * that are waiting for available {@link Executor}.
     */
    private final List<Project> buildables = new LinkedList<Project>();

    private static class JobOffer {
        final Executor executor;

        /**
         * Used to wake up a thread.
         */
        final OneShotEvent event = new OneShotEvent();
        /**
         * The project that this {@link Executor} is going to build.
         * (Or null, in which case event is used to trigger a queue maintenance.)
         */
        Project project;

        public JobOffer(Executor executor) {
            this.executor = executor;
        }

        public void set(Project p) {
            this.project = p;
            event.signal();
        }

        public Node getNode() {
            return executor.getOwner().getNode();
        }
    }

    private final Map<Executor,JobOffer> parked = new HashMap<Executor,JobOffer>();

    /**
     * Schedule a new build for this project.
     */
    public synchronized void add( Project p ) {
        if(contains(p))
            return; // no double queueing

        // put the item in the queue
        Calendar due = new GregorianCalendar();
        due.add(Calendar.SECOND, p.getQuietPeriod());
        queue.add(new Item(due,p));

        scheduleMaintenance();   // let an executor know that a new item is in the queue.
    }

    public synchronized void cancel( Project p ) {
        for (Iterator itr = queue.iterator(); itr.hasNext();) {
            Item item = (Item) itr.next();
            if(item.project==p) {
                itr.remove();
                return;
            }
        }
        blockedProjects.remove(p);
        buildables.remove(p);
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
        for (Project p : blockedProjects) {
            r[idx++] = new Item(now, p);
        }
        for (Project p : buildables) {
            r[idx++] = new Item(now, p);
        }
        return r;
    }

    /**
     * Returns true if this queue contaisn the said project.
     */
    public synchronized boolean contains(Project p) {
        // if this project is already scheduled,
        // don't do anything
        if(blockedProjects.contains(p) || buildables.contains(p))
            return true;
        for (Item item : queue) {
            if (item.project == p)
                return true;
        }
        return false;
    }

    /**
     * Called by the executor to fetch something to build next.
     *
     * This method blocks until a next project becomes buildable.
     */
    public Project pop() throws InterruptedException {
        final Executor exec = Executor.currentExecutor();
        boolean successfulReturn = false;

        try {
            while(true) {
                final JobOffer offer = new JobOffer(exec);
                long sleep = -1;

                synchronized(this) {
                    // consider myself parked
                    assert !parked.containsKey(exec);
                    parked.put(exec,offer);

                    // reuse executor thread to do a queue maintainance.
                    // at the end of this we get all the buildable jobs
                    // in the buildables field.
                    maintain();

                    // allocate buildable jobs to executors
                    Iterator<Project> itr = buildables.iterator();
                    while(itr.hasNext()) {
                        Project p = itr.next();
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
                    // am I woken up because I have a project to build?
                    if(offer.project!=null) {
                        // if so, just build it
                        successfulReturn = true;
                        return offer.project;
                    }
                    // otherwise run a queue maintenance
                }
            }
        } finally {
            synchronized(this) {
                // remove myself from the parked list
                JobOffer offer = parked.get(exec);
                if(offer!=null) {
                    if(!successfulReturn && offer.project!=null) {
                        // we are already assigned a project,
                        // ask for someone else to build it.
                        // note that while this thread is waiting for CPU
                        // someone else can schedule this build again.
                        if(!contains(offer.project))
                            buildables.add(offer.project);
                    }

                    // since this executor might have been chosen for
                    // maintenance, schedule another one. Worst case
                    // we'll just run a pointless maintenance, and that's
                    // fine.
                    scheduleMaintenance();
                }
            }
        }
    }

    /**
     * Choses the executor to carry out the build for the given project.
     *
     * @return
     *      null if no {@link Executor} can run it.
     */
    private JobOffer choose(Project p) {
        Node n = p.getAssignedNode();
        if(n!=null) {
            // if a project has assigned node, it can be only built on it
            for (JobOffer offer : parked.values()) {
                if(offer.project==null && offer.getNode()==n)
                    return offer;
            }
            return null;
        }

        // otherwise let's see if the last node that this project was built is available
        // it has up-to-date workspace, so that's usually preferable
        n = p.getLastBuiltOn();
        if(n!=null) {
            for (JobOffer offer : parked.values()) {
                if(offer.project==null && offer.getNode()==n)
                    return offer;
            }
        }

        // duration of a build on a slave tends not to have an impact on
        // the master/slave communication, so that means we should favor
        // running long jobs on slaves.
        Build succ = p.getLastSuccessfulBuild();
        if(succ!=null && succ.getDuration()>15*60*1000) {
            // consider a long job to be > 15 mins
            for (JobOffer offer : parked.values()) {
                if(offer.project==null && offer.getNode() instanceof Slave)
                    return offer;
            }
        }

        // lastly, just look for any idle executor
        for (JobOffer offer : parked.values()) {
            if(offer.project==null)
                return offer;
        }

        // nothing available
        return null;
    }

    /**
     * Wakes up one {@link Executor} so that it will maintain a queue.
     */
    private synchronized void scheduleMaintenance() {
        // this code assumes that after this method is called
        // no more executors will be offered job except by
        // the pop() code.
        for (Entry<Executor, JobOffer> av : parked.entrySet()) {
            if(av.getValue().project==null) {
                av.getValue().event.signal();
                return;
            }
        }
    }


    /**
     * Queue maintainance.
     *
     * Move projects between {@link #queue}, {@link #blockedProjects}, and {@link #buildables}
     * appropriately.
     */
    private synchronized void maintain() {
        for (Project p : blockedProjects) {
            Build lastBuild = p.getLastBuild();
            if (lastBuild == null || !lastBuild.isBuilding()) {
                // ready to be executed
                blockedProjects.remove(p);
                buildables.add(p);
            }
        }

        while(!queue.isEmpty()) {
            Item top = peek();

            if(!top.timestamp.before(new GregorianCalendar()))
                return; // finished moving all ready items from queue

            Build lastBuild = top.project.getLastBuild();
            if(lastBuild==null || !lastBuild.isBuilding()) {
                // ready to be executed immediately
                queue.remove(top);
                buildables.add(top.project);
            }

            // this can't be built know because another build is in progress
            // set this project aside.
            queue.remove(top);
            blockedProjects.add(top.project);
        }
    }

    /**
     * Item in a queue.
     */
    public class Item {
        /**
         * This item can be run after this time.
         */
        final Calendar timestamp;

        /**
         * Project to be built.
         */
        final Project project;

        /**
         * Unique number of this {@link Item}.
         * Used to differenciate {@link Item}s with the same due date.
         */
        final int id;

        public Item(Calendar timestamp, Project project) {
            this.timestamp = timestamp;
            this.project = project;
            synchronized(Queue.this) {
                this.id = iota++;
            }
        }

        public Calendar getTimestamp() {
            return timestamp;
        }

        public Project getProject() {
            return project;
        }

        public int getId() {
            return id;
        }
    }

    /**
     * Unique number generator
     */
    private int iota=0;
}
