package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.List;
import java.util.Vector;

/**
 * {@link ModelObject} that can have additional {@link Action}s.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Actionable implements ModelObject {
    /**
     * Actions contributed to this run.
     */
    private List<Action> actions;

    /**
     * Gets actions contributed to this build.
     *
     * @return
     *      may be empty but never null.
     */
    public synchronized List<Action> getActions() {
        if(actions==null)
            actions = new Vector<Action>();
        return actions;
    }

    public Action getAction(int index) {
        if(actions==null)   return null;
        return actions.get(index);
    }

    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        if(actions!=null) {
            for (Action a : actions) {
                if(a.getUrlName().equals(token))
                    return a;
            }
        }
        return null;
    }
}
