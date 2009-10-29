package test;

import com.sun.security.auth.callback.TextCallbackHandler;
import com.sun.security.auth.module.Krb5LoginModule;

import javax.security.auth.Subject;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.Context;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Hashtable;

/**
 * Testing programatic Kerberos authentication from Java.
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception {
        // http://bmbpcu36.leeds.ac.uk/~david/docs/guide/jndi/jndi-dns.html
        // explains how one can query DNS through JDNI,
        // but this is missing the key part, namely to use the system-defined DNS servers.
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        // To query with default system-configured DNS servers, skip the next line
        env.put(Context.PROVIDER_URL, "dns://home.kohsuke.org");

        // locate  server
        DirContext ictx = new InitialDirContext(env);
        Attributes attributes = ictx.getAttributes("_kerberos._tcp.home.kohsuke.org", new String[]{"SRV"});
        Attribute a = attributes.get("SRV");
        for(int i=0; i<a.size(); i++)
            System.out.println(a.get(i).toString());

        System.out.println(attributes);

        // TODO: is there any way to avoid global pollution?
        System.setProperty("java.security.krb5.realm","home.kohsuke.org".toUpperCase());
        System.setProperty("java.security.krb5.kdc","home.kohsuke.org"); // can have multiple entries separated by ' ', according to sun.security.krb5.Config
        System.setProperty("sun.security.krb5.debug","true");

        Map options = new HashMap();
        options.put("debug","true");
        Krb5LoginModule login = new Krb5LoginModule();

        Subject subject = new Subject();
        login.initialize(subject,new TextCallbackHandler(),
                Collections.<String,Object>emptyMap(), options);
        login.login();
        login.commit();

        System.out.println(subject);

        // but I can also just access LDAP with user=kohsuke@HOME.KOHSUKE.ORG and password,
        // meaning I don't need to talk to Kerberos if I just wanted to do LDAP.
    }
}
