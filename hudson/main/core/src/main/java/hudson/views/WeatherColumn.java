package hudson.views;

import hudson.Extension;
import hudson.model.Descriptor;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

public class WeatherColumn extends ListViewColumn {

    public Descriptor<ListViewColumn> getDescriptor() {
        return DESCRIPTOR;
    }
    
    public static final Descriptor<ListViewColumn> DESCRIPTOR = new DescriptorImpl();

    @Extension
    public static class DescriptorImpl extends Descriptor<ListViewColumn> {
        @Override
        public ListViewColumn newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            // This will be calles with req == null also the Descriptor's doc tells you not. so the default impl fails
            return new WeatherColumn();
        }

        @Override
        public String getDisplayName() {
            return "Weather";
        }
    }

}
