package hudson.model;

import java.io.IOException;
import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class ViewRun<JobT extends ViewJob<JobT,RunT>, RunT extends ViewRun<JobT,RunT>>
    extends Run<JobT,RunT> {

    protected ViewRun(JobT job) throws IOException {
        super(job);
    }

    protected ViewRun(JobT project, File buildDir, RunT prevBuild) throws IOException {
        super(project, buildDir, prevBuild);
    }
}
