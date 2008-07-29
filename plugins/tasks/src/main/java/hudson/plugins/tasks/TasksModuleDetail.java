package hudson.plugins.tasks;

import hudson.model.AbstractBuild;
import hudson.plugins.tasks.util.ModuleDetail;
import hudson.plugins.tasks.util.model.MavenModule;
import hudson.plugins.tasks.util.model.Priority;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Represents the details of a maven module.
 *
 * @author Ulli Hafner
 */
public class TasksModuleDetail extends ModuleDetail {
    /** Unique identifier of this class. */
    private static final long serialVersionUID = -3743047168363581305L;
    /** Handles the task tags. */
    private final TaskTagsHandler taskTagsHandler;

    /**
     * Creates a new instance of <code>ModuleDetail</code>.
     *
     * @param owner
     *            the current build as owner of this result object
     * @param high
     *            tag identifiers indicating high priority
     * @param normal
     *            tag identifiers indicating normal priority
     * @param low
     *            tag identifiers indicating low priority
     * @param module
     *            the selected module to show
     */
    public TasksModuleDetail(final AbstractBuild<?, ?> owner, final MavenModule module, final String header,
            final String high, final String normal, final String low) {
        super(owner, module, header);

        taskTagsHandler = new TaskTagsHandler(high, normal, low, module);
    }

    /**
     * Returns the dynamic result of this module detail view, which is either a
     * task detail object for a single workspace file or a package detail
     * object.
     *
     * @param link
     *            the link containing the path to the selected workspace file
     *            (or package)
     * @return the dynamic result of the FindBugs analysis (detail page for a
     *         package).
     */
    @Override
    public Object getDynamic(final String link, final StaplerRequest request, final StaplerResponse response) {
        if (link.startsWith("package.")) {
            return new TasksPackageDetail(getOwner(), getPackage(StringUtils.substringAfter(link, "package.")), getDisplayName(),
                    getTags(Priority.HIGH), getTags(Priority.NORMAL), getTags(Priority.LOW));
        }
        return super.getDynamic(link, request, response);
    }

    // CHECKSTYLE:OFF - generated delegate -

    public Collection<String> getAvailablePriorities() {
        return taskTagsHandler.getAvailablePriorities();
    }

    @Override
    public Priority[] getPriorities() {
        return taskTagsHandler.getPriorities();
    }

    public final String getTags(final Priority priority) {
        return taskTagsHandler.getTags(priority);
    }

    public final String getTags(final String priority) {
        return taskTagsHandler.getTags(priority);
    }
}
