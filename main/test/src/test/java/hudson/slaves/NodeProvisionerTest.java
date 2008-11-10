package hudson.slaves;

import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.LoadStatistics;
import hudson.model.Node.Mode;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.BulkChange;
import hudson.remoting.Which;
import hudson.remoting.Channel;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.SleepBuilder;
import org.jvnet.hudson.test.TestEnvironment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Kohsuke Kawaguchi
 */
public class NodeProvisionerTest extends HudsonTestCase {
    private final AtomicInteger iota = new AtomicInteger();
    private final class Sleeper implements Callable {
        private final long time;

        private Sleeper(long time) {
            this.time = time;
        }

        public Object call() throws Exception {
            Thread.sleep(time);
            System.out.println("launching slave");
            DumbSlave slave = new DumbSlave("idiot" + iota.incrementAndGet(), "dummy",
                    createTmpDir().getPath(), "1", Mode.NORMAL, "java -jar "+ Which.jarFile(Channel.class), new CommandLauncher(""), RetentionStrategy.NOOP);
            hudson.addNode(slave);
            return slave.toComputer().connect().get();
        }
    }

    private int original;

    @Override
    protected void setUp() throws Exception {
        original = LoadStatistics.CLOCK;
        LoadStatistics.CLOCK = 10; // 10ms
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        LoadStatistics.CLOCK = original;
    }

    public void testAutoProvision() throws Exception {
        BulkChange bc = new BulkChange(hudson);
        try {
            // start a dummy service
            hudson.clouds.add(new Cloud("test") {
                public Collection<PlannedNode> provision(float excessWorkload) {
                    List<PlannedNode> r = new ArrayList<PlannedNode>();
                    while(excessWorkload>0) {
                        System.out.println("Provisioning");
                        Future f = Computer.threadPoolForRemoting.submit(new Sleeper(300));
                        r.add(new PlannedNode("test",f,1));
                        excessWorkload-=1;
                    }
                    return r;
                }

                public Descriptor<Cloud> getDescriptor() {
                    throw new UnsupportedOperationException();
                }
            });

            // no build on the master
            hudson.setNumExecutors(0);
            hudson.setNodes(Collections.<Node>emptyList());

            FreeStyleProject p = createFreeStyleProject();
            p.setAssignedLabel(null);   // let it roam free, or else it ties itself to the master since we have no slaves
            p.getBuildersList().add(new SleepBuilder(100));

            p.scheduleBuild2(0).get();
        } finally {
            bc.abort();
        }
    }
}
