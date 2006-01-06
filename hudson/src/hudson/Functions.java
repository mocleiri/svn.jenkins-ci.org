package hudson;

/**
 * @author Kohsuke Kawaguchi
 */
public class Functions {
    public static boolean containsJob(hudson.model.JobCollection jc,hudson.model.Job j) {
        return jc.containsJob(j);
    }
}
