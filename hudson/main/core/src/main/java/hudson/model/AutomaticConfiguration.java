package hudson.model;

import hudson.tasks.Builder;
import hudson.tasks.BuildStep;

import java.util.Map;
import java.io.Serializable;

/**
 * Abstraction for automatic configuration of build environments.
 *
 * @author Stephen Connolly
 */
public interface AutomaticConfiguration extends Serializable {

    /**
     * The name of this Automatic Configuration.
     * @return The configuration name.
     */
    String getName();

    /**
     * Is this configuration used by a specific build step.
     * @param buildStep The buildstep class.
     * @return true if this configuration should be applied to this buildstep class.
     */
    boolean applicableTo(Class<? extends BuildStep> buildStep);

    /**
     * Configure the environment for this configuration.
     * @param environment The environment to configure.
     * @return An unmodifiable map containing the configured environment.
     */
    Map<String,String> configureEnvironment(Map<String,String> environment);
}
