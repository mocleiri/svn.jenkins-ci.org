package hudson.tasks.junit;

import org.dom4j.Element;
import hudson.model.Build;
import hudson.model.ModelObject;

/**
 * One test result.
 *
 * @author Kohsuke Kawaguchi
 */
public final class CaseResult implements ModelObject, Comparable<CaseResult> {
    private final String className;
    private final String testName;
    private final String errorStackTrace;
    private transient SuiteResult owner;

    CaseResult(Element testCase) {
        className = testCase.attributeValue("classname");
        testName = testCase.attributeValue("name");
        errorStackTrace = getError(testCase);
    }

    private String getError(Element testCase) {
        String msg = testCase.elementText("error");
        if(msg!=null)
            return msg;
        return testCase.elementText("failure");
    }

    public String getDisplayName() {
        return testName;
    }

    public String getName() {
        return testName;
    }

    /**
     * Gets the class name of a test class.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the simple (not qualified) class name.
     */
    public String getSimpleName() {
        int idx = className.lastIndexOf('.');
        return className.substring(idx+1);
    }

    /**
     * Gets the package name of a test case
     */
    public String getPackageName() {
        int idx = className.lastIndexOf('.');
        if(idx<0)       return "(root)";
        else            return className.substring(0,idx);
    }

    /**
     * Gets the name of the test, which is returned from {@link TestCase#getName()}
     */
    public String getTestName() {
        return testName;
    }

    public String getFullName() {
        return className+'.'+getTestName();
    }

    /**
     * If there was an error or a failure, this is the stack trace, or otherwise null.
     */
    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    public boolean isPassed() {
        return errorStackTrace==null;
    }

    public SuiteResult getOwner() {
        return owner;
    }

    public Build getOwnerBuild() {
        return owner.getOwner().getOwner();
    }

    public void freeze(SuiteResult owner) {
        this.owner = owner;
    }

    public int compareTo(CaseResult that) {
        return this.getFullName().compareTo(that.getFullName());
    }
}
