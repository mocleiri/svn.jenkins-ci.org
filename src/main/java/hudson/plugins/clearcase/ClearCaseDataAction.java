package hudson.plugins.clearcase;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.model.Action;
import hudson.plugins.clearcase.ucm.UcmCommon;

@ExportedBean
public class ClearCaseDataAction implements Action{

	@Exported(visibility=3)
	public List<UcmCommon.BaselineDesc> latestBlsOnConfgiuredStream;
	
	@Exported(visibility=3)
	public String csepc;
	
	@Exported(visibility=3)
	public String stream;
	
	private List<String> usedViewNamesList;
	
	private boolean deleteViewsWhenBuildEnds;
	
	public ClearCaseDataAction() {
		super();
		this.deleteViewsWhenBuildEnds = false;
		this.usedViewNamesList = new ArrayList<String>();
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

	public String getCsepc() {
		return csepc;
	}

	public void setCsepc(String csepc) {
		this.csepc = csepc;
	}

	public String getStream() {
		return stream;
	}

	public void setStream(String stream) {
		this.stream = stream;
	}

	public List<String> getUsedViewNamesList() {
		return usedViewNamesList;
	}

	public void setUsedViewNamesList(List<String> usedViewNamesList) {
		this.usedViewNamesList = usedViewNamesList;
	}

	public void setLatestBlsOnConfgiuredStream(
			List<UcmCommon.BaselineDesc> latestBlsOnConfgiuredStream) {
		this.latestBlsOnConfgiuredStream = latestBlsOnConfgiuredStream;
	}

	public List<UcmCommon.BaselineDesc> getLatestBlsOnConfgiuredStream() {
		return latestBlsOnConfgiuredStream;
	}

	public boolean isDeleteViewsWhenBuildEnds() {
		return deleteViewsWhenBuildEnds;
	}

	public void setDeleteViewsWhenBuildEnds(boolean deleteViewsWhenBuildEnds) {
		this.deleteViewsWhenBuildEnds = deleteViewsWhenBuildEnds;
	}	
	

}
