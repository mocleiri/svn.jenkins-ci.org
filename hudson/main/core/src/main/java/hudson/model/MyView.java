package hudson.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * {@link View} that only contains projects for which the current user has access to.
 *
 * @since 1.220
 * @author Tom Huybrechts
 */
public class MyView extends View {

    private final Hudson owner;
    private String description;

    public MyView(Hudson owner) {
        this.owner = owner;
    }

    @Override
    public boolean contains(TopLevelItem item) {
        return (item instanceof Job) && ((Job) item).hasPermission(Hudson.ADMINISTER);
    }

    @Override
    public Item doCreateItem(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException {
        return owner.doCreateItem(req, rsp);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public TopLevelItem getItem(String name) {
        return owner.getItem(name);
    }

    @Override
    public Collection<TopLevelItem> getItems() {
        List<TopLevelItem> items = new ArrayList<TopLevelItem>();
        for (TopLevelItem item : owner.getItems()) {
            if ((item instanceof Job) && ((Job) item).hasPermission(Job.CONFIGURE)) {
                items.add(item);
            }
        }
        return Collections.unmodifiableList(items);
    }

    @Override
    public String getUrl() {
        return "view/" + getViewName() + "/";
    }

    @Override
    public String getViewName() {
        return getDisplayName();
    }

    public String getDisplayName() {
        return "My Projects";
    }

    public TopLevelItem getJob(String name) {
        return getItem(name);
    }

    /**
     * Accepts the new description.
     */
    public synchronized void doSubmitDescription(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        checkPermission(CONFIGURE);

        req.setCharacterEncoding("UTF-8");
        description = req.getParameter("description");
        owner.save();
        rsp.sendRedirect(".");  // go to the top page
    }
}
