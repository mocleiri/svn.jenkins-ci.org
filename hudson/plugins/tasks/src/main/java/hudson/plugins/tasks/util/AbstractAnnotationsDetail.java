package hudson.plugins.tasks.util;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;
import hudson.plugins.tasks.util.model.AnnotationContainer;
import hudson.plugins.tasks.util.model.AnnotationProvider;
import hudson.plugins.tasks.util.model.FileAnnotation;
import hudson.plugins.tasks.util.model.Priority;
import hudson.util.ChartUtil;

import java.io.IOException;
import java.util.Collection;

import org.jfree.chart.JFreeChart;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Base class for annotation detail objects. Instances of this class could be used for
 * Hudson Stapler objects that contain a subset of annotations.
 *
 * @author Ulli Hafner
 */
public abstract class AbstractAnnotationsDetail extends AnnotationContainer implements ModelObject {
    /** Unique identifier of this class. */
    private static final long serialVersionUID = 1750266351592937774L;
    /** Current build as owner of this object. */
    private final AbstractBuild<?, ?> owner;

    /**
     * Creates a new instance of <code>AbstractWarningsDetail</code>.
     *
     * @param owner
     *            current build as owner of this object.
     * @param annotations
     *            the set of warnings represented by this object
     * @param name
     *            the name of this object
     */
    public AbstractAnnotationsDetail(final AbstractBuild<?, ?> owner, final Collection<FileAnnotation> annotations, final String name, final Hierarchy hierarchy) {
        super(name, hierarchy);
        this.owner = owner;

        addAnnotations(annotations);
    }

    /**
     * Returns the header for the detail screen.
     *
     * @return the header
     */
    public String getHeader() {
        return getName() + " - " + getDisplayName();
    }

    /**
     * Returns the build as owner of this object.
     *
     * @return the owner
     */
    public final AbstractBuild<?, ?> getOwner() {
        return owner;
    }

    /**
     * Returns whether this build is the last available build.
     *
     * @return <code>true</code> if this build is the last available build
     */
    public final boolean isCurrent() {
        return owner.getProject().getLastBuild().number == owner.number;
    }

    /**
     * Creates a detail graph for the specified detail object.
     *
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @param detailObject
     *            the detail object to compute the graph for
     * @param upperBound
     *            the upper bound of all tasks
     * @throws IOException
     *             in case of an error
     */
    protected final void createDetailGraph(final StaplerRequest request, final StaplerResponse response,
            final AnnotationProvider detailObject, final int upperBound) throws IOException {
        if (ChartUtil.awtProblem) {
            response.sendRedirect2(request.getContextPath() + "/images/headless.png");
            return;
        }
        JFreeChart chart = ChartBuilder.createHighNormalLowChart(
                detailObject.getNumberOfAnnotations(Priority.HIGH),
                detailObject.getNumberOfAnnotations(Priority.NORMAL),
                detailObject.getNumberOfAnnotations(Priority.LOW), upperBound);
        ChartUtil.generateGraph(request, response, chart, 400, 20);
    }

    /**
     * Returns a localized priority name.
     *
     * @param priorityName
     *            priority as String value
     * @return localized priority name
     */
    public String getLocalizedPriority(final String priorityName) {
        return Priority.fromString(priorityName).getLongLocalizedString();
    }

    /**
     * Returns the dynamic result of this module detail view. Depending on the
     * number of packages, one of the following detail objects is returned:
     * <ul>
     * <li>A detail object for a single workspace file (if the module contains
     * only one package).</li>
     * <li>A package detail object for a specified package (in any other case).</li>
     * </ul>
     *
     * @param link
     *            the link to identify the sub page to show
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @return the dynamic result of this module detail view
     */
    public Object getDynamic(final String link, final StaplerRequest request, final StaplerResponse response) {
        return new DetailBuilder().createDetails(link, owner, getContainer(), getDisplayName());
    }

    /**
     * Generates a PNG image for high/normal/low distribution of the specified object.
     *
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @throws IOException
     *             in case of an error
     */
    public void doStatistics(final StaplerRequest request, final StaplerResponse response) throws IOException {
        new ChartRenderer().doStatistics(request, response, getContainer());
    }

    /**
     * Returns all possible priorities.
     *
     * @return all priorities
     */
    public Priority[] getPriorities() {
        return Priority.values();
    }
}
