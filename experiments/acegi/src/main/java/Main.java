import grails.spring.BeanBuilder;
import org.acegisecurity.AccessDeniedException;
import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.acls.sid.PrincipalSid;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.anonymous.AnonymousAuthenticationToken;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main implements StaplerProxy {
    // mock domain objects
    public Map<String,Job> jobs = new HashMap<String,Job>();

    public static final Permission BROWSE = new Permission(Main.class,"Browse");
    public static final Permission CREATE_JOB = new Permission(Main.class,"Create a new job");

    // seucrity configuration
    public WebApplicationContext context;

    public Main() throws Exception {
        // mock data set up
        mockDataSetup();

        BeanBuilder builder = new BeanBuilder();
        builder.parse(getClass().getResourceAsStream("authentication.groovy"));
        context = builder.createApplicationContext();
        WebAppMain.AUTHENTICATION_MANAGER.setManager(
            (AuthenticationManager) context.getBean("authenticationManager"));
    }

    private void mockDataSetup() {
        Job a = new Job("a");
        jobs.put("a", a);
        Job b = new Job("b");
        jobs.put("b", b);

        // alice can build everything but bob can only build b
        Job.CLASS_ACL.add(new PrincipalSid("alice"),Job.BUILD,true);
        b.getACL().add(new PrincipalSid("bob"),Job.BUILD,true);

        // need to be authenticated to browse
        Job.CLASS_ACL.add(ACL.EVERYONE,Main.BROWSE,true);
        Job.CLASS_ACL.add(ACL.ANONYMOUS,Main.BROWSE,false);
    }

    public List<Permission> getAllPermissions() {
        return Permission.getAll();
    }

    public Object getTarget() {
        if(Stapler.getCurrentRequest().getRestOfPath().equals("/login"))
            return this;    // anonymous access allowed for login
        Job.CLASS_ACL.checkPermission(BROWSE);
        return this;
    }

    public void doSecured(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if(a==null || a instanceof AnonymousAuthenticationToken) { // TODO: replace by a proper authorization
            /* this won't initiate basic authentication
            rsp.sendError(rsp.SC_UNAUTHORIZED);
            */

            throw new AccessDeniedException("This page is secured");
        } else {
            rsp.getWriter().println("Secret information");
        }
    }

    public void doLogout(StaplerRequest req, StaplerResponse rsp) throws IOException {
        SecurityContextHolder.clearContext();
        rsp.getWriter().println("Done");
    }

    public String getWho() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if(a==null) return "null";
        return a.getName();
    }
}
