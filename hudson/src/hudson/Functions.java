package hudson;

import hudson.model.ModelObject;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Run;
import hudson.model.Hudson;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.List;
import java.util.Map;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
public class Functions {
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

    public static String getProjectListString(List<Project> projects) {
        return Project.toNameList(projects);
    }

    public static Object ifThenElse(boolean cond, Object thenValue, Object elseValue) {
        return cond ? thenValue : elseValue;
    }
    
    public static String appendIfNotNull(String text, String suffix, String nullText) {
        return text == null ? nullText : text + suffix;
    }

    public static Map getSystemProperties() {
        return System.getProperties();
    }

    public static Map getEnvVars() {
        return EnvVars.masterEnvVars;
    }

    public static boolean isWindows() {
        return File.pathSeparatorChar==';';
    }

    public static List<LogRecord> getLogRecords() {
        return Hudson.logRecords;
    }

    public static String printLogRecord(LogRecord r) {
        return formatter.format(r);
    }

    private static final SimpleFormatter formatter = new SimpleFormatter();
}
