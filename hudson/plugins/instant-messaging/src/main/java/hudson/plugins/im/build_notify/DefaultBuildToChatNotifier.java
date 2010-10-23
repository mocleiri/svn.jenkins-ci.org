package hudson.plugins.im.build_notify;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.im.IMPublisher;
import hudson.plugins.im.tools.BuildHelper;
import hudson.scm.ChangeLogSet.Entry;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * {@link BuildToChatNotifier} that maintains the traditional behaviour of {@link IMPublisher}.
 *
 * @author Kohsuke Kawaguchi
 */
public class DefaultBuildToChatNotifier extends SummaryOnlyBuildToChatNotifier {
    @DataBoundConstructor
    public DefaultBuildToChatNotifier() {
    }

    @Override
    public String buildStartMessage(IMPublisher publisher, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder(super.buildStartMessage(publisher, build, listener));

        if (build.getPreviousBuild() != null) {
            sb.append(" (previous build: ")
                .append(BuildHelper.getResultDescription(build.getPreviousBuild()));

            if (build.getPreviousBuild().getResult().isWorseThan(Result.SUCCESS)) {
                AbstractBuild<?, ?> lastSuccessfulBuild = BuildHelper.getPreviousSuccessfulBuild(build);
                if (lastSuccessfulBuild != null) {
                    sb.append(" -- last ").append(Result.SUCCESS).append(" #")
                        .append(lastSuccessfulBuild.getNumber())
                        .append(" ").append(lastSuccessfulBuild.getTimestampString()).append(" ago");
                }
            }
            sb.append(")");
        }
        
        return sb.toString();
    }

    @Override
    public String buildCompletionMessage(IMPublisher publisher, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder(super.buildCompletionMessage(publisher,build,listener));

        if (!build.getChangeSet().isEmptySet()) {
            boolean hasManyChangeSets = build.getChangeSet().getItems().length > 1;
            for (Entry entry : build.getChangeSet()) {
                sb.append("\n");
                if (hasManyChangeSets) {
                    sb.append("* ");
                }
                sb.append(entry.getAuthor()).append(": ").append(entry.getMsg());
            }
        }

        return sb.toString();
    }

    // set a high ordinal so that this comes in the top, as the default selection
    @Extension(ordinal=100)
    public static class DescriptorImpl extends BuildToChatNotifierDescriptor {
        @Override
		public String getDisplayName() {
            return "Summary + SCM changes";
        }
    }
}
