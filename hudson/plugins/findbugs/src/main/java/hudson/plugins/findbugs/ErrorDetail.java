package hudson.plugins.findbugs;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;

/**
 * Result object to visualize the errors during parsing.
 */
public class ErrorDetail implements ModelObject  {
    /** Current build as owner of this action. */
    private final AbstractBuild<?, ?> owner;
    /** All errors of the project. */
    private final String errors;

    /**
     * Creates a new instance of <code>ErrorDetail</code>.
     *
     * @param owner
     *            current build as owner of this action.
     * @param errors
     *            all modules of the project
     */
    public ErrorDetail(final AbstractBuild<?, ?> owner, final String errors) {
        this.owner = owner;
        this.errors = errors;
    }

    /**
     * Returns the build as owner of this action.
     *
     * @return the owner
     */
    public final AbstractBuild<?, ?> getOwner() {
        return owner;
    }

    /** {@inheritDoc} */
    public String getDisplayName() {
        return "Errors";
    }

    /**
     * Returns the errors in the project.
     *
     * @return the errors in the project
     */
    public String getErrors() {
        return errors;
    }
}

