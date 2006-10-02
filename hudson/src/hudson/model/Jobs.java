package hudson.model;

import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class Jobs {
    /**
     * List of all installed job types.
     */
    public static final List<Descriptor> JOBS = (List)Descriptor.toList(
        Project.DESCRIPTOR,
        ExternalJob.DESCRIPTOR
    );
}
