package hudson.plugins.jwsdp_sqe;

/**
 * A {@link Test} is a set of {@link TestCase}s.
 *
 * <p>
 * And it's apparently also counted as a runnable test by itself.
 * Ugly design, if you ask me.
 *
 * @author Kohsuke Kawaguchi
 */
public class Test extends TestCollection<Test,TestCase> {
    public String getChildTitle() {
        return "Test Case";
    }

    public int getTotalCount() {
        return super.getTotalCount()+1;
    }

    public int getFailCount() {
        return super.getFailCount() + (getStatus()==Status.PASS ? 0 : 1);
    }
}
