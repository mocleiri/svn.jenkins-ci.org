package hudson.plugins.clearcase;

import java.util.List;

import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.ucm.LatestBaselinesAction;
import hudson.plugins.clearcase.ucm.UcmCommon;

public class ClearCaseReportAction implements Action {

	private AbstractBuild<?,?> build;
	private String cspec;
	private String stream;
	private static String urlName = "clearcaseInformation";
	
	public ClearCaseReportAction(AbstractBuild<?,?> build) {
        this.build = build;
        
        if (build.getProject().getScm() instanceof ClearCaseSCM) {     
        	this.cspec = ((ClearCaseSCM) build.getProject().getScm()).getEffectiveConfigSpec();
        }
        else {
        	this.stream = ((ClearCaseUcmSCM) build.getProject().getScm()).getStream();
        }
	}
	
    public String getIconFileName(){
    	return "gear2.gif";
    }
    
    public String getDisplayName(){
    	return "ClearCase Information";
    }
    
    public String getUrlName(){
    	return urlName;
    }
    
    public static String getUrlNameStat(){
    	return urlName;
    }

    // Used by the index.jelly of this class to include the sidebar.jelly
    public AbstractBuild<?, ?> getOwner() {
		return build;
	}

    public String getConfigSpecHtml() {
    	String configSpecHtml = cspec;
    	configSpecHtml = configSpecHtml.replaceAll("\n","<br/>");
    	return configSpecHtml;
    }
    
    public boolean isCspec() {
    	return cspec.length() > 0;
    }
    
    public List<UcmCommon.BaselineDesc> getBaselines() {
    	LatestBaselinesAction baselinesAction = build.getPreviousBuild().getAction(LatestBaselinesAction.class);
    	
    	if (baselinesAction != null) {
    		return baselinesAction.getLatestBlsOnConfgiuredStream();
    	}
    	else {
    		return null;
    	}
    }
    
    public boolean isBaselineInfo() {
    	LatestBaselinesAction baselinesAction = build.getPreviousBuild().getAction(LatestBaselinesAction.class);    	
    	return (baselinesAction != null);
    }

	public String getStream() {
		return stream;
	}   
    
}
