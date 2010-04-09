package hudson.plugins.clearcase;

import hudson.model.Action;
import hudson.plugins.clearcase.ucm.UcmCommon;

import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class ClearCaseDataAction implements Action {

    @Exported(visibility = 3)
    public List<UcmCommon.Baseline> latestBlsOnConfiguredStream;

    @Exported(visibility = 3)
    public String configSpec;

    @Exported(visibility = 3)
    public String streamSelector;

    public ClearCaseDataAction() {
        super();
    }

    public ClearCaseDataAction(View view) {
        if (view != null) {
            configSpec = view.getConfigSpec();
            if (view instanceof UcmView) {
                UcmView ucmView = (UcmView) view;
                streamSelector = ucmView.getStreamSelector();
            }
        }
    }

    public String getConfigSpec() {
        return configSpec;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    public List<UcmCommon.Baseline> getLatestBlsOnConfiguredStream() {
        return latestBlsOnConfiguredStream;
    }

    public String getStreamSelector() {
        return streamSelector;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    public boolean hasBaselinesInformation() {
        return latestBlsOnConfiguredStream != null && !latestBlsOnConfiguredStream.isEmpty();
    }
    
    public void setConfigSpec(String configSpec) {
        this.configSpec = configSpec;
    }

    public void setLatestBlsOnConfiguredStream(
            List<UcmCommon.Baseline> latestBlsOnConfiguredStream) {
        this.latestBlsOnConfiguredStream = latestBlsOnConfiguredStream;
    }

    public void setStreamSelector(String streamSelector) {
        this.streamSelector = streamSelector;
    }

}
