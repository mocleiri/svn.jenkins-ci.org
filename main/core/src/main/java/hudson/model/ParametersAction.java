package hudson.model;

import java.util.List;

/**
 * Records the parameter values used for a build
 */
public class ParametersAction implements Action {
	
	private final List<ParameterValue> parameters;
	private final AbstractBuild<?,?> build;

	public ParametersAction(List<ParameterValue> parameters, AbstractBuild<?,?> build) {
		this.parameters = parameters;
		this.build = build;
	}
	
	public String substitute(String text) {
		for (ParameterValue parameter: parameters) {
			text = text.replaceAll("\\$\\{" + parameter.getName() + "\\}", parameter.getValue().toString());
		}
		return text;
	}
	
	public AbstractBuild<?,?> getBuild() {
		return build;
	}

	public List<ParameterValue> getParameters() {
		return parameters;
	}

	@Override
	public String getDisplayName() {
		return "Parameters";
	}

	@Override
	public String getIconFileName() {
		return "clock.gif";
	}

	@Override
	public String getUrlName() {
		return "parameters";
	}

}
