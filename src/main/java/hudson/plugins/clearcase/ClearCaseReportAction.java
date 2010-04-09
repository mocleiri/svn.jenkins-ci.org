package hudson.plugins.clearcase;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.plugins.clearcase.ucm.UcmCommon;

import java.util.List;

public class ClearCaseReportAction implements Action {

    private AbstractBuild<?,?> build;
    private ClearCaseDataAction clearCaseDataAction;
    private static String urlName = "clearcaseInformation";
	
	public ClearCaseReportAction(AbstractBuild<?,?> build) {
        this.build = build;
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
        String configSpecHtml = getConfigSpec();
        configSpecHtml = configSpecHtml.replaceAll("\n","<br/>");
        return configSpecHtml;
    }
    
    public List<UcmCommon.Baseline> getBaselines() {
        ClearCaseDataAction clearCaseDataAction = getClearCaseDataAction();
        
        if (clearCaseDataAction != null) {
            return clearCaseDataAction.getLatestBlsOnConfiguredStream();
        } else {
            return null;
        }
    }

    private ClearCaseDataAction getClearCaseDataAction() {
        if (clearCaseDataAction == null) {
            clearCaseDataAction = build.getAction(ClearCaseDataAction.class);
        }
        return clearCaseDataAction;
    }
    
    public boolean isBaselineInfo() {
        ClearCaseDataAction dataAction = getClearCaseDataAction();
        if (dataAction != null) {
            return dataAction.hasBaselinesInformation();
        }
        return false;
    }
    
	public String getStreamSelector() {
        ClearCaseDataAction dataAction = getClearCaseDataAction();
        if (dataAction != null) {
            return dataAction.getStreamSelector();
        }
        return null;
	}
	
	public String getConfigSpec() {
        ClearCaseDataAction dataAction = getClearCaseDataAction();
        if (dataAction != null) {
            return dataAction.getConfigSpec();
        }
        return null;
	}
    
}
