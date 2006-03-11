package hudson.tasks.junit;

import hudson.model.Build;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Cumulative test result of a test class.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ClassResult extends TabulatedResult implements Comparable<ClassResult> {
    private final String className;

    private final List<CaseResult> cases = new ArrayList<CaseResult>();

    private int passCount,failCount;

    private final PackageResult parent;

    ClassResult(PackageResult parent, String className) {
        this.parent = parent;
        this.className = className;
    }

    public Build getOwner() {
        return parent.getOwner();
    }

    public String getTitle() {
        return "Test Result : "+getName();
    }

    public String getName() {
        int idx = className.lastIndexOf('.');
        if(idx<0)       return className;
        else            return className.substring(idx+1);
    }

    public List<CaseResult> getChildren() {
        return cases;
    }

    public int getPassCount() {
        return passCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void add(CaseResult r) {
        cases.add(r);
    }

    void freeze() {
        passCount=failCount=0;
        for (CaseResult r : cases) {
            if(r.isPassed())    passCount++;
            else                failCount++;
        }
        Collections.sort(cases);
    }


    public int compareTo(ClassResult that) {
        return this.className.compareTo(that.className);
    }

    public String getDisplayName() {
        return getName();
    }
}
