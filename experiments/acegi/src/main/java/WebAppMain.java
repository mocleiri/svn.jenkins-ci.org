import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;

/**
 * @author Kohsuke Kawaguchi
 */
public class WebAppMain implements ServletContextListener {
    public static final Main MAIN;

    static {
        try {
            MAIN = new Main();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().setAttribute("app", MAIN);
    }

    public void contextDestroyed(ServletContextEvent sce) {
    }
}
