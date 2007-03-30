package hudson.maven.agent;

import hudson.remoting.Callable;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;

import java.io.IOException;

/**
 * Starts Maven CLI. Remotely executed.
 *
 * @author Kohsuke Kawaguchi
 */
public class RunCommand implements Callable {
    private final String[] args;

    public RunCommand(String... args) {
        this.args = args;
    }

    public Object call() throws Throwable {
        // return Main.class.getClassLoader().toString();

        PluginManagerInterceptor.setListener(new PluginManagerListener() {
            public void preExecute(MavenProject project, MojoExecution exec, PlexusConfiguration mergedConfig, ExpressionEvaluator eval) throws IOException, InterruptedException, AbortException {
                System.out.println("***** "+exec.getMojoDescriptor().getGoal());
            }

            public void postExecute(MavenProject project, MojoExecution exec, PlexusConfiguration mergedConfig, ExpressionEvaluator eval, Exception exception) throws IOException, InterruptedException, AbortException {
                System.out.println("==== "+exec.getMojoDescriptor().getGoal());
            }
        });

        return Main.launch(args);
    }
}
