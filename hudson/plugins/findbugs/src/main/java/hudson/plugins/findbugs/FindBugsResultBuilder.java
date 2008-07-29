package hudson.plugins.findbugs;

import hudson.model.AbstractBuild;
import hudson.plugins.findbugs.util.ParserResult;

/**
 * Creates a new FindBugs result based on the values of a previous build and the
 * current project.
 *
 * @author Ulli Hafner
 */
public class FindBugsResultBuilder {
    /**
     * Creates a result that persists the FindBugs information for the
     * specified build.
     *
     * @param build
     *            the build to create the action for
     * @param project
     *            the project containing the annotations
     * @return the result action
     */
    public FindBugsResult build(final AbstractBuild<?, ?> build, final ParserResult project) {
        Object previous = build.getPreviousBuild();
        while (previous instanceof AbstractBuild<?, ?>) {
            AbstractBuild<?, ?> previousBuild = (AbstractBuild<?, ?>)previous;
            FindBugsResultAction previousAction = previousBuild.getAction(FindBugsResultAction.class);
            if (previousAction != null) {
                return new FindBugsResult(build, project, previousAction.getResult());
            }
            previous = previousBuild.getPreviousBuild();
        }
        return new FindBugsResult(build, project);
    }
}

