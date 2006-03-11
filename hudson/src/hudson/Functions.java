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
}
