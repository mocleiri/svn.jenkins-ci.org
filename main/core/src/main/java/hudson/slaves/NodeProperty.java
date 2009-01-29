package hudson.slaves;

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Node;
import hudson.tasks.Builder;
import hudson.tasks.Environment;

import java.io.IOException;

public abstract class NodeProperty<N extends Node> implements Describable<NodeProperty<?>>, ExtensionPoint {

	public abstract NodePropertyDescriptor getDescriptor();
	
    protected transient N node;
	
	protected void setNode(N node) { this.node = node; }
	
    /**
     * Runs before the {@link Builder} runs, and performs a set up.
     *
     * @param build
     *      The build in progress for which an {@link Environment} object is created.
     *      Never null.
     * @param launcher
     *      This launcher can be used to launch processes for this build.
     *      If the build runs remotely, launcher will also run a job on that remote machine.
     *      Never null.
     * @param listener
     *      Can be used to send any message.
     * @return
     *      non-null if the build can continue, null if there was an error
     *      and the build needs to be aborted.
     * @throws IOException
     *      terminates the build abnormally. Hudson will handle the exception
     *      and reports a nice error message.
     * @since 1.150
     */
    public Environment setUp( AbstractBuild build, Launcher launcher, BuildListener listener ) throws IOException, InterruptedException {
    	return new Environment() {};
    }
}
