package hudson.plugins.perforce;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.server.IServer;

import static hudson.Util.fixEmpty;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.scm.AbstractScmTagAction;
import hudson.util.FormFieldValidator;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link Action} that lets people create tag for the given build.
 *
 * @author Mike Wille
 */
public class PerforceTagAction extends AbstractScmTagAction {
    private final PerforceDepot depot;
    private final int changeNumber;
    private final String view;

    private String tag;
    private String desc;

    /**
     * Constructs a new tag action for tagging the build at a given change id
     */
    public PerforceTagAction(AbstractBuild build, PerforceDepot depot, int changeNumber, String views) {
        super(build);
        this.depot = depot;
        this.changeNumber = changeNumber;
        this.view = views;
    }

    /**
     * Constructs a new tag action for tagging the build at a given label
     */
    public PerforceTagAction(AbstractBuild build, PerforceDepot depot, String label, String views) {
        super(build);
        this.depot = depot;
        this.changeNumber = IChangelist.UNKNOWN;  // -1
        this.tag = label;
        this.view = views;
    }

    public int getChangeNumber() {
        return changeNumber;
    }

    public String getIconFileName() {
        if (tag == null && !Hudson.isAdmin())
            return null;
        return "save.gif";
    }

    public String getDisplayName() {
        return isTagged() ? "Perforce Label" : "Label This Build";
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDescription() {
        return desc;
    }

    public void setDescription(String desc) {
        this.desc = desc;
    }

    /**
     * Returns true if this build has already been tagged at least once.
     */
    public boolean isTagged() {
        return tag != null;
    }

    /**
     * Checks to see if the user entered tag matches any Perforce restrictions.
     */
    public String isInvalidTag(String tag) {
        Pattern spaces = Pattern.compile("\\s{1,}");
        Matcher m = spaces.matcher(tag);
        if (m.find()) {
            return "Spaces are not allowed.";
        }
        return null;
    }

    /**
     * Checks if the value is a valid Perforce tag (label) name.
     */
    public synchronized void doCheckTag(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        new FormFieldValidator(req, rsp, false) {
            protected void check() throws IOException, ServletException {
                String tag = fixEmpty(request.getParameter("value")).trim();
                if (tag == null) {// nothing entered yet
                    ok();
                    return;
                }
                error(isInvalidTag(tag));
            }
        }.check();
    }

    /**
     * Invoked to actually tag the workspace.
     * TODO(CQ) fix existing bug where this code assumes that views is a depot-only list of
     * mappings, not a list of depot-client pairs.
     */
    public synchronized void doSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        if (!Hudson.adminCheck(req, rsp))
            return;

        tag = req.getParameter("name");
        desc = req.getParameter("desc");
        try {
            IServer server = P4jUtil.newServer(depot.getPort(), "prog", "ver", depot.getUser(), depot.getPassword());
            ILabel label = P4jUtil.newLabel(server, tag, desc, changeNumber, view.split("\n"));
            server.updateLabel(label);
        } catch (Exception e) {
            tag = null;
            desc = null;
            e.printStackTrace();
            throw new IOException("Failed to issue perforce label: " + e.getMessage());
        }
        build.save();
        rsp.sendRedirect(".");
    }
}
