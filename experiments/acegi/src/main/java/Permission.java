import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Permission, which represents activity that requires a security privilege.
 *
 * <p>
 * Each permission is represented by a specific instance of {@link Permission}. 
 *
 * @author Kohsuke Kawaguchi
 */
public class Permission {
    public final Class owner;

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

    public Permission(Class owner, String name, Permission impliedBy) {
        this.owner = owner;
        this.name = name;
        this.impliedBy = impliedBy;

        synchronized (PERMISSIONS) {
            List<Permission> set = PERMISSIONS.get(owner);
            if(set==null) {
                set = new ArrayList<Permission>();
                PERMISSIONS.put(owner,set);
            }
            set.add(this);
        }
    }

    public Permission(Class owner, String name) {
        this(owner,name,null);
    }

    /**
     * All the permissions in the system, keyed by their owners.
     */
    private static Map<Class, List<Permission>> PERMISSIONS = new HashMap<Class,List<Permission>>();
}
