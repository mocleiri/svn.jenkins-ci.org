package hudson.model;

/**
 * A value for a parameter in a build.
 *
 * <h2>Persistence</h2>
 * <p>
 * Instances of {@link ParameterValue}s are persisted into build's <tt>build.xml</tt>
 * through XStream, so instances need to be persistable.
 *
 * <h2>Assocaited Views</h2>
 * <h4>value.jelly</h4>
 * The <tt>value.jelly</tt> view contributes a UI fragment to display the parameter
 * values used for a build.
 *
 * <h2>Notes</h2>
 * <ol>
 * <li>{@link ParameterValue} is used to record values of the past build, but
 *     {@link ParameterDefinition} used back then might be gone already, or represent
 *     a different parameter now. So don't try to use the name to infer
 *     {@link ParameterDefinition} is.
 * </ol>
 * @see ParameterDefinition
 */
public abstract class ParameterValue {
    protected final String name;

    protected ParameterValue(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public abstract Object getValue();
}
