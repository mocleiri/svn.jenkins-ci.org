package hudson.model;

import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;

/**
 * @author Kohsuke Kawaguchi
 */
public class Queue {

    private final Comparator itemComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            Item lhs = (Item) o1;
            Item rhs = (Item) o2;

            int r = lhs.timestamp.getTime().compareTo(rhs.timestamp.getTime());
            if(r!=0)    return r;

            return lhs.id-rhs.id;
        }
    };

    /**
     * Items in the queue ordered by {@link Item#timestamp}.
     */
    private final Set items = new TreeSet(itemComparator);

    /**
     * {@link Project}s that can be built immediately
     * but blocked because another build is in progress.
     */
    private final Set blockedProjects = new HashSet();

    /**
     * Schedule a new build for this project.
     */
    public synchronized void add( Project p ) {
        // if this project is already scheduled,
        // don't do anything
        if(blockedProjects.contains(p))
            return;
        for (Iterator itr = items.iterator(); itr.hasNext();) {
            Item item = (Item) itr.next();
            if(item.project==p)
                return;
        }

        // put the item in the queue
        Calendar due = new GregorianCalendar();
        due.add(Calendar.SECOND,5);
        items.add(new Item(due,p));

        notify();   // let the thread know that a new item is in the queue.
    }

    public synchronized void cancel( Project p ) {
        for (Iterator itr = items.iterator(); itr.hasNext();) {
            Item item = (Item) itr.next();
            if(item.project==p) {
                itr.remove();
                return;
            }
        }
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    private synchronized Item peek() {
        return (Item)items.iterator().next();
    }

    /**
     * Gets a snapshot of items in the queue.
     */
    public synchronized Item[] getItems() {
        Item[] r = new Item[items.size()+blockedProjects.size()];
        items.toArray(r);
        int idx=items.size();
        Calendar now = new GregorianCalendar();
        for (Iterator itr = blockedProjects.iterator(); itr.hasNext();) {
            Project p = (Project) itr.next();
            r[idx++] = new Item(now,p);
        }
        return r;
    }

    /**
     * Called by the executor to fetch something to build next.
     */
    public synchronized Project pop() throws InterruptedException {
        outer:
        while(true) {
            for (Iterator itr = blockedProjects.iterator(); itr.hasNext();) {
                Project p = (Project) itr.next();
                Build lastBuild = p.getLastBuild();
                if(lastBuild==null || !lastBuild.isBuilding()) {
                    // ready to be executed
                    blockedProjects.remove(p);
                    return p;
                }
            }

            if(isEmpty()) {
                // if nothing is in the queue, sleep until we've got something.
                wait();
            } else {
                // otherwise wait until the first item in the queue is due.
                long diff = peek().timestamp.getTimeInMillis()-new GregorianCalendar().getTimeInMillis();
                if(diff<100)    diff=100;    // avoid wait(0)
                wait(diff);
            }

            Item top;

            while(!isEmpty()) {
                top = peek();

                if(!top.timestamp.before(new GregorianCalendar()))
                    continue outer; // not ready

                Build lastBuild = top.project.getLastBuild();
                if(lastBuild==null || !lastBuild.isBuilding()) {
                    // ready to be executed
                    items.remove(top);
                    return top.project;
                }

                // set this project aside.
                blockedProjects.add(top.project);
                items.remove(top);
            }
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
