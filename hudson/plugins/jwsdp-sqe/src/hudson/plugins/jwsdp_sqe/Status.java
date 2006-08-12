package hudson.plugins.jwsdp_sqe;

/**
 * @author Kohsuke Kawaguchi
 */
public enum Status {
    /**
     * Test ran successfully.
     */
    PASS,
    /**
     * Test ran but failed.
     */
    FAIL,
    /**
     * Test didn't run.
     */
    SKIP
}
