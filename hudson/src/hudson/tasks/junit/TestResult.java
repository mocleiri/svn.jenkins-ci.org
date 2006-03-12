package hudson.tasks.junit;

import hudson.model.Build;
import hudson.model.BuildListener;
import org.apache.tools.ant.DirectoryScanner;
import org.dom4j.DocumentException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Root of all the test results for one build.
 *
 * @author Kohsuke Kawaguchi
 */
public final class TestResult extends MetaTabulatedResult {
    /**
     * List of all {@link SuiteResult}s in this test.
     * This is the core data structure to be persisted in the disk.
     */
    private final List<SuiteResult> suites = new ArrayList<SuiteResult>();

    /**
     * {@link #suites} keyed by their names for faster lookup.
     */
    private transient Map<String,SuiteResult> suitesByName;

    /**
     * Results tabulated by package.
     */
    private transient Map<String,PackageResult> byPackages;

    /*package*/ transient TestResultAction parent;

    /**
     * Number of all tests.
     */
    private transient int totalTests;
    /**
     * Number of failed/error tests.
     */
    private transient List<CaseResult> failedTests;

    /**
     * Creates an empty result.
     */
    TestResult() {
        freeze();
    }

    TestResult(TestResultAction parent, DirectoryScanner results, BuildListener listener) {
        this.parent = parent;
        String[] includedFiles = results.getIncludedFiles();
        File baseDir = results.getBasedir();

        long buildTime = parent.owner.getTimestamp().getTimeInMillis();

        for (String value : includedFiles) {
            File reportFile = new File(baseDir, value);
            try {
                if(buildTime <= reportFile.lastModified())
                    // only count files that were actually updated during this build
                    suites.add(new SuiteResult(reportFile));
            } catch (DocumentException e) {
                e.printStackTrace(listener.error("Failed to read "+reportFile));
            }
        }

        freeze();
    }

    public String getDisplayName() {
        return "Test Result";
    }

    public Build getOwner() {
        return parent.owner;
    }

    @Override
    public TestResult getPreviousResult() {
        Build b = getOwner();
        while(true) {
            b = b.getPreviousBuild();
            if(b==null)
                return null;
            TestResultAction r = b.getAction(TestResultAction.class);
            if(r!=null)
                return r.getResult();
        }
    }

    public String getTitle() {
        return "Test Result";
    }

    public String getChildTitle() {
        return "Package";
    }

    @Override
    public int getPassCount() {
        return totalTests-getFailCount();
    }

    @Override
    public int getFailCount() {
        return failedTests.size();
    }

    public List<CaseResult> getFailedTests() {
        return failedTests;
    }

    public Collection<PackageResult> getChildren() {
        return byPackages.values();
    }

    public PackageResult getDynamic(String packageName, StaplerRequest req, StaplerResponse rsp) {
        return byPackage(packageName);
    }

    public PackageResult byPackage(String packageName) {
        return byPackages.get(packageName);
    }

    public SuiteResult getSuite(String name) {
        return suitesByName.get(name);
    }

    /**
     * Builds up the transient part of the data structure.
     */
    void freeze() {
        suitesByName = new HashMap<String,SuiteResult>();
        totalTests = 0;
        failedTests = new ArrayList<CaseResult>();
        byPackages = new TreeMap<String,PackageResult>();
        for (SuiteResult s : suites) {
            s.freeze(this);

            suitesByName.put(s.getName(),s);

            totalTests += s.getCases().size();
            for(CaseResult cr : s.getCases()) {
                if(!cr.isPassed())
                    failedTests.add(cr);

                String pkg = cr.getPackageName();
                PackageResult pr = byPackage(pkg);
                if(pr==null)
                    byPackages.put(pkg,pr=new PackageResult(this,pkg));
                pr.add(cr);
            }
        }

        for (PackageResult pr : byPackages.values())
            pr.freeze();
    }
}
