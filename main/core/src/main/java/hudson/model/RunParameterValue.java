package hudson.model;

import org.kohsuke.stapler.DataBoundConstructor;

public class RunParameterValue implements ParameterValue {

	private final String name;
	private final Run run;
	
	@DataBoundConstructor
	public RunParameterValue(String name, Run run) {
		super();
		this.run = run;
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Run getValue() {
		return run;
	}

}
