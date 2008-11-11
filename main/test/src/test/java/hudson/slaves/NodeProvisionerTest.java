package hudson.slaves;

import hudson.BulkChange;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.LoadStatistics;
import hudson.model.Node;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.SleepBuilder;

import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Kohsuke Kawaguchi
 */
public class NodeProvisionerTest extends HudsonTestCase {

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
            DummyCloudImpl cloud = new DummyCloudImpl(this);
            hudson.clouds.add(cloud);

            // no build on the master, to make sure we get everything from the cloud
            hudson.setNumExecutors(0);
            hudson.setNodes(Collections.<Node>emptyList());

            FreeStyleProject p = createFreeStyleProject();
            p.setAssignedLabel(null);   // let it roam free, or else it ties itself to the master since we have no slaves
            p.getBuildersList().add(new SleepBuilder(100));

            Future<FreeStyleBuild> f = p.scheduleBuild2(0);
            f.get(30, TimeUnit.SECONDS); // if it's taking too long, abort.

            // since there's only one job, we expect there to be just one slave
            assertEquals(1,cloud.numProvisioned);
        } finally {
            bc.abort();
        }
    }

}
