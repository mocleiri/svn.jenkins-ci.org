package hudson.plugins.jwsdp_sqe;

import hudson.model.Build;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * A {@link Test} is a set of {@link TestCase}s.
 *
 * @author Kohsuke Kawaguchi
 */
public class Test extends TestCollection<Test,TestCase> {
}
