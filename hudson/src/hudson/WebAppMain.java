package hudson;

import hudson.model.Hudson;
import hudson.triggers.Trigger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Entry point when Hudson is used as a webapp.
 *
 * @author Kohsuke Kawaguchi
 */
public class WebAppMain implements ServletContextListener {

    /**
     * Creates the sole instance of {@link Hudson} and register it to the {@link ServletContext}.
     */
    public void contextInitialized(ServletContextEvent event) {
        File home = getHomeDir(event);
        home.mkdirs();
        System.out.println("hudson home directory: "+home);

        try {
            event.getServletContext().setAttribute("app",new Hudson(home));
        } catch( IOException e ) {
            throw new Error(e);
        }

        new PluginManager(event.getServletContext());

        // set the version
        Properties props = new Properties();
        try {
            InputStream is = getClass().getResourceAsStream("hudson-version.properties");
            if(is!=null)
                props.load(is);
        } catch (IOException e) {
            e.printStackTrace(); // if the version properties is missing, that's OK.
        }
        Object ver = props.get("version");
        if(ver==null)   ver="?";
        event.getServletContext().setAttribute("version",ver);

        Trigger.init(); // start running trigger
    }

    /**
     * Determines the home directory for Hudson.
     *
     * People makes configuration mistakes, so we are trying to be nice
     * with those by doing {@link String#trim()}.
     */
    private File getHomeDir(ServletContextEvent event) {
        // check JNDI for the home directory first
        try {
            Context env = (Context) new InitialContext().lookup("java:comp/env");
            String value = (String) env.lookup("HUDSON_HOME");
            if(value!=null && value.trim().length()>0)
                return new File(value.trim());
        } catch (NamingException e) {
            // ignore
        }

        // look at the env var next
        String env = EnvVars.masterEnvVars.get("HUDSON_HOME");
        if(env!=null)
            return new File(env.trim());

        // finally check the system property
        String sysProp = System.getProperty("HUDSON_HOME");
        if(sysProp!=null)
            return new File(sysProp.trim());

        // otherwise pick a place by ourselves
        String root = event.getServletContext().getRealPath("/WEB-INF/workspace");
        if(root!=null)
            return new File(root.trim());

        // if for some reason we can't put it within the webapp, use home directory.
        return new File(new File(System.getProperty("user.home")),".hudson");
    }

    public void contextDestroyed(ServletContextEvent event) {
        Hudson instance = Hudson.getInstance();
        if(instance!=null)
            instance.cleanUp();
    }
}
