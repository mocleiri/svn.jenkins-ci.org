package hudson.tasks;

import hudson.model.Descriptor;

import javax.servlet.http.HttpServletRequest;

/**
 * Metadata of a {@link BuildStep} class.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class BuildStepDescriptor extends Descriptor<BuildStep> {

    protected BuildStepDescriptor(Class<? extends BuildStep> clazz) {
        super(clazz);
    }

    /**
     * Returns the resource path to the help screen HTML, if any.
     */
    public String getHelpFile() {
        return "";
    }
}
