package hudson.security;

import com.sun.jndi.ldap.LdapCtxFactory;
import groovy.lang.Binding;
import hudson.Util;
import hudson.tasks.MailAddressResolver;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.util.FormFieldValidator;
import hudson.util.Scrambler;
import hudson.util.spring.BeanBuilder;
import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.acegisecurity.userdetails.ldap.LdapUserDetails;
import org.acegisecurity.ldap.search.FilterBasedLdapUserSearch;
import org.acegisecurity.ldap.LdapUserSearch;
import org.acegisecurity.ldap.LdapDataAccessException;
import org.acegisecurity.ldap.InitialDirContextFactory;
import org.acegisecurity.ldap.LdapTemplate;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.dao.DataAccessException;

import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * {@link SecurityRealm} implementation that uses LDAP for authentication.
 *
 *
 * <h2>Key Object Classes</h2>
 *
 * <h4>Group Membership</h4>
 *
 * <p>
 * Two object classes seem to be relevant. These are in RFC 2256 and core.schema. These use DN for membership,
 * so it can create a group of anything. I don't know what the difference between these two are.
 * <pre>
   attributetype ( 2.5.4.31 NAME 'member'
     DESC 'RFC2256: member of a group'
     SUP distinguishedName )

   attributetype ( 2.5.4.50 NAME 'uniqueMember'
     DESC 'RFC2256: unique member of a group'
     EQUALITY uniqueMemberMatch
     SYNTAX 1.3.6.1.4.1.1466.115.121.1.34 )

   objectclass ( 2.5.6.9 NAME 'groupOfNames'
     DESC 'RFC2256: a group of names (DNs)'
     SUP top STRUCTURAL
     MUST ( member $ cn )
     MAY ( businessCategory $ seeAlso $ owner $ ou $ o $ description ) )

   objectclass ( 2.5.6.17 NAME 'groupOfUniqueNames'
     DESC 'RFC2256: a group of unique names (DN and Unique Identifier)'
     SUP top STRUCTURAL
     MUST ( uniqueMember $ cn )
     MAY ( businessCategory $ seeAlso $ owner $ ou $ o $ description ) )
 * </pre>
 *
 * <p>
 * This one is from nis.schema, and appears to model POSIX group/user thing more closely.
 * <pre>
   objectclass ( 1.3.6.1.1.1.2.2 NAME 'posixGroup'
     DESC 'Abstraction of a group of accounts'
     SUP top STRUCTURAL
     MUST ( cn $ gidNumber )
     MAY ( userPassword $ memberUid $ description ) )

   attributetype ( 1.3.6.1.1.1.1.12 NAME 'memberUid'
     EQUALITY caseExactIA5Match
     SUBSTR caseExactIA5SubstringsMatch
     SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )

   objectclass ( 1.3.6.1.1.1.2.0 NAME 'posixAccount'
     DESC 'Abstraction of an account with POSIX attributes'
     SUP top AUXILIARY
     MUST ( cn $ uid $ uidNumber $ gidNumber $ homeDirectory )
     MAY ( userPassword $ loginShell $ gecos $ description ) )

   attributetype ( 1.3.6.1.1.1.1.0 NAME 'uidNumber'
     DESC 'An integer uniquely identifying a user in an administrative domain'
     EQUALITY integerMatch
     SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 SINGLE-VALUE )

   attributetype ( 1.3.6.1.1.1.1.1 NAME 'gidNumber'
     DESC 'An integer uniquely identifying a group in an administrative domain'
     EQUALITY integerMatch
     SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 SINGLE-VALUE )
 * </pre>
 *
 * <p>
 * Active Directory specific schemas (from <a href="http://www.grotan.com/ldap/microsoft.schema">here</a>).
 * <pre>
   objectclass ( 1.2.840.113556.1.5.8
     NAME 'group'
     SUP top
     STRUCTURAL
     MUST (groupType )
     MAY (member $ nTGroupMembers $ operatorCount $ adminCount $
         groupAttributes $ groupMembershipSAM $ controlAccessRights $
         desktopProfile $ nonSecurityMember $ managedBy $
         primaryGroupToken $ mail ) )

   objectclass ( 1.2.840.113556.1.5.9
     NAME 'user'
     SUP organizationalPerson
     STRUCTURAL
     MAY (userCertificate $ networkAddress $ userAccountControl $
         badPwdCount $ codePage $ homeDirectory $ homeDrive $
         badPasswordTime $ lastLogoff $ lastLogon $ dBCSPwd $
         localeID $ scriptPath $ logonHours $ logonWorkstation $
         maxStorage $ userWorkstations $ unicodePwd $
         otherLoginWorkstations $ ntPwdHistory $ pwdLastSet $
         preferredOU $ primaryGroupID $ userParameters $
         profilePath $ operatorCount $ adminCount $ accountExpires $
         lmPwdHistory $ groupMembershipSAM $ logonCount $
         controlAccessRights $ defaultClassStore $ groupsToIgnore $
         groupPriority $ desktopProfile $ dynamicLDAPServer $
         userPrincipalName $ lockoutTime $ userSharedFolder $
         userSharedFolderOther $ servicePrincipalName $
         aCSPolicyName $ terminalServer $ mSMQSignCertificates $
         mSMQDigests $ mSMQDigestsMig $ mSMQSignCertificatesMig $
         msNPAllowDialin $ msNPCallingStationID $
         msNPSavedCallingStationID $ msRADIUSCallbackNumber $
         msRADIUSFramedIPAddress $ msRADIUSFramedRoute $
         msRADIUSServiceType $ msRASSavedCallbackNumber $
         msRASSavedFramedIPAddress $ msRASSavedFramedRoute $
         mS-DS-CreatorSID ) )
 * </pre>
 *
 *
 * <h2>References</h2>
 * <dl>
 * <dt><a href="http://www.openldap.org/doc/admin22/schema.html">Standard Schemas</a>
 * <dd>
 * The downloadable distribution contains schemas that define the structure of LDAP entries.
 * Because this is a standard, we expect most LDAP servers out there to use it, although
 * there are different objectClasses that can be used for similar purposes, and apparently
 * many deployments choose to use different objectClasses.
 *
 * <dt><a href="http://www.ietf.org/rfc/rfc2256.txt">RFC 2256</a>
 * <dd>
 * Defines the meaning of several key datatypes used in the schemas with some explanations. 
 *
 * <dt><a href="http://msdn.microsoft.com/en-us/library/ms675085(VS.85).aspx">Active Directory schema</a>
 * <dd>
 * More navigable schema list, including core and MS extensions specific to Active Directory.
 * </dl>
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
    
    /**
     * This defines the organizational unit that contains groups.
     *
     * Normally "" to indicate the full LDAP search, but can be often narrowed down to
     * something like "ou=groups"
     *
     * @see FilterBasedLdapUserSearch
     */
    public final String groupSearchBase;

    /*
        Other configurations that are needed:

        group search base DN (relative to root DN)
        group search filter (uniquemember={1} seems like a reasonable default)
        group target (CN is a reasonable default)

        manager dn/password if anonyomus search is not allowed.

        See GF configuration at http://weblogs.java.net/blog/tchangu/archive/2007/01/ldap_security_r.html
        Geronimo configuration at http://cwiki.apache.org/GMOxDOC11/ldap-realm.html
     */

    /**
     * If non-null, we use this and {@link #managerPassword}
     * when binding to LDAP.
     *
     * This is necessary when LDAP doesn't support anonymous access.
     */
    public final String managerDN;

    /**
     * Scrambled password, used to first bind to LDAP.
     */
    private final String managerPassword;

    /**
     * Created in {@link #createSecurityComponents()}. Can be used to connect to LDAP.
     */
    private transient LdapTemplate ldapTemplate;

    @DataBoundConstructor
    public LDAPSecurityRealm(String server, String rootDN, String userSearchBase, String userSearch, String groupSearchBase, String managerDN, String managerPassword) {
        this.server = server.trim();
        if(Util.fixEmptyAndTrim(rootDN)==null)    rootDN=Util.fixNull(inferRootDN(server));
        this.rootDN = rootDN.trim();
        this.userSearchBase = userSearchBase.trim();
        if(Util.fixEmptyAndTrim(userSearch)==null)    userSearch="uid={0}";
        this.userSearch = userSearch.trim();
        this.groupSearchBase = Util.fixEmptyAndTrim(groupSearchBase);
        this.managerDN = Util.fixEmpty(managerDN);
        this.managerPassword = Scrambler.scramble(Util.fixEmpty(managerPassword));
    }

    public String getServerUrl() {
        return addPrefix(server);
    }

    /**
     * Infer the root DN.
     *
     * @return null if not found.
     */
    private String inferRootDN(String server) {
        try {
            Hashtable<String,String> props = new Hashtable<String,String>();
            if(managerDN!=null) {
                props.put(Context.SECURITY_PRINCIPAL,managerDN);
                props.put(Context.SECURITY_CREDENTIALS,getManagerPassword());
            }
            DirContext ctx = LdapCtxFactory.getLdapCtxInstance(getServerUrl()+'/', props);
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

    public String getManagerPassword() {
        return Scrambler.descramble(managerPassword);
    }

    public String getLDAPURL() {
        return getServerUrl()+'/'+Util.fixNull(rootDN);
    }

    public SecurityComponents createSecurityComponents() {
        Binding binding = new Binding();
        binding.setVariable("instance", this);

        BeanBuilder builder = new BeanBuilder();
        builder.parse(Hudson.getInstance().servletContext.getResourceAsStream("/WEB-INF/security/LDAPBindSecurityRealm.groovy"),binding);
        final WebApplicationContext appContext = builder.createApplicationContext();

        ldapTemplate = new LdapTemplate(findBean(InitialDirContextFactory.class, appContext));

        return new SecurityComponents(
            findBean(AuthenticationManager.class, appContext),
            new UserDetailsService() {
                final LdapUserSearch ldapSerach = findBean(LdapUserSearch.class, appContext);
                public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
                    try {
                        return ldapSerach.searchForUser(username);
                    } catch (LdapDataAccessException e) {
                        LOGGER.log(Level.WARNING, "Failed to search LDAP for username="+username,e);
                        throw new UserMayOrMayNotExistException(e.getMessage(),e);
                    }
                }
            });
    }

    @Override
    public GroupDetails loadGroupByGroupname(String groupname) throws UsernameNotFoundException, DataAccessException {
        // TODO: obtain a DN instead so that we can obtain multiple attributes later
        final Set<String> groups = (Set<String>)ldapTemplate.searchForSingleAttributeValues(groupSearchBase, GROUP_SEARCH,
                new String[]{groupname}, "cn");

        if(groups.isEmpty())
            throw new UsernameNotFoundException(groupname);

        return new GroupDetails() {
            public String getName() {
                return groups.iterator().next();
            }
        };
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
                LdapUserDetails details = (LdapUserDetails) hudson.getSecurityRealm().getSecurityComponents().userDetails.loadUserByUsername(u.getId());
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
        public String getDisplayName() {
            return Messages.LDAPSecurityRealm_DisplayName();
        }

        public void doServerCheck(StaplerRequest req, StaplerResponse rsp, @QueryParameter final String server,
        		@QueryParameter final String managerDN,
        		@QueryParameter final String managerPassword
        		) throws IOException, ServletException {
            new FormFieldValidator(req,rsp,true) {
                protected void check() throws IOException, ServletException {
                    try {
                        Hashtable<String,String> props = new Hashtable<String,String>();
                        if(managerDN!=null && managerDN.trim().length() > 0  && !"undefined".equals(managerDN)) {
                            props.put(Context.SECURITY_PRINCIPAL,managerDN);
                        }
                        if(managerPassword!=null && managerPassword.trim().length() > 0 && !"undefined".equals(managerPassword)) {
                            props.put(Context.SECURITY_CREDENTIALS,managerPassword);
                        }
                        DirContext ctx = LdapCtxFactory.getLdapCtxInstance(addPrefix(server)+'/', props);
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

    /**
     * If the given "server name" is just a host name (plus optional host name), add ldap:// prefix.
     * Otherwise assume it already contains the scheme, and leave it intact.
     */
    private static String addPrefix(String server) {
        if(server.contains("://"))  return server;
        else    return "ldap://"+server;
    }

    static {
        LIST.add(DESCRIPTOR);
    }

    private static final Logger LOGGER = Logger.getLogger(LDAPSecurityRealm.class.getName());

    /**
     * LDAP filter to look for groups by their names.
     *
     * "{0}" is the group name as given by the user.
     * See http://msdn.microsoft.com/en-us/library/aa746475(VS.85).aspx for the syntax by example.
     * WANTED: The specification of the syntax.
     */
    public static String GROUP_SEARCH = System.getProperty(LDAPSecurityRealm.class.getName()+".groupSearch",
            "(& (cn={0}) (| (objectclass=groupOfNames) (objectclass=groupOfUniqueNames) (objectclass=posixGroup)))");
}
