package hudson;

import hudson.model.ModelObject;

/**
 * @author Kohsuke Kawaguchi
 */
public class Functions {
    public static boolean containsJob(hudson.model.JobCollection jc,hudson.model.Job j) {
        return jc.containsJob(j);
    }

    public static boolean isModel(Object o) {
        return o instanceof ModelObject;
    }
}
