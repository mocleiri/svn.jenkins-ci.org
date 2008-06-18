package hudson.plugins.tasks; // NOPMD

import hudson.XmlFile;
import hudson.model.AbstractBuild;
import hudson.model.ModelObject;
import hudson.plugins.tasks.parser.Task;
import hudson.plugins.tasks.parser.TasksProject;
import hudson.plugins.tasks.util.ChartRenderer;
import hudson.plugins.tasks.util.PrioritiesDetail;
import hudson.plugins.tasks.util.PriorityDetailFactory;
import hudson.plugins.tasks.util.SourceDetail;
import hudson.plugins.tasks.util.model.AnnotationContainer;
import hudson.plugins.tasks.util.model.AnnotationProvider;
import hudson.plugins.tasks.util.model.AnnotationStream;
import hudson.plugins.tasks.util.model.DefaultAnnotationContainer;
import hudson.plugins.tasks.util.model.FileAnnotation;
import hudson.plugins.tasks.util.model.JavaPackage;
import hudson.plugins.tasks.util.model.MavenModule;
import hudson.plugins.tasks.util.model.Priority;
import hudson.plugins.tasks.util.model.WorkspaceFile;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.thoughtworks.xstream.XStream;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Represents the results of the task  scanner. One instance of this class is persisted for
 * each build via an XML file.
 *
 * @author Ulli Hafner
 */
//CHECKSTYLE:COUPLING-OFF
@SuppressWarnings("PMD.TooManyFields")
public class TasksResult implements ModelObject, Serializable, AnnotationProvider  {
    /** Unique identifier of this class. */
    private static final long serialVersionUID = -344808345805935004L;
    /** Error logger. */
    private static final Logger LOGGER = Logger.getLogger(TasksResult.class.getName());
    /** Serialization provider. */
    private static final XStream XSTREAM = new AnnotationStream();

    static {
        XSTREAM.alias("task", Task.class);
    }

    /** The parsed project with annotations. */
    @SuppressWarnings("Se")
    private transient WeakReference<TasksProject> project;

    /** Current build as owner of this action. */
    @SuppressWarnings("Se")
    private final AbstractBuild<?, ?> owner;

    /** Tag identifiers indicating high priority. */
    private final String high;
    /** Tag identifiers indicating normal priority. */
    private final String normal;
    /** Tag identifiers indicating low priority. */
    private final String low;

    /** The total number of tasks. */
    private final int numberOfTasks;
    /** Difference between this and the previous build. */
    private final int delta;
    /** The number of high priority tasks in this build. */
    private final int highPriorityTasks;
    /** The number of low priority tasks in this build. */
    private final int lowPriorityTasks;
    /** The number of normal priority tasks in this build. */
    private final int normalPriorityTasks;
    /** The number of scanned files in the project. */
    private final int numberOfFiles;

    /**
     * Creates a new instance of <code>TasksResult</code>.
     *
     * @param build
     *            the current build as owner of this action
     * @param project
     *            the parsed FindBugs result
     * @param high
     *            tag identifiers indicating high priority
     * @param normal
     *            tag identifiers indicating normal priority
     * @param low
     *            tag identifiers indicating low priority
     */
    public TasksResult(final AbstractBuild<?, ?> build, final TasksProject project, final String high, final String normal, final String low) {
        this(build, project, project.getNumberOfAnnotations(), high, normal, low);
    }

