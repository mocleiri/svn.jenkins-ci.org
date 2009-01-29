package hudson.slaves;

import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.util.List;

public class NodeProperties {
    public static final List<NodePropertyDescriptor> PROPERTIES = Descriptor.toList(
    		(NodePropertyDescriptor) EnvironmentVariablesNodeProperty.DESCRIPTOR
    );

    /**
     * List up all {@link NodePropertyDescriptor}s that are applicable for the given project.
     *
     * @return
     *      The signature doesn't use {@link BuildWrapperDescriptor} to maintain compatibility
     *      with {@link BuildWrapper} implementations before 1.150.
     */
    public static List<NodePropertyDescriptor> getFor(Node node) {
		return PROPERTIES;
    }

}
