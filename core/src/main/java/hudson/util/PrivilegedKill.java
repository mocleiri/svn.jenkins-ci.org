package hudson.util;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extension point that defines the privileged kill implementation
 * (sudo, pfexec) for an <code>ProcessTree</code>
 *
 * @author jpederzolli
 */
public abstract class PrivilegedKill implements Describable<PrivilegedKill>, ExtensionPoint, Serializable {

    private static transient final Logger LOGGER = Logger.getLogger(PrivilegedKill.class.getName());

    /**
     * Returns all the registered {@link PrivilegedKill} descriptors.
     */
    public static DescriptorExtensionList<PrivilegedKill, Descriptor<PrivilegedKill>> all() {
        if (Hudson.getInstance() != null) {
            return Hudson.getInstance().getDescriptorList(PrivilegedKill.class);
        }
        return null;
    }

    /**
     * Returns all the registered {@link PrivilegedKill} implementations.
     */
    public static List<PrivilegedKill> getRegisteredImplementations() {
        List<PrivilegedKill> privilegedKillExts = new ArrayList<PrivilegedKill>();
        for (Descriptor<PrivilegedKill> descriptor : PrivilegedKill.all()) {
            try {
                privilegedKillExts.add(descriptor.newInstance(null, null));
            } catch (Descriptor.FormException fe) {
                //will not happen
            }
        }
        return privilegedKillExts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Descriptor<PrivilegedKill> getDescriptor() {
        if (Hudson.getInstance() != null) {
            return Hudson.getInstance().getDescriptorOrDie(getClass());
        }
        return null;
    }

    /**
     * get applicable PrivilegedKill extension (if any) based off a ProcessTree class.
     * This method will always return null when run on slaves
     *
     * @param clazz ProcessTree implementation
     * @return PrivilegedKill extension
     */
    public static PrivilegedKill filter(Class<? extends ProcessTree> clazz) {
        //only attempt to find privileged kill impl if on master, i.e. Hudson.getInstance() != null
        if (Hudson.getInstance() != null) {
            return filter(all(), clazz);
        }
        return null;
    }

    /**
     * get applicable PrivilegedKill extension (if any) based off a ProcessTree class and DescriptorExtensionList
     *
     * @param descriptorList DescriptorExtensionList to filter
     * @param clazz          ProcessTree implementation
     * @return PrivilegedKill extension
     */
    public static PrivilegedKill filter(DescriptorExtensionList<PrivilegedKill, Descriptor<PrivilegedKill>> descriptorList, Class<? extends ProcessTree> clazz) {
        if (descriptorList != null) {
            for (final Descriptor<PrivilegedKill> descriptor : descriptorList) {
                try {
                    PrivilegedKill pk = descriptor.newInstance(null, null);
                    if (pk.isApplicable(clazz)) {
                        return pk;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.INFO, "failed to instantiate privileged kill object", e);
                }
            }
        }
        return null;
    }

    /**
     * get applicable PrivilegedKill extension (if any) based off a ProcessTree class and  DescriptorExtensionList
     *
     * @param privilegedKillList List of PrivilegedKill objects to filter
     * @param clazz              ProcessTree implementation
     * @return PrivilegedKill extension
     */
    public static PrivilegedKill filter(List<PrivilegedKill> privilegedKillList, Class<? extends ProcessTree> clazz) {
        if (privilegedKillList != null) {
            for (final PrivilegedKill privilegedKill : privilegedKillList) {
                if (privilegedKill.isApplicable(clazz)) {
                    return privilegedKill;
                }
            }
        }
        return null;
    }

    /**
     * execute the default kill command against a process
     *
     * @param process process to be killed
     * @return exit value from kill command
     */
    public int execute(ProcessTree.OSProcess process) {

        int exitValue = -1;
        int pid = process.getPid();
        if (pid > 0) {

            List<String> cmd = getPrivilegedKillCommand(pid);

            if (cmd == null || cmd.isEmpty()) {
                LOGGER.log(Level.INFO, "unable to construct privileged kill command, aborting privileged kill.");
                return -1;
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);

            try {
                exitValue = pb.start().waitFor();
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "failed to terminate process [" + pid + "] via forked command [" + pb.command() + "]", e);
            }
        } else {
            LOGGER.log(Level.INFO, "unable to determine pid, aborting privileged kill.");
        }
        return exitValue;
    }


    /**
     * determine if this PrivilegedKill implementation is appliciable to a specific <code>OSProcess</code> implementation
     *
     * @param osProcessType OSProcess type to check
     * @return true if applicable
     */
    public abstract boolean isApplicable(Class<? extends ProcessTree> osProcessType);


    /**
     * get the privileged kill command for a pid
     *
     * @param pid process id to kill
     * @return privileged kill command
     */
    protected abstract List<String> getPrivilegedKillCommand(int pid);

    private static final long serialVersionUID = 1L;
}
