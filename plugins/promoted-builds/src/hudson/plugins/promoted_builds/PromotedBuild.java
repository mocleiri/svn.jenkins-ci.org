package hudson.plugins.promoted_builds;

import hudson.model.Run;

import java.io.IOException;
import java.io.File;
import java.util.Calendar;

/**
 * @author Kohsuke Kawaguchi
 */
public class PromotedBuild extends Run<PromotedJob, PromotedBuild> {
    protected PromotedBuild(PromotedJob job) throws IOException {
        super(job);
    }

    protected PromotedBuild(PromotedJob job, Calendar timestamp) {
        super(job, timestamp);
    }

    protected PromotedBuild(PromotedJob project, File buildDir) throws IOException {
        super(project, buildDir);
    }
}
