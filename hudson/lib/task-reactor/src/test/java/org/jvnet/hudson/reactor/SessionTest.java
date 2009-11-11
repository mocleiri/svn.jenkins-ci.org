package org.jvnet.hudson.reactor;

import junit.framework.TestCase;
import org.objectweb.carol.cmi.test.TeeWriter;

import javax.naming.NamingException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.AbstractList;
import java.util.concurrent.Executors;

/**
 * @author Kohsuke Kawaguchi
 */
public class SessionTest extends TestCase {
    /**
     * Makes sure the ordering happens.
     */
    public void testSequentialOrdering() throws Exception {
        Session s = buildSession("->t1->m1 m1->t2->m2 m2->t3->",new TestTask() {
            public void run(String id) throws Exception {
                System.out.println(id);
            }
        });
        assertEquals(3,s.size());

        String sw = execute(s);

        assertEquals("Started t1\nEnded t1\nAttained m1\nStarted t2\nEnded t2\nAttained m2\nStarted t3\nEnded t3\n", sw);
    }

    private String execute(Session s) throws Exception {
        StringWriter sw = new StringWriter();
        System.out.println("----");
        final PrintWriter w = new PrintWriter(new TeeWriter(sw,new OutputStreamWriter(System.out)),true);

        s.execute(Executors.newCachedThreadPool(),new SessionListener() {
            public synchronized void onTaskStarted(Task t) {
                w.println("Started "+t.getDisplayName());
            }

            public synchronized void onTaskCompleted(Task t) {
                w.println("Ended "+t.getDisplayName());
            }

            public synchronized void onTaskFailed(Task t, Throwable err) {
                w.println("Failed "+t.getDisplayName()+" with "+err);
            }

            public synchronized void onAttained(Milestone milestone) {
                w.println("Attained "+milestone);
            }
        });
        return sw.toString();
    }

    /**
     * Makes sure tasks can be executed in parallel.
     */
    public void testConcurrentExecution() throws Exception {
        execute(buildSession("->t1-> ->t2->", createLatch(2)));
    }

    /**
     * Scheduling of downstream jobs go through slightly different path, so test that too.
     */
    public void testConcurrentExecution2() throws Exception {
        execute(buildSession("->t1->m m->t2-> m->t3->", new TestTask() {
            TestTask latch = createLatch(2);
            public void run(String id) throws Exception {
                if (id.equals("t1"))    return;
                latch.run(id);
            }
        }));
    }

    /**
     * Is the exception properly forwarded?
     */
    public void testFailure() throws Exception {
        final Exception[] e = new Exception[1];
        try {
            execute(buildSession("->t1->", new TestTask() {
                public void run(String id) throws Exception {
                    throw e[0]=new NamingException("Yep");
                }
            }));
            fail();
        } catch (ReactorException x) {
            assertSame(e[0],x.getCause());
        }
    }

    /**
     * Creates {@link TestTask} that waits for multiple tasks to be blocked together.
     */
    private TestTask createLatch(final int threshold) {
        return new TestTask() {
            final Object lock = new Object();
            int pending = 0;
            boolean go = false;

            public void run(String id) throws InterruptedException {
                synchronized (lock) {
                    pending++;
                    if (pending==threshold) {
                        // make sure two of us execute at the same time
                        go = true;
                        lock.notifyAll();
                    }

                    while (!go)
                        lock.wait();
                }
            }
        };
    }

    interface TestTask {
        void run(String id) throws Exception;
    }

    private Session buildSession(String spec, final TestTask work) {
        class TaskImpl implements Task {
            final String id;
            final Collection<Milestone> requires;
            final Collection<Milestone> attains;

            TaskImpl(String id) {
                String[] tokens = id.split("->");
                this.id = tokens[1];
                // tricky handling necessary due to inconsistency in how split works
                this.requires = adapt(tokens[0].length()==0 ? Collections.<String>emptyList() : Arrays.asList(tokens[0].split(",")));
                this.attains = adapt(tokens.length<3 ? Collections.<String>emptyList() : Arrays.asList(tokens[2].split(",")));
            }

            private Collection<Milestone> adapt(List<String> strings) {
                List<Milestone> r = new ArrayList<Milestone>();
                for (String s : strings)
                    r.add(new MilestoneImpl(s));
                return r;
            }

            public Collection<Milestone> requires() {
                return requires;
            }

            public Collection<Milestone> attains() {
                return attains;
            }

            public String getDisplayName() {
                return id;
            }

            public void run() throws Exception {
                work.run(id);
            }
        }

        Collection<TaskImpl> tasks = new ArrayList<TaskImpl>();
        for (String node : spec.split(" "))
            tasks.add(new TaskImpl(node));

        return Session.fromTasks(tasks);
    }

    private static class MilestoneImpl implements Milestone {
        private final String id;

        private MilestoneImpl(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MilestoneImpl milestone = (MilestoneImpl) o;
            return id.equals(milestone.id);

        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
