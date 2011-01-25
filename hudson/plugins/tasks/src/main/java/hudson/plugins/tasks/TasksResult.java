package hudson.plugins.tasks; // NOPMD

import hudson.model.AbstractBuild;
import hudson.plugins.analysis.core.BuildResult;
import hudson.plugins.analysis.core.ResultAction;
import hudson.plugins.analysis.util.model.Priority;
import hudson.plugins.tasks.parser.Task;
import hudson.plugins.tasks.parser.TasksParserResult;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.XStream;

/**
 * Represents the results of the task scanner. One instance of this class is persisted for
 * each build via an XML file.
 *
 * @author Ulli Hafner
 */
public class TasksResult extends BuildResult {
    /** Unique identifier of this class. */
    private static final long serialVersionUID = -344808345805935004L;

    /** Tag identifiers indicating high priority. */
    private final String highTags;
    /** Tag identifiers indicating normal priority. */
    private final String normalTags;
    /** Tag identifiers indicating low priority. */
    private final String lowTags;

    /** The number of scanned files in the project. */
    private final int numberOfFiles;

    /**
     * Creates a new instance of {@link TasksResult}.
     *
     * @param build
     *            the current build as owner of this action
     * @param defaultEncoding
     *            the default encoding to be used when reading and parsing files
     * @param result
     *            the parsed annotations
     * @param highTags
     *            tag identifiers indicating high priority
     * @param normalTags
     *            tag identifiers indicating normal priority
     * @param lowTags
     *            tag identifiers indicating low priority
     */
    public TasksResult(final AbstractBuild<?, ?> build, final String defaultEncoding,
            final TasksParserResult result, final String highTags, final String normalTags, final String lowTags) {
        super(build, defaultEncoding, result);

        this.highTags = highTags;
        this.normalTags = normalTags;
        this.lowTags = lowTags;

        numberOfFiles = result.getNumberOfScannedFiles();
    }

    /** {@inheritDoc} */
    @Override
    protected void configure(final XStream xstream) {
        xstream.alias("task", Task.class);
    }

    /**
     * Returns a summary message for the summary.jelly file.
     *
     * @return the summary message
     */
    public String getSummary() {
        return ResultSummary.createSummary(this);
    }

    /** {@inheritDoc} */
    @Override
    protected String createDeltaMessage() {
        return ResultSummary.createDeltaMessage(this);
    }

    /**
     * Returns the number of scanned files in this project.
     *
     * @return the number of scanned files in this project
     */
    public int getNumberOfFiles() {
        return numberOfFiles;
    }

    /**
     * Returns the display name (bread crumb name) of this result.
     *
     * @return the display name of this result.
     */
    public String getDisplayName() {
        return Messages.Tasks_ProjectAction_Name();
    }

    /** {@inheritDoc} */
    @Override
    protected String getSerializationFileName() {
        return "open-tasks.xml";
    }

    /**
     * Returns the actually used priorities.
     *
     * @return the actually used priorities.
     */
    @Override
    public Priority[] getPriorities() {
        ArrayList<Priority> priorities = new ArrayList<Priority>();
        if (StringUtils.isNotEmpty(highTags)) {
            priorities.add(Priority.HIGH);
        }
        if (StringUtils.isNotEmpty(normalTags)) {
            priorities.add(Priority.NORMAL);
        }
        if (StringUtils.isNotEmpty(lowTags)) {
            priorities.add(Priority.LOW);
        }
        return priorities.toArray(new Priority[priorities.size()]);
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
            return highTags;
        }
        else if (priority == Priority.NORMAL) {
            return normalTags;
        }
        else {
            return lowTags;
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
                return Messages.Tasks_NamespaceDetail();
            }
        }
        return Messages.Tasks_PackageDetail();
    }

    /** {@inheritDoc} */
    @Override
    protected Class<? extends ResultAction<? extends BuildResult>> getResultActionType() {
        return TasksResultAction.class;
    }

    // Backward compatibility. Do not remove.
    // CHECKSTYLE:OFF
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("SE")
    @SuppressWarnings("unused")
    @Deprecated
    private transient int numberOfTasks;
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("SE")
    @SuppressWarnings("unused")
    @Deprecated
    private transient int highPriorityTasks;
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("SE")
    @SuppressWarnings("unused")
    @Deprecated
    private transient int lowPriorityTasks;
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("SE")
    @SuppressWarnings("unused")
    @Deprecated
    private transient int normalPriorityTasks;
}