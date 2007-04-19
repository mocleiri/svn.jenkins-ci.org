package hudson.maven.agent;

import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.DefaultPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.IOException;

/**
 * Description in META-INF/plexus/components.xml makes it possible to use this instead of the default
 * plugin manager.
 *
 * @author Kohsuke Kawaguchi
 */
public class PluginManagerInterceptor extends DefaultPluginManager {
    /**
     * {@link PluginManagerListener} that receives events.
     * There's no way external code can connect to a running instance of
     * {@link PluginManagerInterceptor}, so this cannot be made instance fields. 
     */
    private static PluginManagerListener listener;

    /**
     * {@link ComponentConfigurator} filter to intercept the mojo configuration.
     */
    private ComponentConfiguratorFilter configuratorFilter;

    public static void setListener(PluginManagerListener _listener) {
        listener = _listener;
    }

    @Override
    public void initialize() {
        super.initialize();
        container = new ContainerFilter(container) {
            /**
             * {@link DefaultPluginManager} uses it to load plugins and their configurators.
             *
             * @param name
             *      groupId+':'+artifactId of the plugin.
             */
            public PlexusContainer getChildContainer(String name) {
                PlexusContainer child = super.getChildContainer(name);
                if(child==null) return null;
                return new ContainerFilter(child) {
                    public Object lookup(String componentKey) throws ComponentLookupException {
                        return wrap(super.lookup(componentKey), componentKey);
                    }

                    public Object lookup(String role, String roleHint) throws ComponentLookupException {
                        return wrap(super.lookup(role,roleHint), role);
                    }

                    public void release(Object component) throws ComponentLifecycleException {
                        if(component==configuratorFilter)
                            super.release(configuratorFilter.core);
                        else
                            super.release(component);
                    }

                    private Object wrap(Object c, String componentKey) {
                        if(c!=null && componentKey.equals(ComponentConfigurator.ROLE)) {
                            if(configuratorFilter.core!=null)
                                throw new IllegalStateException("ComponentConfigurationFilter being reused. " +
                                    "This is a bug in Hudson. Please report that to the development team.");
                            configuratorFilter.core = (ComponentConfigurator)c;
                            c = configuratorFilter;
                        }
                        return c;
                    }
                };
            }
        };
    }

    public void executeMojo(final MavenProject project, final MojoExecution mojoExecution, MavenSession session) throws ArtifactResolutionException, MojoExecutionException, MojoFailureException, ArtifactNotFoundException, InvalidDependencyVersionException, PluginManagerException, PluginConfigurationException {
        class MojoConfig {
            PlexusConfiguration config;
            ExpressionEvaluator eval;

            void callPost(Exception exception) throws IOException, InterruptedException {
                if(listener!=null)
                    listener.postExecute(project,mojoExecution,config,eval,exception);
            }
        }

        final MojoConfig config = new MojoConfig();

        // prepare interception of ComponentConfigurator, so that we can get the final PlexusConfiguration object
        // representing the configuration before Mojo object is filled with that.
        configuratorFilter = new ComponentConfiguratorFilter(null) {
            @Override
            public void configureComponent(Object component, PlexusConfiguration configuration, ExpressionEvaluator expressionEvaluator, ClassRealm containerRealm, ConfigurationListener configListener) throws ComponentConfigurationException {
                try {
                    config.config = configuration;
                    config.eval = expressionEvaluator;
                    if(listener!=null)
                        listener.preExecute(project,mojoExecution,configuration,expressionEvaluator);
                    super.configureComponent(component, configuration, expressionEvaluator, containerRealm, configListener);
                } catch (IOException e) {
                    throw new ComponentConfigurationException(e);
                } catch (InterruptedException e) {
                    // orderly abort
                    throw new AbortException("Execution aborted",e);
                }
            }
        };

        try {
            try {
                // inside the executeMojo but before the mojo actually gets executed,
                // we should be able to trap the mojo configuration.
                super.executeMojo(project, mojoExecution, session);
                config.callPost(null);
            } catch (MojoExecutionException e) {
                config.callPost(e);
                throw e;
            } catch (MojoFailureException e) {
                config.callPost(e);
                throw e;
            }
        } catch (InterruptedException e) {
            // orderly abort
            throw new AbortException("Execution aborted",e);
        } catch (IOException e) {
            throw new PluginManagerException(e.getMessage(),e);
        }
    }
}
