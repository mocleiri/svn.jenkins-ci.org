package hudson.slaves;

import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Result;
import hudson.tasks.Environment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NodeProperties {
	public static final List<NodePropertyDescriptor> PROPERTIES = Descriptor
			.toList((NodePropertyDescriptor) EnvironmentVariablesNodeProperty.DESCRIPTOR);

	/**
	 * List up all {@link NodePropertyDescriptor}s that are applicable for the
	 * given project.
	 */
	public static List<NodePropertyDescriptor> getFor(Node node) {
		List<NodePropertyDescriptor> result = new ArrayList<NodePropertyDescriptor>();
		for (NodePropertyDescriptor npd : PROPERTIES) {
			if (npd.isApplicable(node.getClass())) {
				result.add(npd);
			}
		}
		return result;
	}
	
	/**
	 * Can only be called from a build.
	 * 
	 * @param environments
	 * @param build
	 * @param listener
	 * @return false if failed
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static boolean setupEnvironment(List<Environment> environments, AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        Hudson hudson = Hudson.getInstance();
		for (NodeProperty<?> p: hudson.getNodeProperties()) {
        	Environment e = p.setUp(build, launcher, listener);
        	if(e==null)
        		return false;
        	environments.add(e);
        }
        
        Node node = Computer.currentComputer().getNode();
        if (node != hudson) { // no need to repeat if we are running on the master
        	for (NodeProperty<?> p: node.getNodeProperties()) {
        		Environment e = p.setUp(build, launcher, listener);
        		if(e==null)
        			return false;
        		environments.add(e);
        	}
        }
		
        return true;
	}

}
