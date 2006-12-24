package hudson.model;

import hudson.FilePath;
import hudson.Util;
import hudson.ExtensionPoint;
import hudson.model.Descriptor.FormException;

import java.io.IOException;

/**
 * Information about a Hudson slave node.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Slave implements Node, ExtensionPoint, Describable<Slave> {
    /**
     * Name of this slave node.
     */
    protected final String name;

    /**
     * Description of this node.
     */
    private final String description;

    /**
     * Path to the root of the workspace
     * from the view point of this node, such as "/hudson"
     */
    protected final String remoteFS;


    /**
     * Number of executors of this node.
     */
    private int numExecutors = 2;

    /**
     * Job allocation strategy.
     */
    private Mode mode;

    public Slave(String name, String description, String remoteFS, int numExecutors, Mode mode) throws FormException {
        this.name = name;
        this.description = description;
        this.numExecutors = numExecutors;
        this.mode = mode;
        this.remoteFS = remoteFS;

        if (name.equals(""))
            throw new FormException("Invalid slave configuration. Name is empty", null);

        // this prevents the config from being saved when slaves are offline.
        // on a large deployment with a lot of slaves, some slaves are bound to be offline,
        // so this check is harmful.
        //if (!localFS.exists())
        //    throw new FormException("Invalid slave configuration for " + name + ". No such directory exists: " + localFS, null);
        if (remoteFS.equals(""))
            throw new FormException("Invalid slave configuration for " + name + ". No remote directory given", null);
    }

    public final String getNodeName() {
        return name;
    }

    public final String getNodeDescription() {
        return description;
    }

    public final int getNumExecutors() {
        return numExecutors;
    }

    public final Mode getMode() {
        return mode;
    }

    /**
     * Estimates the clock difference with this slave.
     *
     * @return
     *      difference in milli-seconds.
     *      a positive value indicates that the master is ahead of the slave,
     *      and negative value indicates otherwise.
     */
    public abstract long getClockDifference() throws IOException;


    public abstract SlaveDescriptor getDescriptor();

    /**
     * Gets the clock difference in HTML string.
     */
    public final String getClockDifferenceString() {
        try {
            long diff = getClockDifference();
            if(-1000<diff && diff <1000)
                return "In sync";  // clock is in sync

            long abs = Math.abs(diff);

            String s = Util.getTimeSpanString(abs);
            if(diff<0)
                s += " ahead";
            else
                s += " behind";

            if(abs>100*60) // more than a minute difference
                s = "<span class='error'>"+s+"</span>";

            return s;
        } catch (IOException e) {
            return "<span class='error'>Unable to check</span>";
        }
    }

    /**
     * @deprecated
     *      To be removed. Don't use.
     */
    public abstract FilePath getWorkspaceRoot();

    /**
     * Gets th ecorresponding computer object.
     */
    public final Computer getComputer() {
        return Hudson.getInstance().getComputer(getNodeName());
    }

    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Slave that = (Slave) o;

        return name.equals(that.name);
    }

    public final int hashCode() {
        return name.hashCode();
    }
}
