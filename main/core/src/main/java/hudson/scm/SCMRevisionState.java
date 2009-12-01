package hudson.scm;

import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.Launcher;
import hudson.FilePath;
import hudson.util.PartialOrder;

/**
 * Immutable object that represents revisions of the files in the repository,
 * used to represent the result of
 * {@linkplain SCM#poll(AbstractProject, Launcher, FilePath, TaskListener, SCMRevisionState) a SCM polling}.
 *
 * <p>
 * This object is used so that the successive polling can compare the tip of the repository now vs
 * what it was when it was last time polled. (Before 1.337, Hudson was only able to compare the tip
 * of the repository vs the state of the workspace, which resulted in a problem like HUDSON-2180.
 *
 * <p>
 * {@link SCMRevisionState} is persisted as an action to {@link AbstractBuild}.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.337
 */
public abstract class SCMRevisionState implements Action {
    /**
     * Compares this state with another state to determine which is newer (=greater.)
     * <p>
     * Revision state normally consists of multiple values, so as a whole they result in a partial order.
     */
    public abstract PartialOrder compareTo(SCMRevisionState that);

//
// implemented as an invisible action by default
//
    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }

    /**
     * Constant that represents no revision state, which is used with pre-1.337 {@link SCM} implementation.
     */
    public static SCMRevisionState NONE = new None();

    private static final class None extends SCMRevisionState {
        public PartialOrder compareTo(SCMRevisionState that) {
            return PartialOrder.INCOMPARABLE;
        }
    }
}
