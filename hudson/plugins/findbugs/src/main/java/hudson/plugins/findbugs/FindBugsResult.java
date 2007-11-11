package hudson.plugins.findbugs;

import hudson.model.AbstractBuild;
import hudson.plugins.findbugs.util.SourceDetail;
import hudson.util.IOException2;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Represents the results of the FindBugs analysis. One instance of this class is persisted for
 * each build via an XML file.
 *
 * @author Ulli Hafner
 */
// CHECKSTYLE:OFF
public class FindBugsResult extends AbstractWarningsDetail {
// CHECKSTYLE:ON
    /** No result at all. */
    @java.lang.SuppressWarnings("unchecked")
    private static final Set<Warning> EMPTY_SET = Collections.EMPTY_SET;
    /** Unique identifier of this class. */
    private static final long serialVersionUID = 2768250056765266658L;
    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(FindBugsResult.class.getName());
    /** Difference between this and the previous build. */
    private final int delta;
    /** The parsed FindBugs result. */
    @SuppressWarnings("Se")
    private transient WeakReference<JavaProject> project;
    /** All new warnings in the current build.*/
    @SuppressWarnings("Se")
    private transient WeakReference<Set<Warning>> newWarnings;
    /** All fixed warnings in the current build.*/
    @SuppressWarnings("Se")
    private transient WeakReference<Set<Warning>> fixedWarnings;
    /** The number of warnings in this build. */
    private final int numberOfWarnings;
    /** The number of new warnings in this build. */
    private final int numberOfNewWarnings;
    /** The number of fixed warnings in this build. */
    private final int numberOfFixedWarnings;
    /** The number of low priority warnings in this build. */
    private int low;
    /** The number of normal priority warnings in this build. */
    private int normal;
    /** The number of high priority warnings in this build. */
    private int high;

    /**
     * Creates a new instance of <code>FindBugsResult</code>.
     *
     * @param build
     *            the current build as owner of this action
     * @param project
     *            the parsed FindBugs result
     */
    public FindBugsResult(final AbstractBuild<?,?> build, final JavaProject project) {
        this(build, project, new JavaProject());
    }

    /**
     * Creates a new instance of <code>FindBugsResult</code>.
     * @param build
     *            the current build as owner of this action
     * @param project
     *            the parsed FindBugs result
     * @param previousProject the parsed FindBugs result of the previous build
     */
    public FindBugsResult(final AbstractBuild<?, ?> build, final JavaProject project, final JavaProject previousProject) {
        super(build, project.getWarnings());
        numberOfWarnings = project.getNumberOfWarnings();

        this.project = new WeakReference<JavaProject>(project);
        delta = project.getNumberOfWarnings() - previousProject.getNumberOfWarnings();

        Set<Warning> allWarnings = project.getWarnings();

        Set<Warning> warnings = WarningDifferencer.getNewWarnings(allWarnings, previousProject.getWarnings());
        numberOfNewWarnings = warnings.size();
        newWarnings = new WeakReference<Set<Warning>>(warnings);

        warnings = WarningDifferencer.getFixedWarnings(allWarnings, previousProject.getWarnings());
        numberOfFixedWarnings = warnings.size();
        fixedWarnings = new WeakReference<Set<Warning>>(warnings);

        computePriorities(allWarnings);
    }

    /** {@inheritDoc} */
    public String getDisplayName() {
        return "FindBugs Result";
    }

    /**
     * Gets the number of warnings.
     *
     * @return the number of warnings
     */
    @Override
    public int getNumberOfWarnings() {
        return numberOfWarnings;
    }

    /**
     * Gets the number of fixed warnings.
     *
     * @return the number of fixed warnings
     */
    public int getNumberOfFixedWarnings() {
        return numberOfFixedWarnings;
    }

    /**
     * Gets the number of new warnings.
     *
     * @return the number of new warnings
     */
    public int getNumberOfNewWarnings() {
        return numberOfNewWarnings;
    }

    /**
     * Returns the delta.
     *
     * @return the delta
     */
    public int getDelta() {
        return delta;
    }

