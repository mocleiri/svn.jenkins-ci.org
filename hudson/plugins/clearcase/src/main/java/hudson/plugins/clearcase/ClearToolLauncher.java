package hudson.plugins.clearcase;

import hudson.FilePath;
import hudson.model.TaskListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface to hide the Hudson launch parts so other parts can mock the actual launch.
 * 
 * @author Erik Ramfelt
 */
public interface ClearToolLauncher {
	/**
	 * Launches a command
	 * @param cmds the command to launch
	 * @param in optional, if the command should be able to receive input
	 * @param out optional, can be used to gather the output stream
	 * @param relativePath optional, the relative path where the command should be issued
	 * @return true if the command was successful, false otherwise
	 */
	boolean run(String[] cmds, InputStream in, OutputStream out, String relativePathStr) 
		throws IOException, InterruptedException;
	
	/**
	 * Returns a task listener for a hudson job
	 * @return a task listener
	 */
	TaskListener getListener();
	
	/**
	 * Returns the workspace file path for a hudson job
	 * @return the workspace file path
	 */
	FilePath getWorkspace();
}
