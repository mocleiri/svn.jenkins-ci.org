package hudson.model;

import hudson.tasks.BuildStep;

import java.util.Map;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Stephen Connolly
 */
abstract public class AbstractAutomaticConfiguration implements AutomaticConfiguration {
    private final String name;

    protected AbstractAutomaticConfiguration(String name) {
        this.name = name;
    }

    /**
     * The name of this Automatic Configuration.
     *
     * @return The configuration name.
     */
    public String getName() {
        return name;
    }

    /**
     * Is this configuration used by a specific build step.
     *
     * @param buildStep The buildstep class.
     * @return true if this configuration should be applied to this buildstep class.
     */
    public boolean applicableTo(Class<? extends BuildStep> buildStep) {
        return true;
    }

    /**
     * Configure the environment for this configuration.
     *
     * @param environment The environment to configure.
     * @return An unmodifiable map containing the configured environment.
     */
    public Map<String, String> configureEnvironment(Map<String, String> environment) {
        return Collections.unmodifiableMap(environment);
    }

    /** {@inheritDoc} */
    public String toString() {
        StringBuffer buf = new StringBuffer("AbstractAutomaticConfiguration");
        buf.append('[');
        buf.append("name = ");
        buf.append('\'');
        buf.append(name);
        buf.append('\'');
        buf.append(']');
        return buf.toString();
    }
}
