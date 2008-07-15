package hudson.model;

import java.util.List;

/**
 * Records the parameter values used for a build.
 *
 * <P>
 * This object is associated with the build record so that we remember what parameters
 * were used for what build.
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
	
	public Object getValue(String name) {
		for (ParameterValue p: parameters) {
			if (p.getName().equals(name)) {
				return p.getValue();
			}
		}
		return null;
	}

}
