package hudson;

import hudson.model.ModelObject;

/**
 * @author Kohsuke Kawaguchi
 */
public class Functions {
    public static boolean containsJob(hudson.model.JobCollection jc,hudson.model.Job j) {
        return jc.containsJob(j);
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
}
