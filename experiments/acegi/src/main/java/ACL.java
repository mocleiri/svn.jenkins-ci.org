import org.acegisecurity.AccessDeniedException;
import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.acls.sid.GrantedAuthoritySid;
import org.acegisecurity.acls.sid.PrincipalSid;
import org.acegisecurity.acls.sid.Sid;
import org.acegisecurity.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Accses control list.
 * 
 * @author Kohsuke Kawaguchi
 */
public class ACL {
    public static final class Entry {
        // Sid has value-equality semantics
        public final Sid sid;
        public final Permission permission;
        public final boolean allowed;

        public Entry(Sid sid, Permission permission, boolean allowed) {
            this.sid = sid;
            this.permission = permission;
            this.allowed = allowed;
        }
    }

    private final List<Entry> entries = new ArrayList<Entry>();
    private ACL parent;

    public ACL(ACL parent) {
        this.parent = parent;
    }

    public void add(Entry e) {
        entries.add(e);
    }

    public void add(Sid sid, Permission permission, boolean allowed) {
        add(new Entry(sid,permission,allowed));
    }

    /**
     * Checks if the current security principal has this permission.
     *
     * @throws AccessDeniedException
     *      if the user doesn't have the permission.
     */
    public void checkPermission(Permission p) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();

        if(!hasPermission(a,p))
            throw new AccessDeniedException(a.toString()+" is missing "+p.name);
    }

    public boolean hasPermission(Authentication a, Permission permission) {
        // ACL entries for this principal takes precedence
        Boolean b = hasPermission(new PrincipalSid(a),permission);
        if(b!=null) return b;

        // after that, we check if the groups this principal belongs to
        // has any ACL entries.
        // here we are using GrantedAuthority as a group
        for(GrantedAuthority ga : a.getAuthorities()) {
            b = hasPermission(new GrantedAuthoritySid(ga),permission);
            if(b!=null) return b;
        }

        // finally everyone
        b = hasPermission(EVERYONE,permission);
        if(b!=null) return b;

        if(parent!=null)
            return parent.hasPermission(a,permission);
        
        return false;
    }

    // sub-routine used by the above method
    private Boolean hasPermission(Sid p, Permission permission) {
        for( ; permission!=null; permission=permission.impliedBy ) {
            for (Entry e : entries) {
                if(e.permission==permission && e.sid.equals(p))
                    return e.allowed;
            }
        }
        return null;
    }

    /**
     * {@link Sid} that represents everyone, including anonymous users.
     */
    public static final Sid EVERYONE = new Sid() {};

    public static final Sid ANONYMOUS = new PrincipalSid("anonymous");
}
