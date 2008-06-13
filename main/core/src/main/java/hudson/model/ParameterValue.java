package hudson.model;

/**
 * A value for a parameter in a build.
 */
public interface ParameterValue {

	String getName();
	Object getValue();
	
}
