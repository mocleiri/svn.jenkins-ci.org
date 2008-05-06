package hudson.plugins.pmd;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.plugins.pmd.parser.PmdCollector;
import hudson.plugins.pmd.util.HealthAwarePublisher;
import hudson.plugins.pmd.util.HealthReportBuilder;
import hudson.plugins.pmd.util.model.JavaProject;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.lang.StringUtils;

/**
 * Publishes the results of the PMD analysis.
 *
 * @author Ulli Hafner
 */
public class PmdPublisher extends HealthAwarePublisher {
    /** Default PMD pattern. */
    private static final String DEFAULT_PATTERN = "**/pmd.xml";
    /** Descriptor of this publisher. */
    public static final PmdDescriptor PMD_DESCRIPTOR = new PmdDescriptor();

    /**
     * Creates a new instance of <code>PmdPublisher</code>.
     *
     * @param pattern
     *            Ant file-set pattern to scan for PMD files
     * @param threshold
     *            Bug threshold to be reached if a build should be considered as
     *            unstable.
     * @param healthy
     *            Report health as 100% when the number of warnings is less than
     *            this value
     * @param unHealthy
     *            Report health as 0% when the number of warnings is greater
     *            than this value
     * @param height
     *            the height of the trend graph
     * @stapler-constructor
     */
    public PmdPublisher(final String pattern, final String threshold, final String healthy, final String unHealthy, final String height) {
        super(pattern, threshold, healthy, unHealthy, height);
    }

    /** {@inheritDoc} */
    @Override
    public Action getProjectAction(final AbstractProject<?, ?> project) {
        return new PmdProjectAction(project, getTrendHeight());
    }

    /** {@inheritDoc} */
    @Override
    public JavaProject perform(final AbstractBuild<?, ?> build, final BuildListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        logger.println("Collecting pmd analysis files...");

        JavaProject project = parseAllWorkspaceFiles(build, logger);
        PmdResult result = new PmdResultBuilder().build(build, project);
        HealthReportBuilder healthReportBuilder = createHealthReporter(
                Messages.PMD_ResultAction_HealthReportSingleItem(),
                Messages.PMD_ResultAction_HealthReportMultipleItem("%d"));
        build.getActions().add(new PmdResultAction(build, healthReportBuilder, result));

        return project;
    }

    /**
     * Scans the workspace for PMD files matching the specified pattern and
     * returns all found annotations merged in a project.
     *
     * @param build
     *            the build to create the action for
     * @param logger
     *            the logger
     * @return the project with the annotations
     * @throws IOException
     *             if the files could not be read
     * @throws InterruptedException
     *             if user cancels the operation
     */
    private JavaProject parseAllWorkspaceFiles(final AbstractBuild<?, ?> build,
            final PrintStream logger) throws IOException, InterruptedException {
        PmdCollector pmdCollector = new PmdCollector(
                    logger,
                    build.getTimestamp().getTimeInMillis(),
                    StringUtils.defaultIfEmpty(getPattern(), DEFAULT_PATTERN));

        return build.getProject().getWorkspace().act(pmdCollector);
    }

    /** {@inheritDoc} */
    public Descriptor<Publisher> getDescriptor() {
        return PMD_DESCRIPTOR;
    }
}