    /**
     * Returns the associated project of this result.
     *
     * @return the associated project of this result.
     */
    public JavaProject getProject() {
        try {
            if (project == null) {
                loadResult();
            }
            JavaProject result = project.get();
            if (result == null) {
                loadResult();
            }
            return project.get();
        }
        catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to load FindBugs files.", exception);
        }
        catch (InterruptedException exception) {
            LOGGER.log(Level.WARNING, "Failed to load FindBugs files: operation has been canceled.", exception);
        }
        return new JavaProject();
    }

    /**
     * Returns the new warnings of this build.
     *
     * @return the new warnings of this build.
     */
    public Set<Warning> getNewWarnings() {
        try {
            if (newWarnings == null) {
                loadPreviousResult();
            }
            Set<Warning> result = newWarnings.get();
            if (result == null) {
                loadPreviousResult();
            }
            return newWarnings.get();
        }
        catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to load FindBugs files.", exception);
        }
        catch (InterruptedException exception) {
            LOGGER.log(Level.WARNING, "Failed to load FindBugs files: operation has been canceled.", exception);
        }
        return EMPTY_SET;
    }

    /**
     * Returns the fixed warnings of this build.
     *
     * @return the fixed warnings of this build.
     */
    public Set<Warning> getFixedWarnings() {
        try {
            if (fixedWarnings == null) {
                loadPreviousResult();
            }
            Set<Warning> result = fixedWarnings.get();
            if (result == null) {
                loadPreviousResult();
            }
            return fixedWarnings.get();
        }
        catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to load FindBugs files.", exception);
        }
        catch (InterruptedException exception) {
            LOGGER.log(Level.WARNING, "Failed to load FindBugs files: operation has been canceled.", exception);
        }
        return EMPTY_SET;
    }

    /**
     * Loads the FindBugs results and wraps them in a weak reference that might
     * get removed by the garbage collector.
     *
     * @throws IOException if the files could not be read
     * @throws InterruptedException if the operation has been canceled
     */
    private void loadResult() throws IOException, InterruptedException {
        try {
            FindBugsCounter findBugsCounter = new FindBugsCounter(getOwner());
            JavaProject result = findBugsCounter.findBugs();
            if (isCurrent()) {
                findBugsCounter.restoreMapping(result);
            }
            computeWarningMapping(result.getWarnings());
            computePriorities(result.getWarnings());
            project = new WeakReference<JavaProject>(result);
        }
        catch (SAXException exception) {
            throw new IOException2(exception);
        }
    }

    /**
     * Loads the FindBugs results and the result of the previous build and wraps
     * them in a weak reference that might get removed by the garbage collector.
     *
     * @throws IOException
     *             if the files could not be read
     * @throws InterruptedException
     *             if the operation has been canceled
     */
    private void loadPreviousResult() throws IOException, InterruptedException {
        loadResult();

        if (hasPreviousResult()) {
            newWarnings = new WeakReference<Set<Warning>>(
                    WarningDifferencer.getNewWarnings(getProject().getWarnings(), getPreviousResult().getWarnings()));
        }
        else {
            newWarnings = new WeakReference<Set<Warning>>(getProject().getWarnings());
        }
        if (hasPreviousResult()) {
            fixedWarnings = new WeakReference<Set<Warning>>(
                    WarningDifferencer.getFixedWarnings(getProject().getWarnings(), getPreviousResult().getWarnings()));
        }
        else {
            fixedWarnings = new WeakReference<Set<Warning>>(EMPTY_SET);
        }
    }

    /**
     * Returns the dynamic result of the FindBugs analysis (a detail page for a
     * module, package or warnings file or a detail object for new or fixed
     * warnings).
     *
     * @param link
     *            the link to identify the sub page to show
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @return the dynamic result of the FindBugs analysis (detail page for a
     *         package).
     */
    public Object getDynamic(final String link, final StaplerRequest request, final StaplerResponse response) {
        if ("fixed".equals(link)) {
            return new FixedWarningsDetail(getOwner(), getFixedWarnings());
        }
        else if ("new".equals(link)) {
            return new NewWarningsDetail(getOwner(), getNewWarnings());
        }
        else {
            if (isSingleModuleProject()) {
                if (isSinglePackageProject()) {
                    return new SourceDetail(getOwner(), getWarning(link));
                }
                else {
                    return new PackageDetail(getOwner(), getProject().getModules().iterator().next().getPackage(link));
                }
            }
            else {
                return new ModuleDetail(getOwner(), getProject().getModule(link));
            }
        }
    }

    /**
     * Returns the packages of this project.
     *
     * @return the packages of this project
     */
    public Set<JavaPackage> getPackages() {
        return getProject().getPackages();
    }

    /**
     * Returns the modules of this project.
     *
     * @return the modules of this project
     */
    public Collection<Module> getModules() {
        return getProject().getModules();
    }

    /**
     * Returns the warnings of this project.
     *
     * @return the warnings of this project
     */
    @Override
    public Set<Warning> getWarnings() {
        return getProject().getWarnings();
    }

    /**
     * Returns whether this project contains just one maven module. In this case
     * we show package statistics instead of module statistics.
     *
     * @return <code>true</code> if this project contains just one maven
     *         module
     */
    public boolean isSingleModuleProject() {
        return getProject().getModules().size() == 1;
    }

    /**
     * Returns whether we only have a single package. In this case the module
     * and package statistics are suppressed and only the tasks are shown.
     *
     * @return <code>true</code> for single module projects
     */
    public boolean isSinglePackageProject() {
        return isSingleModuleProject() && getProject().getPackages().size() == 1;
    }

    /**
     * Returns the number of warnings of the specified package in the previous build.
     *
     * @param packageName
     *            the package to return the warnings for
     * @return number of warnings of the specified package.
     */
    public int getPreviousNumberOfWarnings(final String packageName) {
        JavaProject previousResult = getPreviousResult();
        if (previousResult != null) {
            return previousResult.getNumberOfWarnings(packageName);
        }
        return 0;
    }

    /**
     * Returns the results of the previous build.
     *
     * @return the result of the previous build, or <code>null</code> if no
     *         such build exists
     */
    public JavaProject getPreviousResult() {
        FindBugsResultAction action = getOwner().getAction(FindBugsResultAction.class);
        if (action.hasPreviousResultAction()) {
            return action.getPreviousResultAction().getResult().getProject();
        }
        else {
            return null;
        }
    }

    /**
     * Returns whether a previous build result exists.
     *
     * @return <code>true</code> if a previous build result exists.
     */
    public boolean hasPreviousResult() {
        return getOwner().getAction(FindBugsResultAction.class).hasPreviousResultAction();
    }

    /**
     * Computes the low, normal and high priority count.
     *
     * @param allWarnings
     *            all project warnings
     */
    private void computePriorities(final Set<Warning> allWarnings) {
        low = WarningDifferencer.countLowPriorityWarnings(allWarnings);
        normal = WarningDifferencer.countNormalPriorityWarnings(allWarnings);
        high = WarningDifferencer.countHighPriorityWarnings(allWarnings);
    }

    /**
     * Returns the total number of warnings with priority LOW.
     *
     * @return the total number of warnings with priority LOW
     */
    @Override
    public int getNumberOfLowWarnings() {
        return low;
    }

    /**
     * Returns the total number of warnings with priority HIGH.
     *
     * @return the total number of warnings with priority HIGH
     */
    @Override
    public int getNumberOfHighWarnings() {
        return high;
    }

    /**
     * Returns the total number of warnings with priority NORMAL.
     *
     * @return the total number of warnings with priority NORMAL
     */
    @Override
    public int getNumberOfNormalWarnings() {
        return normal;
    }

    /**
     * Generates a PNG image for high/normal/low distribution of a maven module.
     *
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @throws IOException
     *             in case of an error
     */
    public final void doModuleStatistics(final StaplerRequest request, final StaplerResponse response) throws IOException {
        createDetailGraph(request, response, getProject().getModule(request.getParameter("module")), getProject().getWarningBound());
    }

    /**
     * Generates a PNG image for high/normal/low distribution of a Java package.
     *
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @throws IOException
     *             in case of an error
     */
    public final void doPackageStatistics(final StaplerRequest request, final StaplerResponse response) throws IOException {
        Module module = getModules().iterator().next();
        createDetailGraph(request, response, module.getPackage(request.getParameter("package")), module.getWarningBound());
    }
}
