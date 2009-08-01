package hudson.plugins.parameterizedtrigger;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;

import java.io.IOException;

public abstract class AbstractBuildParameters implements Describable<AbstractBuildParameters> {

	public abstract Action getAction(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException;

	@Override
	public Descriptor<AbstractBuildParameters> getDescriptor() {
		System.out.println(getClass() + "->" + Hudson.getInstance().getDescriptor(getClass()));
        return Hudson.getInstance().getDescriptor(getClass());
	}
	
}
