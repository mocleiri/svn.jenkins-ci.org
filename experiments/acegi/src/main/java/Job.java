import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.acegisecurity.context.SecurityContextHolder;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Job implements AccessControlledObject {
    public final String name;
    private final ACL acl = new ACL(CLASS_ACL);

    public Job(String name) {
        this.name = name;
    }

    public ACL getACL() {
        return acl;
    }

    public void doBuild(StaplerRequest req, StaplerResponse rsp) throws IOException {
        acl.checkPermission(BUILD);
        rsp.getWriter().println("Build started. You are "+ SecurityContextHolder.getContext().getAuthentication());
    }

    public static final Permission BROWSE = new Permission("Browse");
    public static final Permission BUILD = new Permission("Perform build");

    // TODO: we want to hide this somewhat
    // perhaps we can create it behind the scene per every class?
    public static final ACL CLASS_ACL = new ACL(null);
}
