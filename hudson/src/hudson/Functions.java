package hudson;

import hudson.model.Fingerprint.RangeSet;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestObject;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.List;
import java.util.SortedMap;

/**
 * @author Kohsuke Kawaguchi
 */
public class Functions {
    public static boolean containsJob(hudson.model.JobCollection jc,hudson.model.Job j) {
        return jc.containsJob(j);
    }

    public static Job getJob(String name) {
        return Hudson.getInstance().getJob(name);
    }

    public static Run getRun(Job job, int number) {
        return job.getBuildByNumber(number);
    }

    public static boolean isModel(Object o) {
        return o instanceof ModelObject;
    }

    public static String getDiffString(int i) {
        if(i==0)    return "\u00B10";   // +/-0
        String s = Integer.toString(i);
        if(i>0)     return "+"+s;
        else        return s;
    }

    /**
     * {@link #getDiffString2(int)} that doesn't show anything for +/-0
     */
    public static String getDiffString2(int i) {
        if(i==0)    return "";
        String s = Integer.toString(i);
        if(i>0)     return "+"+s;
        else        return s;
    }

    /**
     * Adds the proper suffix.
     */
    public static String addSuffix(int n, String singular, String plural) {
        StringBuffer buf = new StringBuffer();
        buf.append(n).append(' ');
        if(n==1)
            buf.append(singular);
        else
            buf.append(plural);
        return buf.toString();
    }

    public static RunUrl decompose(StaplerRequest req) {
        List<Ancestor> ancestors = (List<Ancestor>) req.getAncestors();
        for (Ancestor anc : ancestors) {
            if(anc.getObject() instanceof Run) {
                // bingo
                String ancUrl = anc.getUrl();

                String reqUri = req.getOriginalRequestURI();

                return new RunUrl(
                    (Run) anc.getObject(), ancUrl,
                    reqUri.substring(ancUrl.length()),
                    req.getContextPath() );
            }
        }
        return null;
    }

    public static final class RunUrl {
        private final String contextPath;
        private final String basePortion;
        private final String rest;
        private final Run run;

        public RunUrl(Run run, String basePortion, String rest, String contextPath) {
            this.run = run;
            this.basePortion = basePortion;
            this.rest = rest;
            this.contextPath = contextPath;
        }

        public String getBaseUrl() {
            return basePortion;
        }

        /**
         * Returns the same page in the next build.
         */
        public String getNextBuildUrl() {
            return getUrl(run.getNextBuild());
        }

        /**
         * Returns the same page in the previous build.
         */
        public String getPreviousBuildUrl() {
            return getUrl(run.getPreviousBuild());
        }

        private String getUrl(Run n) {
            if(n ==null)
                return null;
            else {
                String url = contextPath + '/' + n.getUrl();
                assert url.endsWith("/");
                url = url.substring(0,url.length()-1);  // cut off the trailing '/'
                return url+rest;
            }
        }
    }

    public static Node.Mode[] getNodeModes() {
        return Node.Mode.values();
    }

    public static String getTestRelativePathFrom(TestObject it, CaseResult testCase) {
        return testCase.getRelativePathFrom(it);
    }

    public static SortedMap<Integer,RangeSet> getProjectRelationshipMap(Project lhs, Project rhs) {
        return lhs.getRelationship(rhs);
    }
}
