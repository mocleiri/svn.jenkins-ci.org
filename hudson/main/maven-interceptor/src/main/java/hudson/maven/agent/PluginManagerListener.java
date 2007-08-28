package hudson.maven.agent;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;

import java.io.IOException;

/**
 * Receives notification from {@link PluginManagerInterceptor},
 * before and after a mojo is executed.
 * 
 * @author Kohsuke Kawaguchi
 */
public interface PluginManagerListener {
    void preExecute(MavenProject project,MojoExecution exec, PlexusConfiguration mergedConfig, ExpressionEvaluator eval) throws IOException, InterruptedException;

    /**
     * Called after the mojo has finished executing.
     *
     * @param project
     *      Same object as passed to {@link #preExecute}.
     * @param exec
     *      Same object as passed to {@link #preExecute}.
     * @param mergedConfig
     *      Same object as passed to {@link #preExecute}.
     * @param eval
     *      Same object as passed to {@link #preExecute}.
     * @param exception
     *      If mojo execution failed with {@link MojoFailureException} or
     *      {@link MojoExecutionException}, this method is still invoked
     *      with those error objects.
     *      If mojo executed successfully, this parameter is null.
     */
    void postExecute(MavenProject project, MojoExecution exec, PlexusConfiguration mergedConfig, ExpressionEvaluator eval, Exception exception) throws IOException, InterruptedException;
}
