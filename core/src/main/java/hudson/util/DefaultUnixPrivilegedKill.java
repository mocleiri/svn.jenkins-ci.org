package hudson.util;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.ModelObject;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Embedded plugin that provides the default Unix privileged kill command (/usr/bin/sudo /bin/kill -15 <PID>)
 *
 * @auther jpederzolli
 */
public class DefaultUnixPrivilegedKill extends PrivilegedKill {

    private static transient final Logger LOGGER = Logger.getLogger(DefaultUnixPrivilegedKill.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    public Descriptor<PrivilegedKill> getDescriptor() {
        return new DescriptorImpl();
    }

    /**
     * determine if this PrivilegedKill implementation is appliciable to a specific OSProcess implementation
     *
     * @param os OS type to check
     * @return true if applicable
     */
    public boolean isApplicable(Class<? extends ProcessTree> os) {
        return ProcessTree.Unix.class.isAssignableFrom(os);
    }

    /**
     * get command to execute as superuser
     *
     * @return List containing executable path + flags
     */
    protected List<String> getPrivilegedCommand() {
        return Collections.singletonList("/usr/bin/sudo");
    }

    /**
     * get the kill command
     *
     * @return List containing kill executable path + flags
     */
    protected List<String> getKillCommand() {
        List<String> killCmd = new ArrayList<String>();
        killCmd.add("/bin/kill");
        killCmd.add("-15");
        return Collections.unmodifiableList(killCmd);
    }

    @Override
    protected List<String> getPrivilegedKillCommand(int pid) {
        List<String> privilegedCommand = getPrivilegedCommand();

        if (privilegedCommand == null || privilegedCommand.isEmpty()) {
            LOGGER.log(Level.INFO, "getPrivilegedCommand() returned null or empy command");
            return null;
        }
        List<String> killCommand = getKillCommand();
        if (killCommand == null || killCommand.isEmpty()) {
            LOGGER.log(Level.INFO, "getKillCommand() returned null or empy command");
            return null;
        }

        List<String> privilegedKillCommand = new ArrayList<String>();
        privilegedKillCommand.addAll(privilegedCommand);
        privilegedKillCommand.addAll(killCommand);
        privilegedKillCommand.add(String.valueOf(pid));
        return privilegedKillCommand;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<PrivilegedKill> implements ModelObject {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return DefaultUnixPrivilegedKill.class.getName();
        }

        @Override
        public DefaultUnixPrivilegedKill newInstance(StaplerRequest req, JSONObject formData) {
            return new DefaultUnixPrivilegedKill();
        }
    }
}
