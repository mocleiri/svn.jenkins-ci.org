package hudson.tasks.junit;

import org.dom4j.Element;
import hudson.model.Build;

import java.util.Comparator;

/**
 * One test result.
 *
 * @author Kohsuke Kawaguchi
 */
public final class CaseResult extends TestObject implements Comparable<CaseResult> {
    private final String className;
    private final String testName;
    private final String errorStackTrace;
    private transient SuiteResult parent;

    /**
     * This test has been failing since this build number (not id.)
     *
     * If {@link #isPassed() passing}, this field is left unused to 0.
     */
    private /*final*/ int failedSince;

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

    /**
     * Gets the name of the test, which is returned from {@link TestCase#getName()}
     */
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

    public String getFullName() {
        return className+'.'+getName();
    }

    /**
     * If this test failed, then return the build number
     * when this test started failing.
     */
    public int getFailedSince() {
        return failedSince;
    }

    /**
     * Gets the number of consecutive builds (including this)
     * that this test case has been failing.
     */
    public int getAge() {
        if(isPassed())
            return 0;
        else
            return getOwner().getNumber()-failedSince+1;
    }

    @Override
    public CaseResult getPreviousResult() {
        SuiteResult pr = parent.getPreviousResult();
        if(pr==null)    return null;
        return pr.getCase(getName());
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

    public SuiteResult getParent() {
        return parent;
    }

    public Build getOwner() {
        return parent.getParent().getOwner();
    }

    public void freeze(SuiteResult parent) {
        this.parent = parent;
        if(!isPassed()) {
            CaseResult prev = getPreviousResult();
            if(prev!=null && !prev.isPassed())
                this.failedSince = prev.failedSince;
            else
                this.failedSince = getOwner().getNumber();
        }
    }

    public int compareTo(CaseResult that) {
        return this.getFullName().compareTo(that.getFullName());
    }

    public Status getStatus() {
        CaseResult pr = getPreviousResult();
        if(pr==null) {
            return isPassed() ? Status.PASSED : Status.FAILED;
        }

        if(pr.isPassed()) {
            return isPassed() ? Status.PASSED : Status.REGRESSION;
        } else {
            return isPassed() ? Status.FIXED : Status.FAILED;
        }
    }

    /**
     * Constants that represent the status of this test.
     */
    public enum Status {
        /**
         * This test runs OK, just like its previous run.
         */
        PASSED("result-passed","Passed",true),
        /**
         * This test failed, just like its previous run.
         */
        FAILED("result-failed","Failed",false),
        /**
         * This test has been failing, but now it runs OK.
         */
        FIXED("result-fixed","Fixed",true),
        /**
         * This test has been running OK, but now it failed.
         */
        REGRESSION("result-regression","Regression",false);

        private final String cssClass;
        private final String message;
        public final boolean isOK;

        Status(String cssClass, String message, boolean OK) {
           this.cssClass = cssClass;
           this.message = message;
           isOK = OK;
       }

        public String getCssClass() {
            return cssClass;
        }

        public String getMessage() {
            return message;
        }

        public boolean isRegression() {
            return this==REGRESSION;
        }
    }

    /**
     * For sorting errors by age.
     */
    /*package*/ static final Comparator<CaseResult> BY_AGE = new Comparator<CaseResult>() {
        public int compare(CaseResult lhs, CaseResult rhs) {
            return lhs.getAge()-rhs.getAge();
        }
    };
}
