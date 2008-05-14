package hudson.plugins.clearcase;

import java.util.ArrayList;
import java.util.List;

/**
 * Request object for the mkbl cleartool command.
 * 
 *
 * @author Henrik L. Hansen
 * @see http://www.ipnom.com/ClearCase-Commands/mkbl.html
 */
public class MkblRequest {
    
    // comment is optional, null indicates no comment
    private String comment;
        
    // list of components to baseline, if empty all components will be baselined
    private List<String> components =new ArrayList<String>();
        
    private boolean baselineIdentical;
    
    // list of activities to baseline, if empty all activities will be baselined
    private List<String> activities = new ArrayList<String>(); 
    
    // should the label be full or incremental ?
    private boolean fullLabel;
    
    // name of the view, null indicates view of current directory
    private String viewTag;
    
    // name of baseline
    private String baselineName;

    public List<String> getActivities() {
        return activities;
    }

    public void setActivities(List<String> activities) {
        this.activities = activities;
    }
    
    public void addActivity(String activity) {
        activities.add(activity);
    }

    public boolean isBaselineIdentical() {
        return baselineIdentical;
    }

    public void setBaselineIdentical(boolean baselineIdentical) {
        this.baselineIdentical = baselineIdentical;
    }

    public String getBaselineName() {
        return baselineName;
    }

    public void setBaselineName(String baselineName) {
        this.baselineName = baselineName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getComponents() {
        return components;
    }

    public void setComponents(List<String> components) {
        this.components = components;
    }
    
    public void addComponent(String component) {
        components.add(component);
    }

    public boolean isFullLabel() {
        return fullLabel;
    }

    public void setFullLabel(boolean fullLabel) {
        this.fullLabel = fullLabel;
    }

    public String getViewTag() {
        return viewTag;
    }

    public void setViewTag(String viewTag) {
        this.viewTag = viewTag;
    }
    
    
}