    /**
     * Creates a new instance of <code>TasksResult</code>.
     *
     * @param build the current build as owner of this action
     * @param project the parsed FindBugs result
     * @param previousNumberOfTasks the previous number of open tasks
     * @param high
     *            tag identifiers indicating high priority
     * @param normal
     *            tag identifiers indicating normal priority
     * @param low
     *            tag identifiers indicating low priority
     */
    public TasksResult(final AbstractBuild<?, ?> build, final TasksProject project, final int previousNumberOfTasks, final String high, final String normal, final String low) {
        owner = build;

        highPriorityTasks = project.getNumberOfAnnotations(Priority.HIGH);
        lowPriorityTasks = project.getNumberOfAnnotations(Priority.LOW);
        normalPriorityTasks = project.getNumberOfAnnotations(Priority.NORMAL);

        this.high = high;
        this.normal = normal;
        this.low = low;

        numberOfTasks = project.getNumberOfAnnotations();
        delta = numberOfTasks - previousNumberOfTasks;

        numberOfFiles = project.getNumberOfScannedFiles();
        this.project = new WeakReference<TasksProject>(project);

        try {
            Collection<FileAnnotation> files = project.getAnnotations();
            getDataFile().write(files.toArray(new FileAnnotation[files.size()]));
        }
        catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to serialize the open tasks result.", exception);
        }
    }

    /**
     * Returns a summary message for the summary.jelly file.
     *
     * @return the summary message
     */
    public String getSummary() {
        return ResultSummary.createSummary(this);
    }

    /**
     * Returns whether this result belongs to the last build.
     *
     * @return <code>true</code> if this result belongs to the last build
     */
    public final boolean isCurrent() {
        return owner.getProject().getLastBuild().number == owner.number;
    }

    /**
     * Returns the build as owner of this action.
     *
     * @return the owner
     */
    public final AbstractBuild<?, ?> getOwner() {
        return owner;
    }

    /**
     * Returns the number of scanned files in this project.
     *
     * @return the number of scanned files in a {@link TasksProject}
     */
    public int getNumberOfFiles() {
        return numberOfFiles;
    }

    /**
     * Gets the number of tasks.
     *
     * @return the number of tasks
     */
    public int getNumberOfAnnotations() {
        return numberOfTasks;
    }

    /**
     * Returns the total number of tasks of the specified priority for
     * this object.
     *
     * @param priority
     *            the priority
     * @return total number of tasks of the specified priority for this
     *         object
     */
    public int getNumberOfAnnotations(final Priority priority) {
        if (priority == Priority.HIGH) {
            return highPriorityTasks;
        }
        else if (priority == Priority.NORMAL) {
            return normalPriorityTasks;
        }
        else {
            return lowPriorityTasks;
        }
    }

    /**
     * Returns the display name (bread crumb name) of this result.
     *
     * @return the display name of this result.
     */
    public String getDisplayName() {
        return Messages.Tasks_ProjectAction_Name();
    }

    /**
     * Returns the delta between this build and the previous build.
     *
     * @return the delta between this build and the previous build
     */
    public int getDelta() {
        return delta;
    }

    /**
     * Returns the packages in this project.
     *
     * @return the packages in this project
     */
    public Collection<JavaPackage> getPackages() {
        return getProject().getPackages();
    }

    /**
     * Returns the associated project of this result.
     *
     * @return the associated project of this result.
     */
    public synchronized TasksProject getProject() {
        if (project == null) {
            loadResult();
        }
        AnnotationProvider result = project.get();
        if (result == null) {
            loadResult();
        }
        return project.get();
    }

    /**
     * Loads the tasks results and wraps them in a weak reference that might
     * get removed by the garbage collector.
     */
    private void loadResult() {
        TasksProject result;
        try {
            TasksProject newProject = new TasksProject(numberOfFiles);
            FileAnnotation[] annotations = (FileAnnotation[])getDataFile().read();
            newProject.addAnnotations(annotations);
            LOGGER.log(Level.INFO, "Loaded tasks data file " + getDataFile() + " for build " + getOwner().getNumber());
            result = newProject;
        }
        catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to load " + getDataFile(), exception);
            result = new TasksProject();
        }
        project = new WeakReference<TasksProject>(result);
    }

    /**
     * Returns the serialization file.
     *
     * @return the serialization file.
     */
    private XmlFile getDataFile() {
        return new XmlFile(XSTREAM, new File(getOwner().getRootDir(), "open-tasks.xml"));
    }

    /**
     * Returns the dynamic result of this tasks detail view. Depending on the
     * number of modules and packages, one of the following detail objects is
     * returned:
     * <ul>
     * <li>A task detail object for a single workspace file (if the project
     * contains only one package and one module).</li>
     * <li>A package detail object for a specified package (if the project
     * contains only one module).</li>
     * <li>A module detail object for a specified module (in any other case).</li>
     * </ul>
     *
     * @param link
     *            the link to the source code
     * @param request
     *            Stapler request
     * @param response
     *            Stapler response
     * @return the dynamic result of the FindBugs analysis (detail page for a
     *         package).
     */
    public Object getDynamic(final String link, final StaplerRequest request, final StaplerResponse response) {
        PriorityDetailFactory factory = new PriorityDetailFactory() {
            /** {@inheritDoc} */
            @Override
            @SuppressWarnings("IMA")
            protected PrioritiesDetail createPrioritiesDetail(final Priority priority, final AbstractBuild<?, ?> build, final AnnotationContainer container, final String header) {
                return new TasksPrioritiesDetail(build, getProject(), priority, header, high, normal, low);
            }
        };
        if (factory.isPriority(link)) {
            return factory.create(link, owner, getProject(), getDisplayName());
        }
        else if (link.startsWith("module.")) {
            return new TasksModuleDetail(getOwner(), getModule(StringUtils.substringAfter(link, "module.")), getDisplayName(), high, normal, low);
        }
        else if (link.startsWith("package.")) {
            return new TasksPackageDetail(getOwner(), getProject().getPackage(StringUtils.substringAfter(link, "package.")), getDisplayName(), high, normal, low);
        }
        else if (link.startsWith("source.")) {
            return new SourceDetail(getOwner(), getProject().getAnnotation(StringUtils.substringAfter(link, "source.")));
        }
        return null;
    }

    public AnnotationContainer getContainer() {
        return getProject();
    }

    /**
     * Returns the module with the specified name.
     *
     * @param name
     *            the module to get
     * @return the module
     */
    public MavenModule getModule(final String name) {
        return getProject().getModule(name);
    }

    /**
     * Returns the actually used priorities.
     *
     * @return the actually used priorities.
     */
    public List<String> getPriorities() {
        List<String> actualPriorities = new ArrayList<String>();
        for (String priority : getAvailablePriorities()) {
            if (getNumberOfAnnotations(priority) > 0) {
                actualPriorities.add(priority);
            }
        }
        return actualPriorities;
    }

    /**
     * Returns the defined priorities.
     *
     * @return the defined priorities.
     */
    public Collection<String> getAvailablePriorities() {
        ArrayList<String> priorities = new ArrayList<String>();
        if (StringUtils.isNotEmpty(high)) {
            priorities.add(StringUtils.capitalize(StringUtils.lowerCase(Priority.HIGH.name())));
        }
        if (StringUtils.isNotEmpty(normal)) {
            priorities.add(StringUtils.capitalize(StringUtils.lowerCase(Priority.NORMAL.name())));
        }
        if (StringUtils.isNotEmpty(low)) {
            priorities.add(StringUtils.capitalize(StringUtils.lowerCase(Priority.LOW.name())));
        }
        return priorities;
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
     * Returns the tags for the specified priority.
     *
     * @param priority the priority
     *
     * @return the tags for the specified priority
     */
    public final String getTags(final String priority) {
        return getTags(Priority.fromString(priority));
    }

    /**
     * Returns the tags for the specified priority.
     *
     * @param priority
     *            the priority
     * @return the tags for the specified priority
     */
    public final String getTags(final Priority priority) {
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
     * Returns the package category name for the scanned files. Currently, only
     * java and c# files are supported.
     *
     * @return the package category name for the scanned files
     */
    public String getPackageCategoryName() {
        if (hasAnnotations()) {
            String fileName = getAnnotations().iterator().next().getFileName();
            if (fileName.endsWith(".cs")) {
                return hudson.plugins.tasks.util.Messages.NamespaceDetail_header();
            }
        }
        return hudson.plugins.tasks.util.Messages.PackageDetail_header();
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
    public final void doStatistics(final StaplerRequest request, final StaplerResponse response) throws IOException {
        String parameter = request.getParameter("object");
        if (parameter.startsWith("category.")) {
            Set<FileAnnotation> annotations = getProject().getCategory(StringUtils.substringAfter(parameter, "category."));
            ChartRenderer.renderPriorititesChart(request, response, new DefaultAnnotationContainer(annotations), getProject().getAnnotationBound());
        }
        else if (parameter.startsWith("type.")) {
            Set<FileAnnotation> annotations = getProject().getType(StringUtils.substringAfter(parameter, "type."));
            ChartRenderer.renderPriorititesChart(request, response, new DefaultAnnotationContainer(annotations), getProject().getAnnotationBound());
        }
        else if (parameter.startsWith("package.")) {
            AnnotationContainer annotations = getProject().getPackage(StringUtils.substringAfter(parameter, "package."));
            ChartRenderer.renderPriorititesChart(request, response, annotations, getProject().getAnnotationBound());
        }
        else if (parameter.startsWith("module.")) {
            AnnotationContainer annotations = getModule(StringUtils.substringAfter(parameter, "module."));
            ChartRenderer.renderPriorititesChart(request, response, annotations, getProject().getAnnotationBound());
        }
        // TODO: we should parameterize the annotation bound (second parameter instead of getChild)
    }

    // Delegates to TasksProject
    // CHECKSTYLE:OFF

    public Collection<WorkspaceFile> getFiles() {
        return getProject().getFiles();
    }


    /** {@inheritDoc} */
    public FileAnnotation getAnnotation(final long key) {
        return getProject().getAnnotation(key);
    }

    /** {@inheritDoc} */
    public FileAnnotation getAnnotation(final String key) {
        return getProject().getAnnotation(key);
    }

    /** {@inheritDoc} */
    public Collection<FileAnnotation> getAnnotations(final Priority priority) {
        return getProject().getAnnotations(priority);
    }

    /** {@inheritDoc} */
    public Collection<FileAnnotation> getAnnotations(final String priority) {
        return getProject().getAnnotations(priority);
    }

    /** {@inheritDoc} */
    public boolean hasAnnotations(final Priority priority) {
        return getProject().hasAnnotations(priority);
    }

    /** {@inheritDoc} */
    public boolean hasAnnotations(final String priority) {
        return getProject().hasAnnotations(priority);
    }

    /** {@inheritDoc} */
    public final boolean hasAnnotations() {
        return getProject().hasAnnotations();
    }

    /** {@inheritDoc} */
    public Collection<FileAnnotation> getAnnotations() {
        return getProject().getAnnotations();
    }

    /** {@inheritDoc} */
    public int getNumberOfAnnotations(final String priority) {
        return getNumberOfAnnotations(Priority.fromString(priority));
    }
}