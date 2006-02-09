package hudson.tasks;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import hudson.model.BuildListener;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class AntBasedBuildStep implements BuildStep {
    protected final void execTask(Task copyTask, BuildListener listener) {
        try {
            copyTask.execute();
        } catch( BuildException e ) {
            // failing to archive isn't a fatal error
            e.printStackTrace(listener.error(e.getMessage()));
        }
    }
}
