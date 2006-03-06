package hudson.tasks.junit;

import org.dom4j.io.SAXReader;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.DocumentException;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

/**
 * Result of one test suite.
 *
 * <p>
 * The notion of "test suite" is rather arbitrary in JUnit ant task.
 * It's basically one invocation of junit.
 *
 * @author Kohsuke Kawaguchi
 */
public final class SuiteResult {
    private final String name;
    private final String stdout;
    private final String stderr;

    /**
     * Test cases.
     */
    private final List<CaseResult> cases = new ArrayList<CaseResult>();
    private transient TestResult owner;

    SuiteResult(File xmlReport) throws DocumentException {
        Document result = new SAXReader().read(xmlReport);
        Element root = result.getRootElement();
        name = root.attributeValue("name");

        stdout = root.elementText("system-out");
        stderr = root.elementText("system-err");

        for (Element e : (List<Element>)root.elements("testcase")) {
            cases.add(new CaseResult(e));
        }
    }

    public String getName() {
        return name;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public TestResult getOwner() {
        return owner;
    }

    public List<CaseResult> getCases() {
        return cases;
    }

    /**
     * Returns the {@link CaseResult} whose {@link CaseResult#getTestName()}
     * is the same as the given string.
     *
     * <p>
     * Note that test name needs not be unique.
     */
    public CaseResult getCase(String name) {
        for (CaseResult c : cases) {
            if(c.getTestName().equals(name))
                return c;
        }
        return null;
    }

    public void freeze(TestResult owner) {
        this.owner = owner;
        for (CaseResult c : cases)
            c.freeze(this);
    }
}
