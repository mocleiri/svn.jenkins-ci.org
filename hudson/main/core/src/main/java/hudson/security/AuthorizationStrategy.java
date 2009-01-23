package hudson.security;

import hudson.ExtensionPoint;
import hudson.slaves.Cloud;
import hudson.model.AbstractItem;
import hudson.model.Computer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.User;
import hudson.model.View;
import hudson.model.Node;
import hudson.util.DescriptorList;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import net.sf.json.JSONObject;

import org.acegisecurity.Authentication;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Controls authorization throughout Hudson.
 *
 * <h2>Persistence</h2>
 * <p>
 * This object will be persisted along with {@link Hudson} object.
 * Hudson by itself won't put the ACL returned from {@link #getRootACL()} into the serialized object graph,
 * so if that object contains state and needs to be persisted, it's the responsibility of
 * {@link AuthorizationStrategy} to do so (by keeping them in an instance field.)
 *
 * <h2>Re-configuration</h2>
 * <p>
 * The corresponding {@link Describable} instance will be asked to create a new {@link AuthorizationStrategy}
 * every time the system configuration is updated. Implementations that keep more state in ACL beyond
 * the system configuration should use {@link Hudson#getAuthorizationStrategy()} to talk to the current
 * instance to carry over the state. 
 *
 * @author Kohsuke Kawaguchi
 * @see SecurityRealm
 */
public abstract class AuthorizationStrategy implements Describable<AuthorizationStrategy>, ExtensionPoint {
    /**
     * Returns the instance of {@link ACL} where all the other {@link ACL} instances
     * for all the other model objects eventually delegate.
     * <p>
     * IOW, this ACL will have the ultimate say on the access control.
     */
    public abstract ACL getRootACL();

    public ACL getACL(Job<?,?> project) {
    	return getRootACL();
    }

    /**
     * Implementation can choose to provide different ACL for different views.
     * This can be used as a basis for more fine-grained access control.
     *
     * <p>
     * The default implementation returns {@link #getRootACL()}.
     *
     * @since 1.220
     */
    public ACL getACL(View item) {
    	return getRootACL();
    }

    /**
     * Implementation can choose to provide different ACL for different items.
     * This can be used as a basis for more fine-grained access control.
     *
     * <p>
     * The default implementation returns {@link #getRootACL()}.
     *
     * @since 1.220
     */
    public ACL getACL(AbstractItem item) {
        return getRootACL();
    }

    /**
     * Implementation can choose to provide different ACL per user.
     * This can be used as a basis for more fine-grained access control.
     *
     * <p>
     * The default implementation returns {@link #getRootACL()}.
     *
     * @since 1.221
     */
    public ACL getACL(User user) {
        return getRootACL();
    }

    /**
     * Implementation can choose to provide different ACL for different computers.
     * This can be used as a basis for more fine-grained access control.
     *
     * <p>
     * The default implementation delegates to {@link #getACL(Node)}
     *
     * @since 1.220
     */
    public ACL getACL(Computer computer) {
        return getACL(computer.getNode());
    }

    /**
     * Implementation can choose to provide different ACL for different {@link Cloud}s.
     * This can be used as a basis for more fine-grained access control.
     *
     * <p>
     * The default implementation returns {@link #getRootACL()}.
     *
     * @since 1.252
     */
    public ACL getACL(Cloud cloud) {
        return getRootACL();
    }

    public ACL getACL(Node node) {
        return getRootACL();
    }

    /**
     * Returns the list of all group/role names used in this authorization strategy,
     * and the ACL returned from the {@link #getRootACL()} method.
     * <p>
     * This method is used by {@link ContainerAuthentication} to work around the servlet API issue
     * that prevents us from enumerating roles that the user has.
     * <p>
     * If such enumeration is impossible, do the best to list as many as possible, then
     * return it. In the worst case, just return an empty list. Doing so would prevent
     * users from using role names as group names (see HUDSON-2716 for such one such report.)
     *
     * @return
     *      never null.
     */
    public abstract Collection<String> getGroups();

    /**
     * All registered {@link SecurityRealm} implementations.
     */
    public static final DescriptorList<AuthorizationStrategy> LIST = new DescriptorList<AuthorizationStrategy>();
    
    /**
     * {@link AuthorizationStrategy} that implements the semantics
     * of unsecured Hudson where everyone has full control.
     */
    public static final AuthorizationStrategy UNSECURED = new Unsecured();

    private static final class Unsecured extends AuthorizationStrategy implements Serializable {
        /**
         * Maintains the singleton semantics.
         */
        private Object readResolve() {
            return UNSECURED;
        }

        public Descriptor<AuthorizationStrategy> getDescriptor() {
            return DESCRIPTOR;
        }

        @Override
        public ACL getRootACL() {
            return UNSECURED_ACL;
        }

        public Collection<String> getGroups() {
            return Collections.emptySet();
        }

        private static final ACL UNSECURED_ACL = new ACL() {
            public boolean hasPermission(Authentication a, Permission permission) {
                return true;
            }
        };

        private static final Descriptor<AuthorizationStrategy> DESCRIPTOR = new Descriptor<AuthorizationStrategy>() {
            public String getDisplayName() {
                return Messages.AuthorizationStrategy_DisplayName();
            }

            public AuthorizationStrategy newInstance(StaplerRequest req, JSONObject formData) throws FormException {
                return UNSECURED;
            }

            public String getHelpFile() {
                return "/help/security/no-authorization.html";
            }
        };

        static {
            LIST.load(FullControlOnceLoggedInAuthorizationStrategy.class);
            LIST.load(GlobalMatrixAuthorizationStrategy.class);
            LIST.load(LegacyAuthorizationStrategy.class);
            LIST.load(ProjectMatrixAuthorizationStrategy.class);
            
            // can't do this in the constructor due to the initialization order
            LIST.add(Unsecured.DESCRIPTOR);
        }
    }

}
