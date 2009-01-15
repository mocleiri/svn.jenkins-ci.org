package hudson.slaves;

import hudson.BulkChange;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.LoadStatistics;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Label;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.SleepBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
        LoadStatistics.CLOCK = 10; // run x1000 the regular speed to speed up the test
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

            verifySuccessfulCompletion(buildAll(create5SlowJobs()));

            // the time it takes to complete a job is eternally long compared to the time it takes to launch
            // a new slave, so in this scenario we end up allocating 5 slaves for 5 jobs.
            assertEquals(5,cloud.numProvisioned);
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
            createSlave().toComputer().connect(false).get();
            createSlave().toComputer().connect(false).get();

            verifySuccessfulCompletion(buildAll(create5SlowJobs()));

            // we should have used two static slaves, thus only 3 slaves should have been provisioned
            assertEquals(3,cloud.numProvisioned);
        } finally {
            bc.abort();
        }
    }

    /**
     * Scenario: loads on one label shouldn't translate to load on another label.
     */
    public void testLabels() throws Exception {
        BulkChange bc = new BulkChange(hudson);
        try {
            DummyCloudImpl cloud = initHudson(0);
            Label blue = hudson.getLabel("blue");
            Label red = hudson.getLabel("red");
            cloud.label = red;

            // red jobs
            List<FreeStyleProject> redJobs = create5SlowJobs();
            for (FreeStyleProject p : redJobs)
                p.setAssignedLabel(red);

            // blue jobs
            List<FreeStyleProject> blueJobs = create5SlowJobs();
            for (FreeStyleProject p : blueJobs)
                p.setAssignedLabel(blue);

            // build all
            List<Future<FreeStyleBuild>> blueBuilds = buildAll(blueJobs);
            verifySuccessfulCompletion(buildAll(redJobs));

            // cloud should only give us 5 nodes for 5 red jobs
            assertEquals(5,cloud.numProvisioned);

            // and all blue jobs should be still stuck in the queue
            for (Future<FreeStyleBuild> bb : blueBuilds)
                assertFalse(bb.isDone());
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

    private List<FreeStyleProject> create5SlowJobs() throws IOException {
        List<FreeStyleProject> jobs = new ArrayList<FreeStyleProject>();
        for( int i=0; i<5; i++)
            //set a large delay, to simulate the situation where we need to provision more slaves
            // to keep up with the load
            jobs.add(createJob(3000));
        return jobs;
    }

    /**
     * Builds all the given projects at once.
     */
    private List<Future<FreeStyleBuild>> buildAll(List<FreeStyleProject> jobs) {
        System.out.println("Scheduling builds for "+jobs.size()+" jobs");
        List<Future<FreeStyleBuild>> builds = new ArrayList<Future<FreeStyleBuild>>();
        for (FreeStyleProject job : jobs)
            builds.add(job.scheduleBuild2(0));
        return builds;
    }

    private void verifySuccessfulCompletion(List<Future<FreeStyleBuild>> builds) throws Exception {
        System.out.println("Waiting for a completion");
        for (Future<FreeStyleBuild> f : builds) {
            FreeStyleBuild b = f.get();// if it's taking too long, abort.
            assertBuildStatus(Result.SUCCESS,b);
        }
    }
}
