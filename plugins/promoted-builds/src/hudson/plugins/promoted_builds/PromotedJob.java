package hudson.plugins.promoted_builds;

import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.JobDescriptor;
import hudson.model.RunMap;

import java.util.SortedMap;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class PromotedJob extends Job<PromotedJob, PromotedBuild> {

    /**
     * All the promoted builds keyed by their build number.
     */
    private transient final RunMap<PromotedBuild> builds = new RunMap<PromotedBuild>();

    public PromotedJob(Hudson parent, String name) {
        super(parent, name);
    }

    public boolean isBuildable() {
        // I guess it doesn't make sense to map a "build" to the promotion,
        // as you rarely want to promote the last CI build.
        return false;
    }

    protected SortedMap<Integer, PromotedBuild> _getRuns() {
        return builds.getView();
    }

    protected void removeRun(PromotedBuild run) {
        builds.remove(run);
    }

    public Descriptor<Job<PromotedJob, PromotedBuild>> getDescriptor() {
        return DESCRIPTOR;
    }

    static final JobDescriptor<PromotedJob, PromotedBuild> DESCRIPTOR = new JobDescriptor<PromotedJob, PromotedBuild>(PromotedJob.class) {
        public String getDisplayName() {
            return "Managing promotion of another job";
        }

        public PromotedJob newInstance(String name) {
            return new PromotedJob(Hudson.getInstance(),name);
        }
    };

    static {
        Job.XSTREAM.alias("promotedJob",PromotedJob.class);
    }

    private static final Logger logger = Logger.getLogger(PromotedJob.class.getName());
}
