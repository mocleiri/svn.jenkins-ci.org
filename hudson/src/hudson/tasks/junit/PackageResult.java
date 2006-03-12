package hudson.tasks.junit;

import hudson.model.Build;

import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Cumulative test result for a package.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PackageResult extends MetaTabulatedResult {
    private final String packageName;

    /**
     * All {@link ClassResult}s keyed by their short name.
     */
    private final Map<String,ClassResult> classes = new TreeMap<String,ClassResult>();

    private int passCount,failCount;

    private final TestResult parent;

    PackageResult(TestResult parent, String packageName) {
        this.packageName = packageName;
        this.parent = parent;
    }

    public String getName() {
        return packageName;
    }

    public Build getOwner() {
        return parent.getOwner();
    }

    public PackageResult getPreviousResult() {
        TestResult tr = parent.getPreviousResult();
        if(tr==null)    return null;
        return tr.byPackage(getName());
    }

    public String getTitle() {
        return "Test Result : "+getName();
    }

    public String getChildTitle() {
        return "Class";
    }

    public int getPassCount() {
        return passCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public ClassResult getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
        return classes.get(name);
    }

    public Collection<ClassResult> getChildren() {
        return classes.values();
    }

    public List<CaseResult> getFailedTests() {
        List<CaseResult> r = new ArrayList<CaseResult>();
        for (ClassResult clr : classes.values()) {
            for (CaseResult cr : clr.getChildren()) {
                if(!cr.isPassed())
                    r.add(cr);
            }
        }
        Collections.sort(r,CaseResult.BY_AGE);
        return r;
    }

    void add(CaseResult r) {
        String n = r.getSimpleName();
        ClassResult c = classes.get(n);
        if(c==null)
            classes.put(n,c=new ClassResult(this,n));
        c.add(r);
    }

    void freeze() {
        passCount=failCount=0;
        for (ClassResult cr : classes.values()) {
            cr.freeze();
            passCount += cr.getPassCount();
            failCount += cr.getFailCount();
        }
    }


    public int compareTo(PackageResult that) {
        return this.packageName.compareTo(that.packageName);
    }

    public String getDisplayName() {
        return packageName;
    }
}
