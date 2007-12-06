package hudson.security;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.DescriptorList;
import org.acegisecurity.Authentication;
import org.kohsuke.stapler.StaplerRequest;

import java.io.Serializable;

import net.sf.json.JSONObject;

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

        /**
         * Thie {@link AuthorizationStrategy} is special
         * in that it cannot be explicitly configured, hence there's no
         * descriptor for this. 
         */
        public Descriptor<AuthorizationStrategy> getDescriptor() {
            return DESCRIPTOR;
        }

        @Override
        public ACL getRootACL() {
            return UNSECURED_ACL;
        }

        private static final ACL UNSECURED_ACL = new ACL() {
            public boolean hasPermission(Authentication a, Permission permission) {
                return true;
            }
        };

        private static final Descriptor<AuthorizationStrategy> DESCRIPTOR = new Descriptor<AuthorizationStrategy>(Unsecured.class) {
            public String getDisplayName() {
                return "Anyone can do anything";
            }

            public AuthorizationStrategy newInstance(StaplerRequest req, JSONObject formData) throws FormException {
                return UNSECURED;
            }
        };

    }

    /**
     * All registered {@link SecurityRealm} implementations.
     */
    public static final DescriptorList<AuthorizationStrategy> LIST = new DescriptorList<AuthorizationStrategy>(
        Unsecured.DESCRIPTOR
    );
}
