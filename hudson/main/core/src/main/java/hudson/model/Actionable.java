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
public abstract class Actionable extends AbstractModelObject {
    /**
     * Actions contributed to this model object.
     */
    private List<Action> actions;

    /**
     * Gets actions contributed to this build.
     *
     * <p>
     * A new {@link Action} can be added by {@code getActions().add(...)}.
     *
     * @return
     *      may be empty but never null.
     */
    public synchronized List<Action> getActions() {
        if(actions==null)
            actions = new Vector<Action>();
        return actions;
    }

    /**
     * Gets all actions of a specified type that contributed to this build.
     *
     * @param type The type of action to return.
     * @return
     *      may be empty but never null.
     * @see #getAction(Class)
     */
    public <T extends Action> List<T> getActions(Class<T> type) {
        List<T> result = new Vector<T>();
        for (Action a : getActions())
            if (type.isInstance(a))
                result.add(type.cast(a));
        return result;
    }

    /**
     * Adds a new action.
     *
     * Short for <tt>getActions().add(a)</tt>
     */
    public void addAction(Action a) {
        getActions().add(a);
    }

    public Action getAction(int index) {
        if(actions==null)   return null;
        return actions.get(index);
    }

    /**
     * Gets the action (first instance to be found) of a specified type that contributed to this build.
     *
     * @param type
     * @return The action or <code>null</code> if no such actions exist.
     * @see #getActions(Class)
     */
    public <T extends Action> T getAction(Class<T> type) {
        for (Action a : getActions())
            if (type.isInstance(a))
                return type.cast(a);
        return null;
    }

    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        for (Action a : getActions()) {
            if(a.getUrlName().equals(token))
                return a;
        }
        return null;
    }
}
