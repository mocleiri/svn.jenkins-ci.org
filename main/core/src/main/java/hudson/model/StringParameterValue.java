package hudson.model;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * {@link ParameterValue} created from {@link StringParameterDefinition}.
 */
public class StringParameterValue extends ParameterValue {
    private final String value;

    @DataBoundConstructor
    public StringParameterValue(String name, String value) {
        super(name);
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
