package hudson.model;

import hudson.model.Descriptor.FormException;
import hudson.node_monitors.NodeMonitor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.export.Exported;

/**
 * Serves as the top of {@link Computer}s in the URL hierarchy.
 * <p>
 * Getter methods are prefixed with '_' to avoid collision with computer names.
 *
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public final class ComputerSet implements ModelObject {
    private static final List<NodeMonitor> monitors;

    @Exported
    public String getDisplayName() {
        return "nodes";
    }

    public static List<NodeMonitor> get_monitors() {
        return monitors;
    }

    @Exported(name="computer",inline=true)
    public Computer[] get_all() {
        return Hudson.getInstance().getComputers();
    }

    public Computer getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        return Hudson.getInstance().getComputer(token);
    }

    public void do_launchAll(StaplerRequest req, StaplerResponse rsp) throws IOException {
        for(Computer c : get_all()) {
            if(c.isLaunchSupported())
                continue;
            c.launch();
        }
        rsp.sendRedirect(".");
    }

    public Api getApi() {
        return new Api(this);
    }

    /**
     * Just to force the execution of the static initializer.
     */
    public static void initialize() {}

    static {
        // create all instances
        ArrayList<NodeMonitor> r = new ArrayList<NodeMonitor>();
        for (Descriptor<NodeMonitor> d : NodeMonitor.LIST)
            try {
                r.add(d.newInstance(null,null));
            } catch (FormException e) {
                // so far impossible. TODO: report
            }
        monitors = r;
    }
}
