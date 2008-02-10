package hudson.tasks.junit;

import hudson.model.AbstractBuild;
import hudson.util.IOException2;
import hudson.*;
import org.apache.tools.ant.DirectoryScanner;
import org.dom4j.DocumentException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    // set during the freeze phase
    private transient TestResultAction parent;

    /**
     * Number of all tests.
     */
    private transient int totalTests;

    private transient int skippedTests;
    
    private float duration;
    
    /**
     * Number of failed/error tests.
     */
    private transient List<CaseResult> failedTests;

    /**
     * Creates an empty result.
     */
    TestResult() {
    }

    /**
     * Collect reports from the given {@link DirectoryScanner}, while
     * filtering out all files that were created before the given time.
     */
    public TestResult(long buildTime, DirectoryScanner results) throws IOException {
        parse(buildTime, results);
    }

    /**
     * Collect reports from the given {@link DirectoryScanner}, while
     * filtering out all files that were created before the given time.
     */
    public void parse(long buildTime, DirectoryScanner results) throws IOException {
        String[] includedFiles = results.getIncludedFiles();
        File baseDir = results.getBasedir();

        boolean parsed=false;

        for (String value : includedFiles) {
            File reportFile = new File(baseDir, value);
            // only count files that were actually updated during this build
            if(buildTime-1000/*error margin*/ <= reportFile.lastModified()) {
                if(reportFile.length()==0) {
                    // this is a typical problem when JVM quits abnormally, like OutOfMemoryError during a test.
                    SuiteResult sr = new SuiteResult(reportFile.getName(), "", "");
                    sr.addCase(new CaseResult(sr,"<init>","Test report file "+reportFile.getAbsolutePath()+" was length 0"));
                    suites.add(sr);
                } else {
                    parse(reportFile);
                }
                parsed = true;
            }
        }

        if(!parsed) {
            long localTime = System.currentTimeMillis();
            if(localTime < buildTime-1000) /*margin*/
                // build time is in the the future. clock on this slave must be running behind
                throw new AbortException(
                    "Clock on this slave is out of sync with the master, and therefore \n" +
                    "I can't figure out what test results are new and what are old.\n" +
                    "Please keep the slave clock in sync with the master.");

            File f = new File(baseDir,includedFiles[0]);
            throw new AbortException(
                String.format(
                "Test reports were found but none of them are new. Did tests run? \n"+
                "For example, %s is %s old\n", f,
                Util.getTimeSpanString(buildTime-f.lastModified())));
        }
    }

    /**
     * Parses an additional report file.
     */
    public void parse(File reportFile) throws IOException {
        try {
            SuiteResult suiteResult = new SuiteResult(reportFile);
            suites.add( suiteResult );
            duration += suiteResult.getDuration(); 
        } catch (RuntimeException e) {
            throw new IOException2("Failed to read "+reportFile,e);
        } catch (DocumentException e) {
            if(!reportFile.getPath().endsWith(".xml"))
                throw new IOException2("Failed to read "+reportFile+"\n"+
                    "Is this really a JUnit report file? Your configuration must be matching too many files",e);
            else
                throw new IOException2("Failed to read "+reportFile,e);
        }
    }

    public String getDisplayName() {
        return "Test Result";
    }

    public AbstractBuild<?,?> getOwner() {
        return parent.owner;
    }

    @Override
    public TestResult getPreviousResult() {
        TestResultAction p = parent.getPreviousResult();
        if(p!=null)
            return p.getResult();
        else
            return null;
    }

    public String getTitle() {
        return "Test Result";
    }

    public String getChildTitle() {
        return "Package";
    }

    // TODO once stapler 1.60 is released: @Exported
    public float getDuration() {
        return duration; 
    }
    
    @Exported(visibility=999)
    @Override
    public int getPassCount() {
        return totalTests-getFailCount()-getSkipCount();
    }

    @Exported(visibility=999)
    @Override
    public int getFailCount() {
        return failedTests.size();
    }

    @Exported
    @Override
    public int getSkipCount() {
        return skippedTests;
    }

    @Override
    public List<CaseResult> getFailedTests() {
        return failedTests;
    }

    @Override
    @Exported(name="child",inline=true)
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
     * Builds up the transient part of the data structure
     * from results {@link #parse(File) parsed} so far.
     *
     * <p>
     * After the data is frozen, more files can be parsed
     * and then freeze can be called again.
     */
    public void freeze(TestResultAction parent) {
        this.parent = parent;
        if(suitesByName==null) {
            // freeze for the first time
            suitesByName = new HashMap<String,SuiteResult>();
            totalTests = 0;
            failedTests = new ArrayList<CaseResult>();
            byPackages = new TreeMap<String,PackageResult>();
        }

        for (SuiteResult s : suites) {
            if(!s.freeze(this))
                continue;

            suitesByName.put(s.getName(),s);

            totalTests += s.getCases().size();
            for(CaseResult cr : s.getCases()) {
                if(cr.isSkipped())
                    skippedTests++;
                else if(!cr.isPassed())
                    failedTests.add(cr);

                String pkg = cr.getPackageName();
                PackageResult pr = byPackage(pkg);
                if(pr==null)
                    byPackages.put(pkg,pr=new PackageResult(this,pkg));
                pr.add(cr);
            }
        }

        Collections.sort(failedTests,CaseResult.BY_AGE);

        for (PackageResult pr : byPackages.values())
            pr.freeze();
    }
}
