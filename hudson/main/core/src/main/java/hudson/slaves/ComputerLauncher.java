package hudson.slaves;

import hudson.ExtensionPoint;
import hudson.model.Computer;
import hudson.model.Describable;
import hudson.remoting.Channel.Listener;
import hudson.util.DescriptorList;
import hudson.util.StreamTaskListener;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Extension point to allow control over how {@link Computer}s are "launched",
 * meaning how they get connected to their slave agent program.
 *
 * <h2>Associated View</h2>
 * <dl>
 * <dt>main.jelly</dt>
 * <dd>
 * This page will be rendered into the top page of the computer (/computer/NAME/)
 * Useful for showing launch related commands and status reports.
 * </dl>
 *
 * <p>
 * <b>EXPERIMENTAL: SIGNATURE MAY CHANGE IN FUTURE RELEASES</b>
 *
 * @author Stephen Connolly
 * @since 24-Apr-2008 22:12:35
 */
public abstract class ComputerLauncher implements Describable<ComputerLauncher>, ExtensionPoint {
    /**
     * Returns true if this {@link ComputerLauncher} supports
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
     * <p>
     * This method must operate synchronously. Asynchrony is provided by {@link Computer#connect(boolean)} and
     * its correct operation depends on this. 
     *
     * @param listener
     *      The progress of the launch, as well as any error, should be sent to this listener.
     *
     * @throws IOException
     *      if the method throws an {@link IOException} or {@link InterruptedException}, the launch was considered
     *      a failure and the stack trace is reported into the listener. This handling is just so that the implementation
     *      of this method doesn't have to dilligently catch those exceptions.
     */
    public abstract void launch(SlaveComputer computer, StreamTaskListener listener) throws IOException , InterruptedException;

    /**
     * Allows the {@link ComputerLauncher} to tidy-up after a disconnect.
     */
    public void afterDisconnect(SlaveComputer computer, StreamTaskListener listener) {
    }

    /**
     * Allows the {@link ComputerLauncher} to prepare for a disconnect.
     */
    public void beforeDisconnect(SlaveComputer computer, StreamTaskListener listener) {
    }

    /**
     * All registered {@link ComputerLauncher} implementations.
     */
    public static final DescriptorList<ComputerLauncher> LIST = new DescriptorList<ComputerLauncher>();

    static {
        LIST.load(JNLPLauncher.class);
        LIST.load(CommandLauncher.class);
    }
}
