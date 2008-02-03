package hudson.tasks.junit;

import hudson.model.AbstractBuild;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    private int passCount,failCount,skipCount;

    private final TestResult parent;
    private float duration; 

    PackageResult(TestResult parent, String packageName) {
        this.packageName = packageName;
        this.parent = parent;
    }

    @Exported
    public String getName() {
        return packageName;
    }

    public AbstractBuild<?,?> getOwner() {
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

    // TODO: wait until stapler 1.60 to do this @Exported
    public float getDuration() {
        return duration; 
    }
    
    @Exported
    public int getPassCount() {
        return passCount;
    }

    @Exported
    public int getFailCount() {
        return failCount;
    }

    @Exported
    public int getSkipCount() {
        return skipCount;
    }

    public ClassResult getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
        return classes.get(name);
    }

    @Exported(name="child")
    public Collection<ClassResult> getChildren() {
        return classes.values();
    }

    public List<CaseResult> getFailedTests() {
        List<CaseResult> r = new ArrayList<CaseResult>();
        for (ClassResult clr : classes.values()) {
            for (CaseResult cr : clr.getChildren()) {
                if(!cr.isPassed() && !cr.isSkipped())
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
        duration += r.getDuration(); 
    }

    void freeze() {
        passCount=failCount=0;
        for (ClassResult cr : classes.values()) {
            cr.freeze();
            passCount += cr.getPassCount();
            failCount += cr.getFailCount();
            skipCount += cr.getSkipCount();
        }
    }


    public int compareTo(PackageResult that) {
        return this.packageName.compareTo(that.packageName);
    }

    public String getDisplayName() {
        return packageName;
    }
}
