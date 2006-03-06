package hudson.tasks.junit;

import hudson.model.BuildListener;
import hudson.model.Action;
import hudson.model.Build;
import org.apache.tools.ant.DirectoryScanner;
import org.dom4j.DocumentException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Root of all the test results for one build.
 *
 * @author Kohsuke Kawaguchi
 */
public final class TestResult implements Action {
    private final List<SuiteResult> suites = new ArrayList<SuiteResult>();

    /**
     * {@link #suites} keyed by their names for faster lookup.
     */
    private transient Map<String,SuiteResult> suitesByName;

    private final Build owner;

    /**
     * Number of all tests.
     */
    private transient int totalTests;
    /**
     * Number of failed/error tests.
     */
    private transient List<CaseResult> failedTests;

    TestResult(Build owner, DirectoryScanner results, BuildListener listener) {
        this.owner = owner;
        String[] includedFiles = results.getIncludedFiles();
        File baseDir = results.getBasedir();

        long buildTime = owner.getTimestamp().getTimeInMillis();

        for (String value : includedFiles) {
            try {
                File reportFile = new File(baseDir, value);
                if(buildTime <= reportFile.lastModified())
                    // only count files that were actually updated during this build
                    suites.add(new SuiteResult(reportFile));
            } catch (DocumentException e) {
                e.printStackTrace(listener.error(e.getMessage()));
            }
        }
        freeze();
    }

    public String getUrlName() {
        return "testReport";
    }

    public String getDisplayName() {
        return "Test Result";
    }

    public String getIconFileName() {
        return "clipboard.gif";
    }

    public Build getOwner() {
        return owner;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public List<CaseResult> getFailedTests() {
        return failedTests;
    }

    public SuiteResult getSuite(String name) {
        return suitesByName.get(name);
    }

    private Object readResolve() {
        // call freeze when we were restored from disk
        freeze();
        return this;
    }

    /**
     * Builds up the transient part of the data structure.
     */
    void freeze() {
        suitesByName = new HashMap<String,SuiteResult>();
        totalTests = 0;
        failedTests = new ArrayList<CaseResult>();
        for (SuiteResult s : suites) {
            s.freeze(this);

            suitesByName.put(s.getName(),s);

            totalTests += s.getCases().size();
            for(CaseResult cr : s.getCases()) {
                if(!cr.isPassed())
                    failedTests.add(cr);
            }
        }
    }
}
