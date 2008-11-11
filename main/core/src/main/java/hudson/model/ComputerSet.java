package hudson.model;

import hudson.Util;
import hudson.slaves.NodeDescriptor;
import hudson.model.Descriptor.FormException;
import hudson.node_monitors.NodeMonitor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import java.io.IOException;
import java.text.ParseException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * Serves as the top of {@link Computer}s in the URL hierarchy.
 * <p>
 * Getter methods are prefixed with '_' to avoid collision with computer names.
 *
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public final class ComputerSet extends AbstractModelObject {
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

    /**
     * Gets all the slave names.
     */
    public List<String> get_slaveNames() {
        return new AbstractList<String>() {
            final List<Node> nodes = Hudson.getInstance().getNodes();

            public String get(int index) {
                return nodes.get(index).getNodeName();
            }

            public int size() {
                return nodes.size();
            }
        };
    }

    /**
     * Number of total {@link Executor}s that belong to this label that are functioning.
     * <p>
     * This excludes executors that belong to offline nodes.
     */
    @Exported
    public int getTotalExecutors() {
        int r=0;
        for (Computer c : get_all()) {
            if(c.isOnline())
                r += c.countExecutors();
        }
        return r;
    }

    /**
     * Number of busy {@link Executor}s that are carrying out some work right now.
     */
    @Exported
    public int getBusyExecutors() {
        int r=0;
        for (Computer c : get_all()) {
            if(c.isOnline())
                r += c.countBusy();
        }
        return r;
    }

    public String getSearchUrl() {
        return "/computers/";
    }

    public Computer getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        return Hudson.getInstance().getComputer(token);
    }

    public void do_launchAll(StaplerRequest req, StaplerResponse rsp) throws IOException {
        for(Computer c : get_all()) {
            if(c.isLaunchSupported())
                continue;
            c.connect(true);
        }
        rsp.sendRedirect(".");
    }

    /**
     * Triggers the schedule update now.
     *
     * TODO: ajax on the client side to wait until the update completion might be nice.
     */
    public void doUpdateNow( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
        
        for (NodeMonitor nodeMonitor : NodeMonitor.getAll()) {
            Thread t = nodeMonitor.triggerUpdate();
            t.setName(nodeMonitor.getColumnCaption());
        }
        rsp.forwardToPreviousPage(req);
    }

    /**
     * First check point in creating a new slave.
     */
    public synchronized void doCreateItem( StaplerRequest req, StaplerResponse rsp,
                                           @QueryParameter("name") String name, @QueryParameter("mode") String mode,
                                           @QueryParameter("from") String from ) throws IOException, ServletException {
        final Hudson app = Hudson.getInstance();
        app.checkPermission(Hudson.CONFIGURE);  // TODO: new permission?

        if (checkName(req, rsp, name)) return;

        if(mode!=null && mode.equals("copy")) {
            Node src = app.getNode(from);
            if(src==null) {
                rsp.setStatus(SC_BAD_REQUEST);
                if(Util.fixEmpty(from)==null)
                    sendError(Messages.ComputerSet_SpecifySlaveToCopy(),req,rsp);
                else
                    sendError(Messages.ComputerSet_NoSuchSlave(from),req,rsp);
                return;
            }

            // copy through XStream
            String xml = Hudson.XSTREAM.toXML(src);
            Node result = (Node)Hudson.XSTREAM.fromXML(xml);
            result.setNodeName(name);

            app.addNode(result);

            // send the browser to the config page
            rsp.sendRedirect2(result.getNodeName()+"/configure");
        } else {
            // proceed to step 2
            if(mode==null) {
                rsp.sendError(SC_BAD_REQUEST);
                return;
            }

            req.setAttribute("descriptor", NodeDescriptor.ALL.find(mode));
            req.getView(this,"_new.jelly").forward(req,rsp);
        }
    }

    /**
     * Really creates a new slave.
     */
    public synchronized void doDoCreateItem( StaplerRequest req, StaplerResponse rsp,
                                           @QueryParameter("name") String name,
                                           @QueryParameter("type") String type ) throws IOException, ServletException {
        try {
            final Hudson app = Hudson.getInstance();
            app.checkPermission(Hudson.CONFIGURE);  // TODO: new permission?

            if (checkName(req, rsp, name)) return;

            Node result = NodeDescriptor.ALL.find(type).newInstance(req, req.getSubmittedForm());
            app.addNode(result);

            // take the user back to the slave list top page
            rsp.sendRedirect2(".");
        } catch (FormException e) {
            sendError(e,req,rsp);
        }
    }

    /**
     * Makes sure that the given name is good as a slave name.
     */
    private boolean checkName(StaplerRequest req, StaplerResponse rsp, String name) throws IOException, ServletException {
        if(name==null) {
            rsp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Query parameter 'name' is required");
            return true;
        }
        name = name.trim();

        try {
            Hudson.checkGoodName(name);
        } catch (ParseException e) {
            rsp.setStatus(SC_BAD_REQUEST);
            sendError(e,req,rsp);
            return true;
        }

        if(Hudson.getInstance().getNode(name)!=null) {
            rsp.setStatus(SC_BAD_REQUEST);
            sendError(Messages.ComputerSet_SlaveAlreadyExists(name),req,rsp);
            return true;
        }
        return false;
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
