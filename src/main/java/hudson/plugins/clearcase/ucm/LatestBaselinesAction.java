package hudson.plugins.clearcase.ucm;

import java.util.List;

import hudson.model.Action;
import hudson.plugins.clearcase.ucm.UcmCommon.BaselineDesc;

public class LatestBaselinesAction implements Action{

	List<UcmCommon.BaselineDesc> latestBlsOnConfgiuredStream;	
	
	
	public LatestBaselinesAction(List<BaselineDesc> latestBlsOnConfgiuredStream) {
		super();
		this.latestBlsOnConfgiuredStream = latestBlsOnConfgiuredStream;
	}

	@Override
	public String getDisplayName() {
		return null;
	}

	@Override
	public String getIconFileName() {
		return null;
	}

	@Override
	public String getUrlName() {
		return null;
	}

	public List<UcmCommon.BaselineDesc> getLatestBlsOnConfgiuredStream() {
		return latestBlsOnConfgiuredStream;
	}

}
