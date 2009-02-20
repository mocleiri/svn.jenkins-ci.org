package de.galan.hudson.monitor;

import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;



public class MonitorDescriptor extends Descriptor<Publisher> {

	public static final String ACTION_LOGO_LARGE = "/plugin/monitor/icons/monitor-32x32.png";
	public static final String ACTION_LOGO_MEDIUM = "/plugin/monitor/icons/monitor-22x22.png";


	protected MonitorDescriptor() {
		super(MonitorPublisher.class);
	}


	@Override
	public String getDisplayName() {
		return "Build Monitor";
	}


	@Override
	public Publisher newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
		return new MonitorPublisher();
	}

}
