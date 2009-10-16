package hudson.plugins.parameterizedtrigger;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.scm.RevisionParameterAction;
import hudson.scm.SubversionTagAction;
import hudson.scm.SubversionSCM.SvnInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class SubversionRevisionBuildParameters extends AbstractBuildParameters {

	@DataBoundConstructor
	public SubversionRevisionBuildParameters() {
	}

	@Override
	public Action getAction(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {

		SubversionTagAction tagAction = build
				.getAction(SubversionTagAction.class);
		if (tagAction == null) {
			listener.getLogger().println(
				"[parameterizedtrigger] no SubversionTagAction found -- is this project an SVN project ?");
			return null;
		}

		List<SvnInfo> infos = new ArrayList<SvnInfo>(tagAction.getTags()
				.keySet());
		return new RevisionParameterAction(infos);
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<AbstractBuildParameters> {

		@Override
		public String getDisplayName() {
			return "Subversion revision";
		}

	}

}
