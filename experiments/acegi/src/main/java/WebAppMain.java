import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author Kohsuke Kawaguchi
 */
public class WebAppMain implements ServletContextListener {
    public static Main MAIN;

    public static final AuthenticationManagerProxy AUTHENTICATION_MANAGER = new AuthenticationManagerProxy();

    public void contextInitialized(ServletContextEvent sce) {
        try {
            MAIN = new Main();
        } catch (Exception e) {
            throw new Error(e);
        }
        sce.getServletContext().setAttribute("app", MAIN);
    }

    public void contextDestroyed(ServletContextEvent sce) {
    }
}
