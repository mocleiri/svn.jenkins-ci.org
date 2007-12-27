package hudson.security;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import hudson.model.Descriptor;
import net.sf.json.JSONObject;
import org.acegisecurity.Authentication;
import org.acegisecurity.acls.sid.GrantedAuthoritySid;
import org.acegisecurity.acls.sid.PrincipalSid;
import org.acegisecurity.acls.sid.Sid;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Role-based authorization via a matrix.
 *
 * @author Kohsuke Kawaguchi
 */
public class GlobalMatrixAuthorizationStrategy extends AuthorizationStrategy {
    private transient ACL acl = new AclImpl();

    /**
     * List up all permissions that are granted.
     *
     * Strings are either the granted authority or the principal,
     * which is not distinguished.
     */
    private final Map<Permission,Set<String>> grantedPermissions = new HashMap<Permission, Set<String>>();

    /**
     * Adds to {@link #grantedPermissions}.
     * Use of this method should be limited during construction,
     * as this object itself is considered immutable once populated.
     */
    private void add(Permission p, String sid) {
        Set<String> set = grantedPermissions.get(p);
        if(set==null)
            grantedPermissions.put(p,set = new HashSet<String>());
        set.add(sid);

    }

    /**
     * Works like {@link #add(Permission, String)} but takes both parameters
     * from a single string of the form <tt>PERMISSIONID:sid</tt>
     */
    private void add(String shortForm) {
        int idx = shortForm.indexOf(':');
        add(Permission.fromId(shortForm.substring(0,idx)),shortForm.substring(idx+1));
    }

    @Override
    public ACL getRootACL() {
        return acl;
    }

    private Object readResolve() {
        acl = new AclImpl();
        return this;
    }

    /**
     * Checks if the given SID has the given permission.
     */
    public boolean hasPermission(String sid, Permission p) {
        Set<String> set = grantedPermissions.get(p);
        return set!=null && set.contains(sid);
    }

    /**
     * Returns all SIDs configured in this matrix, minus "anonymous"
     *
     * @return
     *      Always non-null.
     */
    public List<String> getAllSIDs() {
        Set<String> r = new HashSet<String>();
        for (Set<String> set : grantedPermissions.values())
            r.addAll(set);
        r.remove("anonymous");

        String[] data = r.toArray(new String[r.size()]);
        Arrays.sort(data);
        return Arrays.asList(data);
    }

    private final class AclImpl extends SidACL {
        protected Boolean hasPermission(Sid p, Permission permission) {
            if(GlobalMatrixAuthorizationStrategy.this.hasPermission(toString(p),permission))
                return true;
            return null;
        }

        protected Boolean _hasPermission(Authentication a, Permission permission) {
            Boolean b = super._hasPermission(a,permission);
            // permissions granted to anonymous users are granted to everyone
            if(b==null) b=hasPermission(ANONYMOUS,permission);
            return b;
        }

        private String toString(Sid p) {
            if (p instanceof GrantedAuthoritySid)
                return ((GrantedAuthoritySid) p).getGrantedAuthority();
            if (p instanceof PrincipalSid)
                return ((PrincipalSid) p).getPrincipal();
            // hmm...
            return p.toString();
        }
    }

    public Descriptor<AuthorizationStrategy> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor<AuthorizationStrategy> DESCRIPTOR = new DescriptorImpl();

    /**
     * Persist {@link GlobalMatrixAuthorizationStrategy} as a list of IDs that
     * represent {@link GlobalMatrixAuthorizationStrategy#grantedPermissions}.
     */
    public static final class ConverterImpl implements Converter {
        public boolean canConvert(Class type) {
            return type== GlobalMatrixAuthorizationStrategy.class;
        }

        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            GlobalMatrixAuthorizationStrategy strategy = (GlobalMatrixAuthorizationStrategy)source;

            for (Entry<Permission, Set<String>> e : strategy.grantedPermissions.entrySet()) {
                String p = e.getKey().getId();
                for (String sid : e.getValue()) {
                    writer.startNode("permission");
                    context.convertAnother(p+':'+sid);
                    writer.endNode();
                }
            }

        }

        public Object unmarshal(HierarchicalStreamReader reader, final UnmarshallingContext context) {
            GlobalMatrixAuthorizationStrategy as = new GlobalMatrixAuthorizationStrategy();

            while (reader.hasMoreChildren()) {
                reader.moveDown();
                String id = (String)context.convertAnother(as,String.class);
                as.add(id);
                reader.moveUp();
            }

            return as;
        }
    }
    
    static {
        LIST.add(DESCRIPTOR);
    }

    public static final class DescriptorImpl extends Descriptor<AuthorizationStrategy> {
        public DescriptorImpl() {
            super(GlobalMatrixAuthorizationStrategy.class);
        }

        public String getDisplayName() {
            return Messages.GlobalMatrixAuthorizationStrategy_DisplayName();
        }

        public AuthorizationStrategy newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            GlobalMatrixAuthorizationStrategy gmas = new GlobalMatrixAuthorizationStrategy();
            for(Map.Entry<String,JSONObject> r : (Set<Map.Entry<String,JSONObject>>)formData.getJSONObject("data").entrySet()) {
                String sid = r.getKey();
                for(Map.Entry<String,Boolean> e : (Set<Map.Entry<String,Boolean>>)r.getValue().entrySet()) {
                    if(e.getValue()) {
                        Permission p = Permission.fromId(e.getKey().replace('-','.'));
                        gmas.add(p,sid);
                    }
                }
            }
            return gmas;
        }

        public String getHelpFile() {
            return "/help/security/global-matrix.html";
        }

        public List<PermissionGroup> getAllGroups() {
            List<PermissionGroup> groups = new ArrayList<PermissionGroup>(PermissionGroup.getAll());
            groups.remove(PermissionGroup.get(Permission.class));
            return groups;
        }
    }
}

