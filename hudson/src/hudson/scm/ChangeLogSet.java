package hudson.scm;

/**
 * Represents SCM change list.
 *
 * Use the "index" view of this object to render the changeset detail page,
 * and use the "digest" view of this object to render the summary page.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ChangeLogSet {
    /**
     * Returns true if there's no change.
     */
    public abstract boolean isEmptySet();
}
