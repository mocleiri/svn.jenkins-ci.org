package hudson.plugins.jclouds;

import hudson.Extension;

import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class SlicehostCloud extends JCloudsCloud {
	

	@DataBoundConstructor
    public SlicehostCloud(String user, String secret,
			String instanceCapStr, List<JCloudTemplate> templates) {
		super("slicehost", user, secret, instanceCapStr, templates);
    }

    @Extension
    public static class DescriptorImpl extends JCloudsCloud.DescriptorImpl {

    	@Override
    	public String getDisplayName() {
    		return "Slicehost";
    	}
    }

}
