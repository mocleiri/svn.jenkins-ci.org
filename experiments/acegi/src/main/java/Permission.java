/**
 *
 * TODO: I need to be able to 
 *
 * @author Kohsuke Kawaguchi
 */
public class Permission {
    public final String name;
    /**
     * Bundled {@link Permission} that also implies this permission.
     *
     * <p>
     * This allows us to organize permissions in a hierarchy, so that
     * for example we can say "view workspace" permission is implied by
     * the (broader) "read" permission.
     *
     * <p>
     * The idea here is that for most people, access control based on
     * such broad permission bundle is good enough, and those few
     * that need finer control can do so.
     */
    public final Permission impliedBy;

    public Permission(String name, Permission impliedBy) {
        this.name = name;
        this.impliedBy = impliedBy;
    }

    public Permission(String name) {
        this(name,null);
    }
}
