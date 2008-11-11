package hudson.slaves;

import hudson.BulkChange;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.LoadStatistics;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Result;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.SleepBuilder;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

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
        super.tearDown();
        LoadStatistics.CLOCK = original;
    }

    /**
     * Scenario: schedule a build and see if one slave is provisioned.
     */
    public void testAutoProvision() throws Exception {
        BulkChange bc = new BulkChange(hudson);
        try {
            DummyCloudImpl cloud = initHudson();


            FreeStyleProject p = createJob(100);

            Future<FreeStyleBuild> f = p.scheduleBuild2(0);
            f.get(30, TimeUnit.SECONDS); // if it's taking too long, abort.

            // since there's only one job, we expect there to be just one slave
            assertEquals(1,cloud.numProvisioned);
        } finally {
            bc.abort();
        }
    }

    private FreeStyleProject createJob(int delay) throws IOException {
        FreeStyleProject p = createFreeStyleProject();
        p.setAssignedLabel(null);   // let it roam free, or else it ties itself to the master since we have no slaves
        p.getBuildersList().add(new SleepBuilder(delay));
        return p;
    }

    private DummyCloudImpl initHudson() throws IOException {
        // start a dummy service
        DummyCloudImpl cloud = new DummyCloudImpl(this);
        hudson.clouds.add(cloud);

        // no build on the master, to make sure we get everything from the cloud
        hudson.setNumExecutors(0);
        hudson.setNodes(Collections.<Node>emptyList());
        return cloud;
    }


    /**
     * Scenario: we got a lot of jobs all of the sudden, and we need to fire up a few nodes.
     */
    public void testLoadSpike() throws Exception {
        BulkChange bc = new BulkChange(hudson);
        try {
            DummyCloudImpl cloud = initHudson();

            List<FreeStyleProject> jobs = new ArrayList<FreeStyleProject>();
            for( int i=0; i<10; i++)
                jobs.add(createJob(3000)); //set a large delay, or else we'd only fire up 1 instance

            System.out.println("Scheduling a build");
            List<Future<FreeStyleBuild>> builds = new ArrayList<Future<FreeStyleBuild>>();
            for (FreeStyleProject job : jobs)
                builds.add(job.scheduleBuild2(0));

            System.out.println("Waiting for a completion");
            for (Future<FreeStyleBuild> f : builds) {
                FreeStyleBuild b = f.get();// if it's taking too long, abort.
                assertEquals(Result.SUCCESS,b.getResult());
            }

            // since there's only one job, we expect there to be just one slave
            assertTrue(1<cloud.numProvisioned);
        } finally {
            bc.abort();
        }
    }
}
