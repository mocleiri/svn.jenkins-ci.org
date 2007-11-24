import grails.spring.BeanBuilder;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.acegisecurity.AccessDeniedException;
import org.acegisecurity.Authentication;
import org.acegisecurity.acls.sid.PrincipalSid;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.ProviderManager;
import org.acegisecurity.providers.anonymous.AnonymousAuthenticationToken;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    // mock domain objects
    public Map<String,Job> jobs = new HashMap<String,Job>();

    public static final Permission BROWSE = new Permission("Browse");
    public static final Permission CREATE_JOB = new Permission("Create a new job");

    // seucrity configuration
    public ProviderManager authenticationManager;

    public Main() throws Exception {
        // mock data set up
        mockDataSetup();

        Binding binding = new Binding();
        BeanBuilder builder = new BeanBuilder();
        binding.setVariable("builder", builder);
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setScriptBaseClass(ClosureScript.class.getName());
        GroovyShell shell = new GroovyShell(binding,cc);

        ClosureScript s = (ClosureScript)shell.parse(getClass().getResourceAsStream("authentication.groovy"));
        s.setDelegate(builder);
        s.run();
        WebApplicationContext ac = builder.createApplicationContext();
        authenticationManager = (ProviderManager) ac.getBean("authenticationManager");
    }

    private void mockDataSetup() {
        Job a = new Job("a");
        jobs.put("a", a);
        Job b = new Job("b");
        jobs.put("b", b);

        // alice can build everything but bob can only build b
        Job.CLASS_ACL.add(new PrincipalSid("alice"),Job.BUILD,true);
        b.getACL().add(new PrincipalSid("bob"),Job.BUILD,true);
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
}
