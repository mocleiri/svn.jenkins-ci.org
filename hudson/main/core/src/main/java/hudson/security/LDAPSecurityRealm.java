package hudson.security;

import com.sun.jndi.ldap.LdapCtxFactory;
import groovy.lang.Binding;
import hudson.Util;
import hudson.tasks.MailAddressResolver;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.util.FormFieldValidator;
import hudson.util.spring.BeanBuilder;
import net.sf.json.JSONObject;
import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.acegisecurity.userdetails.ldap.LdapUserDetails;
import org.acegisecurity.ldap.search.FilterBasedLdapUserSearch;
import org.acegisecurity.ldap.LdapUserSearch;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.dao.DataAccessException;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link SecurityRealm} implementation that uses LDAP for authentication.
 * 
 * @author Kohsuke Kawaguchi
 * @since 1.166
 */
public class LDAPSecurityRealm extends SecurityRealm {
    /**
     * LDAP server name, optionally with TCP port number, like "ldap.acme.org"
     * or "ldap.acme.org:389".
     */
    public final String server;

    /**
     * The root DN to connect to. Normally something like "dc=sun,dc=com"
     *
     * How do I infer this?
     */
    public final String rootDN;

    /**
     * Specifies the relative DN from {@link #rootDN the root DN}.
     * This is used to narrow down the search space when doing user search.
     *
     * Something like "ou=people" but can be empty.
     */
    public final String userSearchBase;

    /**
     * Query to locate an entry that identifies the user, given the user name string.
     *
     * Normally "uid={0}"
     *
     * @see FilterBasedLdapUserSearch
     */
    public final String userSearch;

    /*
        Other configurations that are needed:

        group search base DN (relative to root DN)
        group search filter (uniquemember={1} seems like a reasonable default)
        group target (CN is a reasonable default)

        manager dn/password if anonyomus search is not allowed.

        See GF configuration at http://weblogs.java.net/blog/tchangu/archive/2007/01/ldap_security_r.html
        Geronimo configuration at http://cwiki.apache.org/GMOxDOC11/ldap-realm.html
     */

    @DataBoundConstructor
    public LDAPSecurityRealm(String server, String rootDN, String userSearchBase, String userSearch) {
        this.server = server.trim();
        if(Util.fixEmptyAndTrim(rootDN)==null)    rootDN=Util.fixNull(inferRootDN(server));
        this.rootDN = rootDN.trim();
        this.userSearchBase = userSearchBase.trim();
        if(Util.fixEmptyAndTrim(userSearch)==null)    userSearch="uid={0}";
        this.userSearch = userSearch.trim();
    }

    /**
     * Infer the root DN.
     *
     * @return null if not found.
     */
    private String inferRootDN(String server) {
        try {
            DirContext ctx = LdapCtxFactory.getLdapCtxInstance("ldap://"+server+'/', new Hashtable());
            Attributes atts = ctx.getAttributes("");
            Attribute a = atts.get("defaultNamingContext");
            if(a!=null) // this entry is available on Active Directory. See http://msdn2.microsoft.com/en-us/library/ms684291(VS.85).aspx
                return a.toString();
            
            a = atts.get("namingcontexts");
            if(a==null) {
                LOGGER.warning("namingcontexts attribute not found in root DSE of "+server);
                return null;
            }
            return a.get().toString();
        } catch (NamingException e) {
            LOGGER.log(Level.WARNING,"Failed to connect to LDAP to infer Root DN for "+server,e);
            return null;
        }
    }

    public String getLDAPURL() {
        return "ldap://"+server+'/'+Util.fixNull(rootDN);
    }

    public SecurityComponents createSecurityComponents() {
        Binding binding = new Binding();
        binding.setVariable("instance", this);

        BeanBuilder builder = new BeanBuilder();
        builder.parse(Hudson.getInstance().servletContext.getResourceAsStream("/WEB-INF/security/LDAPBindSecurityRealm.groovy"),binding);
        final WebApplicationContext appContext = builder.createApplicationContext();

        return new SecurityComponents(
            findBean(AuthenticationManager.class, appContext),
            new UserDetailsService() {
                final LdapUserSearch ldapSerach = findBean(LdapUserSearch.class, appContext);
                public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
                    return ldapSerach.searchForUser(username);
                }
            });
    }

    /**
     * If the security realm is LDAP, try to pick up e-mail address from LDAP.
     */
    public static final class MailAdressResolverImpl extends MailAddressResolver {
        public String findMailAddressFor(User u) {
            // LDAP not active
            Hudson hudson = Hudson.getInstance();
            if(!(hudson.getSecurityRealm() instanceof LDAPSecurityRealm))
                return null;
            try {
                LdapUserDetails details = (LdapUserDetails) HudsonFilter.USER_DETAILS_SERVICE_PROXY.loadUserByUsername(u.getId());
                Attribute mail = details.getAttributes().get("mail");
                if(mail==null)  return null;    // not found
                return (String)mail.get();
            } catch (UsernameNotFoundException e) {
                LOGGER.log(Level.FINE, "Failed to look up LDAP for e-mail address",e);
                return null;
            } catch (DataAccessException e) {
                LOGGER.log(Level.FINE, "Failed to look up LDAP for e-mail address",e);
                return null;
            } catch (NamingException e) {
                LOGGER.log(Level.FINE, "Failed to look up LDAP for e-mail address",e);
                return null;
            }
        }
    }

    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<SecurityRealm> {
        private DescriptorImpl() {
            super(LDAPSecurityRealm.class);
        }

        public LDAPSecurityRealm newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(LDAPSecurityRealm.class,formData);
        }

        public String getDisplayName() {
            return "LDAP";
        }

        public void doServerCheck(StaplerRequest req, StaplerResponse rsp, @QueryParameter("server") final String server) throws IOException, ServletException {
            new FormFieldValidator(req,rsp,true) {
                protected void check() throws IOException, ServletException {
                    try {
                        DirContext ctx = LdapCtxFactory.getLdapCtxInstance("ldap://"+server+'/', new Hashtable());
                        ctx.getAttributes("");
                        ok();   // connected
                    } catch (NamingException e) {
                        // trouble-shoot
                        Matcher m = Pattern.compile("([^:]+)(?:\\:(\\d+))?").matcher(server.trim());
                        if(!m.matches()) {
                            error("Syntax of this field is SERVER or SERVER:PORT");
                            return;
                        }

                        try {
                            InetAddress adrs = InetAddress.getByName(m.group(1));
                            int port=389;
                            if(m.group(2)!=null)
                                port = Integer.parseInt(m.group(2));
                            Socket s = new Socket(adrs,port);
                            s.close();
                        } catch (NumberFormatException x) {
                            // impossible, because of the regexp
                        } catch (UnknownHostException x) {
                            error("Unknown host: "+x.getMessage());
                            return;
                        } catch (IOException x) {
                            error("Unable to connect to "+server+" : "+x.getMessage());
                            return;
                        }

                        // otherwise we don't know what caused it, so fall back to the general error report
                        // getMessage() alone doesn't offer enough
                        error("Unable to connect to "+server+": "+e);
                    } catch (NumberFormatException x) {
                        // The getLdapCtxInstance method throws this if it fails to parse the port number
                        error("Invalid port number");
                    }
                }
            }.check();
        }
    }

    static {
        LIST.add(DESCRIPTOR);
    }

    private static final Logger LOGGER = Logger.getLogger(LDAPSecurityRealm.class.getName());
}
