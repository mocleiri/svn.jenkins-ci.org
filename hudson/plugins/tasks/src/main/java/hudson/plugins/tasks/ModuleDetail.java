package hudson.plugins.tasks;

import hudson.model.AbstractBuild;
import hudson.plugins.tasks.util.SourceDetail;
import hudson.plugins.tasks.util.model.JavaPackage;
import hudson.plugins.tasks.util.model.MavenModule;
import hudson.plugins.tasks.util.model.Priority;
import hudson.plugins.tasks.util.model.WorkspaceFile;

import java.io.IOException;
import java.util.Collection;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Represents the details of a maven module.
 */
public class ModuleDetail extends AbstractTasksResult {
    /** Unique identifier of this class. */
    private static final long serialVersionUID = -3743047168363581305L;
    /** The selected module to show. */
    private final MavenModule module;

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
    public ModuleDetail(final AbstractBuild<?, ?> owner, final MavenModule module,
            final String high, final String normal, final String low) {
        super(owner, module.getAnnotations(), high, normal, low);

        this.module = module;
    }

    /** {@inheritDoc} */
    public String getDisplayName() {
        return module.getName();
    }

    /**
     * Gets the packages of this module that have open tasks.
     *
     * @return the modules
     */
    public Collection<JavaPackage> getPackages() {
        return module.getPackages();
    }

    /**
     * Gets the files of this module that have open tasks.
     *
     * @return the files
     */
    public Collection<WorkspaceFile> getFiles() {
        return module.getFiles();
    }

    /**
     * Returns a tooltip showing the distribution of priorities for the selected
     * package.
     *
     * @param packageName
     *            the package to show the distribution for
     * @return a tooltip showing the distribution of priorities
     */
    public String getToolTip(final String packageName) {
        return module.getPackage(packageName).getToolTip();
    }

    /**
     * Generates a PNG image for high/normal/low distribution of a java package.
     *
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @throws IOException
     *             in case of an error
     */
    public final void doPackageStatistics(final StaplerRequest request, final StaplerResponse response) throws IOException {
        createDetailGraph(request, response, module.getPackage(request.getParameter("package")), module.getAnnotationBound());
    }

    /**
     * Returns the dynamic result of this module detail view, which is either a
     * task detail object for a single workspace file or a package detail
     * object.
     *
     * @param link
     *            the link containing the path to the selected workspace file
     *            (or package)
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @return the dynamic result of the FindBugs analysis (detail page for a
     *         package).
     * @see #isSinglePackageModule()
     */
    public Object getDynamic(final String link, final StaplerRequest request, final StaplerResponse response) {
        if (isSinglePackageModule()) {
            return new SourceDetail(getOwner(), getAnnotation(link));
        }
        else {
            return new PackageDetail(getOwner(), module.getPackage(link),
                    getTags(Priority.HIGH), getTags(Priority.NORMAL), getTags(Priority.LOW));
        }
    }

    /**
     * Returns whether we only have a single module. In this case the module
     * statistics are suppressed and only the package statistics are shown.
     *
     * @return <code>true</code> for single module projects
     */
    public boolean isSinglePackageModule() {
        return module.getPackages().size() == 1;
    }
}
