package hudson.model;

import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapper.Environment;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.triggers.SCMTrigger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Calendar;

import org.kohsuke.stapler.StaplerRequest;

/**
 * A build of a {@link Project}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Build extends AbstractBuild<Project,Build> {

    /**
     * Creates a new build.
     */
    Build(Project project) throws IOException {
        super(project);
    }

    /**
     * Loads a build from a log file.
     */
    Build(Project project, File buildDir) throws IOException {
        super(project,buildDir);
    }
    
    /**
     * During the build this field remembers {@link Environment}s created by
     * {@link BuildWrapper}. This design is bit ugly but forced due to compatibility.
     */
    private transient List<Environment> buildEnvironments;

    @Override
    protected void onStartBuilding() {
        SCMTrigger t = (SCMTrigger)project.getTriggers().get(SCMTrigger.DESCRIPTOR);
        if(t==null) {
            super.onStartBuilding();
        } else {
            synchronized(t) {
                try {
                    t.abort();
                } catch (InterruptedException e) {
                    // handle the interrupt later
                    Thread.currentThread().interrupt();
                }
                super.onStartBuilding();
            }
        }
    }

    @Override
    protected void onEndBuilding() {
        SCMTrigger t = (SCMTrigger)project.getTriggers().get(SCMTrigger.DESCRIPTOR);
        if(t==null) {
            super.onEndBuilding();
        } else {
            synchronized(t) {
                super.onEndBuilding();
                t.startPolling();
            }
        }
    }

    @Override
    public Map<String,String> getEnvVars() {
        Map<String,String> env = super.getEnvVars();

        if(buildEnvironments!=null) {
            for (Environment e : buildEnvironments)
                e.buildEnvVars(env);
        }

        return env;
    }

    public Api getApi(final StaplerRequest req) {
        // TODO: think of a way to compose this to push some of this up
        // the inheritance tree.
        class build {
            public int number = getNumber();
            public Calendar timestamp = getTimestamp();
            public String builtOn = getBuiltOnStr();
            public Result result = getResult();
            public long duration = getDuration();
        }
        return new Api(new build());
    }

//
//
// actions
//
//
    @Override
    public void run() {
        run(new RunnerImpl());
    }
    
    private class RunnerImpl extends AbstractRunner {
        protected Result doRun(BuildListener listener) throws Exception {
            if(!preBuild(listener,project.getBuilders()))
                return Result.FAILURE;
            if(!preBuild(listener,project.getPublishers()))
                return Result.FAILURE;

            buildEnvironments = new ArrayList<Environment>();
            try {
                for( BuildWrapper w : project.getBuildWrappers().values() ) {
                    Environment e = w.setUp(Build.this, launcher, listener);
                    if(e==null)
                        return Result.FAILURE;
                    buildEnvironments.add(e);
                }


                if(!build(listener,project.getBuilders()))
                    return Result.FAILURE;
            } finally {
                // tear down in reverse order
                for( int i=buildEnvironments.size()-1; i>=0; i-- )
                    buildEnvironments.get(i).tearDown(Build.this,listener);
                buildEnvironments = null;
            }

            return null;
        }

        public void post(BuildListener listener) {
            // run all of them even if one of them failed
            try {
                for( Publisher bs : project.getPublishers().values() )
                    bs.perform(Build.this, launcher, listener);
            } catch (InterruptedException e) {
                e.printStackTrace(listener.fatalError("aborted"));
                setResult(Result.FAILURE);
            } catch (IOException e) {
                e.printStackTrace(listener.fatalError("failed"));
                setResult(Result.FAILURE);
            }
        }

        private boolean build(BuildListener listener, Map<?, Builder> steps) throws IOException, InterruptedException {
            for( Builder bs : steps.values() )
                if(!bs.perform(Build.this, launcher, listener))
                    return false;
            return true;
        }

        private boolean preBuild(BuildListener listener,Map<?,? extends BuildStep> steps) {
            for( BuildStep bs : steps.values() )
                if(!bs.prebuild(Build.this,listener))
                    return false;
            return true;
        }
    }
}
