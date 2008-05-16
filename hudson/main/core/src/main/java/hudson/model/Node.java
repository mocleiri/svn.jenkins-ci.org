package hudson.model;

import hudson.Launcher;
import hudson.FilePath;
import hudson.node_monitors.NodeMonitor;
import hudson.util.EnumConverter;
import hudson.util.ClockDifference;
import org.apache.commons.beanutils.ConvertUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.Stapler;
import org.acegisecurity.ui.AbstractProcessingFilter;

import java.util.Set;
import java.io.IOException;

/**
 * Commonality between {@link Slave} and master {@link Hudson}.
 *
 * @author Kohsuke Kawaguchi
 * @see NodeMonitor
 */
public interface Node {
    /**
     * Name of this node.
     *
     * @return
     *      "" if this is master
     */
    String getNodeName();

    /**
     * Human-readable description of this node.
     */
    String getNodeDescription();

    /**
     * Returns a {@link Launcher} for executing programs on this node.
     */
    Launcher createLauncher(TaskListener listener);

    /**
     * Returns the number of {@link Executor}s.
     *
     * This may be different from <code>getExecutors().size()</code>
     * because it takes time to adjust the number of executors.
     */
    int getNumExecutors();

    /**
     * Returns {@link Mode#EXCLUSIVE} if this node is only available
     * for those jobs that exclusively specifies this node
     * as the assigned node.
     */
    Mode getMode();

    /**
     * Gets the corresponding {@link Computer} object.
     *
     * @return
     *      never null.
     */
    Computer toComputer();

    Computer createComputer();

    /**
     * Returns the possibly empty set of labels that are assigned to this node,
     * including the automatic {@link #getSelfLabel() self label}.
     */
    Set<Label> getAssignedLabels();

    /**
     * Returns the possibly empty set of labels that it has been determined as supported by this node.
     * @see hudson.tasks.LabelFinder
     */
    Set<Label> getDynamicLabels();

    /**
     * Gets the special label that represents this node itself.
     */
    Label getSelfLabel();

    /**
     * Returns a "workspace" directory for the given {@link TopLevelItem}.
     *
     * <p>
     * Workspace directory is usually used for keeping out the checked out
     * source code, but it can be used for anything.
     *
     * @return
     *      null if this node is not connected hence the path is not available
     */
    FilePath getWorkspaceFor(TopLevelItem item);

    /**
     * Gets the root directory of this node.
     *
     * <p>
     * Hudson always owns a directory on every node. This method
     * returns that.
     *
     * @return
     *      null if the node is offline and hence the {@link FilePath}
     *      object is not available.
     */
    FilePath getRootPath();

    /**
     * Estimates the clock difference with this slave.
     *
     * @return
     *      always non-null.
     * @throws InterruptedException
     *      if the operation is aborted.
     */
    ClockDifference getClockDifference() throws IOException, InterruptedException;

    public enum Mode {
        NORMAL("Utilize this slave as much as possible"),
        EXCLUSIVE("Leave this machine for tied jobs only");

        private final String description;

        public String getDescription() {
            return description;
        }

        public String getName() {
            return name();
        }

        Mode(String description) {
            this.description = description;
        }

        static {
            ConvertUtils.register(new EnumConverter(),Mode.class);
            Stapler.CONVERT_UTILS.register(new EnumConverter(),Mode.class);  // TODO ISSUE-1704
            try {
                Class.forName(Availability.class.getName());
            } catch (ClassNotFoundException e) {
            }
        }
    }

    public enum Availability {
        ALWAYS("Keep this slave on-line as much as possible", "configPageAlways"),
        SCHEDULED("Take this slave on-line and off-line at specific times", "configPageScheduled"),
        DEMAND("Take this slave on-line and off-line as needed", "configPageDemand");

        private final String configPage;

        private final String description;

        public String getDescription() {
            return description;
        }

        public String getName() {
            return name();
        }

        Availability(String description, String configPage) {
            this.description = description;
            this.configPage = configPage;
        }

        static {
            ConvertUtils.register(new EnumConverter(),Availability.class);
            Stapler.CONVERT_UTILS.register(new EnumConverter(),Availability.class);    // TODO ISSUE-1704
        }

        public void doConfigPage( StaplerRequest req, StaplerResponse rsp ) throws IOException {
            rsp.sendRedirect2(configPage);
        }


    }
}
