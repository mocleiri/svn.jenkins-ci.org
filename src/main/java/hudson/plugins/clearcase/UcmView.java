package hudson.plugins.clearcase;

import hudson.plugins.clearcase.ucm.UcmCommon;
import hudson.plugins.clearcase.ucm.UcmCommon.Baseline;

import java.util.Collections;
import java.util.List;

/**
 * A Clearcase UCM View.
 * 
 * @author vlatombe
 * 
 */
public class UcmView extends View {
    private final String streamSelector;
    private final List<UcmCommon.Baseline> baselines;

    public UcmView(String name, String path, String configSpec, String streamSelector, List<Baseline> baselines) {
        super(name, path, configSpec);
        this.streamSelector = streamSelector;
        if (baselines != null) {
            this.baselines = Collections.unmodifiableList(baselines);
        } else {
            this.baselines = null;
        }
    }

    public List<UcmCommon.Baseline> getBaselines() {
        return baselines;
    }

    public String getStreamSelector() {
        return streamSelector;
    }
    
    @Override
    public String toString() {
        return "UcmView [baselines=" + baselines + ", streamSelector="
                + streamSelector + "]";
    }

}
