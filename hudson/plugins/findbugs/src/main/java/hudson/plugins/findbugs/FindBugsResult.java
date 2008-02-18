package hudson.plugins.findbugs;

import hudson.XmlFile;
import hudson.model.AbstractBuild;
import hudson.plugins.findbugs.model.AnnotationStream;
import hudson.plugins.findbugs.model.FileAnnotation;
import hudson.plugins.findbugs.model.JavaPackage;
import hudson.plugins.findbugs.model.JavaProject;
import hudson.plugins.findbugs.model.MavenModule;
import hudson.plugins.findbugs.model.Priority;
import hudson.plugins.findbugs.model.WorkspaceFile;
import hudson.plugins.findbugs.parser.Bug;
import hudson.plugins.findbugs.util.SourceDetail;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.thoughtworks.xstream.XStream;

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
    private static final Set<FileAnnotation> EMPTY_SET = Collections.EMPTY_SET;
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
    private transient WeakReference<Set<FileAnnotation>> newWarnings;
    /** All fixed warnings in the current build.*/
    @SuppressWarnings("Se")
    private transient WeakReference<Set<FileAnnotation>> fixedWarnings;
    /** The number of warnings in this build. */
    private final int numberOfWarnings;
    /** The number of new warnings in this build. */
    private final int numberOfNewWarnings;
    /** The number of fixed warnings in this build. */
    private final int numberOfFixedWarnings;
    /** The number of low priority warnings in this build. */
    private final int low;
    /** The number of normal priority warnings in this build. */
    private final int normal;
    /** The number of high priority warnings in this build. */
    private final int high;

    /** Serialization provider. */
    private static final XStream XSTREAM = new AnnotationStream();

    static {
        XSTREAM.alias("bug", Bug.class);
    }

    /**
     * Creates a new instance of <code>FindBugsResult</code>.
     *
     * @param build
     *            the current build as owner of this action
     * @param project
     *            the parsed FindBugs result
     */
    public FindBugsResult(final AbstractBuild<?, ?> build, final JavaProject project) {
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
        super(build, project.getAnnotations());
        numberOfWarnings = project.getNumberOfAnnotations();

        this.project = new WeakReference<JavaProject>(project);
        delta = project.getNumberOfAnnotations() - previousProject.getNumberOfAnnotations();

        Collection<FileAnnotation> allWarnings = project.getAnnotations();

        Set<FileAnnotation> warnings = WarningDifferencer.getNewWarnings(allWarnings, previousProject.getAnnotations());
        numberOfNewWarnings = warnings.size();
        newWarnings = new WeakReference<Set<FileAnnotation>>(warnings);

        warnings = WarningDifferencer.getFixedWarnings(allWarnings, previousProject.getAnnotations());
        numberOfFixedWarnings = warnings.size();
        fixedWarnings = new WeakReference<Set<FileAnnotation>>(warnings);

        high = project.getNumberOfAnnotations(Priority.HIGH);
        normal = project.getNumberOfAnnotations(Priority.NORMAL);
        low = project.getNumberOfAnnotations(Priority.LOW);

        try {
            Collection<WorkspaceFile> files = project.getFiles();
            getDataFile().write(files.toArray(new WorkspaceFile[files.size()]));
        }
        catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to serialize the findbugs result.", exception);
        }
    }

    /**
     * Returns the serialization file.
     *
     * @return the serialization file.
     */
    private XmlFile getDataFile() {
        return new XmlFile(XSTREAM, new File(getOwner().getRootDir(), "findbugs-warnings.xml"));
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
    public int getNumberOfAnnotations() {
        return numberOfWarnings;
    }

    /** {@inheritDoc} */
    @Override
    public int getNumberOfAnnotations(final Priority priority) {
        if (priority == Priority.HIGH) {
            return high;
        }
        else if (priority == Priority.NORMAL) {
            return normal;
        }
        else {
            return low;
        }
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
        if (project == null) {
            loadResult();
        }
        JavaProject result = project.get();
        if (result == null) {
            loadResult();
        }
        return project.get();
    }

    /**
     * Returns the new warnings of this build.
     *
     * @return the new warnings of this build.
     */
    public Set<FileAnnotation> getNewWarnings() {
        try {
            if (newWarnings == null) {
                loadPreviousResult();
            }
            Set<FileAnnotation> result = newWarnings.get();
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
    public Set<FileAnnotation> getFixedWarnings() {
        try {
            if (fixedWarnings == null) {
                loadPreviousResult();
            }
            Set<FileAnnotation> result = fixedWarnings.get();
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
     */
    private void loadResult() {
        JavaProject result;
        try {
            JavaProject newProject = new JavaProject();
            WorkspaceFile[] files = (WorkspaceFile[])getDataFile().read();
            for (WorkspaceFile workspaceFile : files) {
                newProject.addAnnotations(workspaceFile.getAnnotations());
            }
            result = newProject;
        }
        catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to load " + getDataFile(), exception);
            result = new JavaProject();
        }
        project = new WeakReference<JavaProject>(result);
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
            newWarnings = new WeakReference<Set<FileAnnotation>>(
                    WarningDifferencer.getNewWarnings(getProject().getAnnotations(), getPreviousResult().getAnnotations()));
        }
        else {
            newWarnings = new WeakReference<Set<FileAnnotation>>(new HashSet<FileAnnotation>(getProject().getAnnotations()));
        }
        if (hasPreviousResult()) {
            fixedWarnings = new WeakReference<Set<FileAnnotation>>(
                    WarningDifferencer.getFixedWarnings(getProject().getAnnotations(), getPreviousResult().getAnnotations()));
        }
        else {
            fixedWarnings = new WeakReference<Set<FileAnnotation>>(EMPTY_SET);
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
                    return new SourceDetail(getOwner(), getAnnotation(link));
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
    public Collection<JavaPackage> getPackages() {
        return getProject().getPackages();
    }

    /**
     * Returns the modules of this project.
     *
     * @return the modules of this project
     */
    public Collection<MavenModule> getModules() {
        return getProject().getModules();
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
            return previousResult.getPackage(packageName).getNumberOfAnnotations();
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
        createDetailGraph(request, response, getProject().getModule(request.getParameter("module")), getProject().getAnnotationBound());
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
        MavenModule module = getModules().iterator().next();
        createDetailGraph(request, response, module.getPackage(request.getParameter("package")), module.getAnnotationBound());
    }

    /**
     * Returns a tooltip showing the distribution of priorities for the selected
     * package.
     *
     * @param name
     *            the package to show the distribution for
     * @return a tooltip showing the distribution of priorities
     */
    public String getToolTip(final String name) {
        if (isSingleModuleProject()) {
            return getProject().getModules().iterator().next().getPackage(name).getToolTip();
        }
        else {
            return getProject().getModule(name).getToolTip();
        }
    }
}
