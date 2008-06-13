package hudson.model;

import org.kohsuke.stapler.DataBoundConstructor;

public class StringParameterValue implements ParameterValue {

	private final String name;
	private final String value;

	@DataBoundConstructor
	public StringParameterValue(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	
	public String getName() {
		return name;
	}
	
}
