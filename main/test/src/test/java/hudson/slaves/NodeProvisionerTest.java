package hudson.slaves;

import hudson.BulkChange;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.LoadStatistics;
import hudson.model.Node;
import hudson.model.Result;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.SleepBuilder;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
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
            DummyCloudImpl cloud = initHudson(10);


            FreeStyleProject p = createJob(10);

            Future<FreeStyleBuild> f = p.scheduleBuild2(0);
            f.get(30, TimeUnit.SECONDS); // if it's taking too long, abort.

            // since there's only one job, we expect there to be just one slave
            assertEquals(1,cloud.numProvisioned);
        } finally {
            bc.abort();
        }
    }

    /**
     * Scenario: we got a lot of jobs all of the sudden, and we need to fire up a few nodes.
     */
    public void testLoadSpike() throws Exception {
        BulkChange bc = new BulkChange(hudson);
        try {
            DummyCloudImpl cloud = initHudson(0);

            verifySuccessfulCompletion(buildAll(create10SlowJobs()));

            // the time it takes to complete a job is eternally long compared to the time it takes to launch
            // a new slave, so in this scenario we end up allocating 10 slaves for 10 jobs.
            assertEquals(10,cloud.numProvisioned);
        } finally {
            bc.abort();
        }
    }

    /**
     * Scenario: make sure we take advantage of statically configured slaves.
     */
    public void testBaselineSlaveUsage() throws Exception {
        BulkChange bc = new BulkChange(hudson);
        try {
            DummyCloudImpl cloud = initHudson(0);
            // add slaves statically upfront
            createSlave();
            createSlave();

            verifySuccessfulCompletion(buildAll(create10SlowJobs()));

            // we should have used two static slaves, thus only 8 slaves should have been provisioned
            assertEquals(8,cloud.numProvisioned);
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

    private DummyCloudImpl initHudson(int delay) throws IOException {
        // start a dummy service
        DummyCloudImpl cloud = new DummyCloudImpl(this, delay);
        hudson.clouds.add(cloud);

        // no build on the master, to make sure we get everything from the cloud
        hudson.setNumExecutors(0);
        hudson.setNodes(Collections.<Node>emptyList());
        return cloud;
    }

    private List<FreeStyleProject> create10SlowJobs() throws IOException {
        List<FreeStyleProject> jobs = new ArrayList<FreeStyleProject>();
        for( int i=0; i<10; i++)
            //set a large delay, to simulate the situation where we need to provision more slaves
            // to keep up with the load
            jobs.add(createJob(3000));
        return jobs;
    }

    /**
     * Builds all the given projects at once.
     */
    private List<Future<FreeStyleBuild>> buildAll(List<FreeStyleProject> jobs) {
        System.out.println("Scheduling a build");
        List<Future<FreeStyleBuild>> builds = new ArrayList<Future<FreeStyleBuild>>();
        for (FreeStyleProject job : jobs)
            builds.add(job.scheduleBuild2(0));
        return builds;
    }

    private void verifySuccessfulCompletion(List<Future<FreeStyleBuild>> builds) throws InterruptedException, ExecutionException {
        System.out.println("Waiting for a completion");
        for (Future<FreeStyleBuild> f : builds) {
            FreeStyleBuild b = f.get();// if it's taking too long, abort.
            assertEquals(Result.SUCCESS,b.getResult());
        }
    }
}
