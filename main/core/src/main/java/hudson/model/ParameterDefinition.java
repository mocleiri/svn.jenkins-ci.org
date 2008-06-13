package hudson.model;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import hudson.util.DescriptorList;

/**
 * Defines a parameter that has been defined for a certain project. Subclasses
 * will define the types of parameters (e.g. String, boolean, ...)
 */
public abstract class ParameterDefinition implements
		Describable<ParameterDefinition> {

	private final String name;

	public String getName() {
		return name;
	}

	public ParameterDefinition(String name) {
		super();
		this.name = name;
	}

	public abstract ParameterDescriptor getDescriptor();

	public abstract ParameterValue newInstance(StaplerRequest req, JSONObject jo);

	/**
	 * A list of available parameter definition types
	 */
	public static final DescriptorList<ParameterDefinition> LIST = new DescriptorList<ParameterDefinition>();

	public abstract static class ParameterDescriptor extends
			Descriptor<ParameterDefinition> {

		protected ParameterDescriptor(Class klazz) {
			super(klazz);
		}

		public String getValuePage() {
			return getViewPage(clazz, "index.jelly");
		}

		@Override
		public String getDisplayName() {
			return "Parameter";
		}

	}

}
