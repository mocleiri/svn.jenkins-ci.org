package hudson.slaves;

import hudson.ExtensionPoint;
import hudson.model.Computer;
import hudson.model.Describable;
import hudson.remoting.Channel.Listener;
import hudson.util.DescriptorList;
import hudson.util.StreamTaskListener;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Extension point to allow control over how Slaves are started.
 *
 * <p>
 * <b>EXPERIMENTAL: SIGNATURE MAY CHANGE IN FUTURE RELEASES</b>
 * 
 * @author Stephen Connolly
 * @since 24-Apr-2008 22:12:35
 */
public abstract class ComputerStartMethod implements Describable<ComputerStartMethod>, ExtensionPoint {
    /**
     * Returns true if this {@link ComputerStartMethod} supports
     * programatic launch of the slave agent in the target {@link Computer}.
     */
    public boolean isLaunchSupported() {
        return true;
    }

    /**
     * Launches the slave agent for the given {@link Computer}.
     *
     * <p>
     * If the slave agent is launched successfully, {@link SlaveComputer#setChannel(InputStream, OutputStream, OutputStream, Listener)}
     * should be invoked in the end to notify Hudson of the established connection.
     * The operation could also fail, in which case there's no need to make any callback notification,
     * (except to notify the user of the failure through {@link StreamTaskListener}.)
     *
     * @param listener
     *      The progress of the launch, as well as any error, should be sent to this listener.
     */
    public abstract void launch(SlaveComputer computer, StreamTaskListener listener);

    /**
     * All registered {@link ComputerStartMethod} implementations.
     */
    public static final DescriptorList<ComputerStartMethod> LIST = new DescriptorList<ComputerStartMethod>();

    static {
        LIST.load(JNLPStartMethod.class);
        LIST.load(CommandStartMethod.class);        
    }
}
