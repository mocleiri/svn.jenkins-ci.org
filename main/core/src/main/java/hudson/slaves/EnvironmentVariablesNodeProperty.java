package hudson.slaves;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.tasks.Environment;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;

public class EnvironmentVariablesNodeProperty extends NodeProperty<Node> {

    /**
     * Slave-specific environment variables
     */
    private Map<String,String> envVars;
    
    @DataBoundConstructor
    public EnvironmentVariablesNodeProperty(List<EnvVars.Entry> env) {
        this.envVars = EnvVars.toMap(env);
    }

    public EnvironmentVariablesNodeProperty(EnvVars.Entry... env) {
        this(Arrays.asList(env));
    }
	
	public NodePropertyDescriptor getDescriptor() {
		return DESCRIPTOR;
	}
	
	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {
		return new Environment() {

			@Override
			public void buildEnvVars(Map<String, String> env) {
				for(Map.Entry<String, String> envVar: envVars.entrySet()) {
					env.put(envVar.getKey(), envVar.getValue());
				}
			}
			
		};
	}

	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	
	public static class DescriptorImpl extends NodePropertyDescriptor {

		@Override
		public String getDisplayName() {
			return "Node-specific environment variables"; 
			// TODO specialize this for master (global environment variables)
		}
		
	}

	public Map<String, String> getEnvVars() {
		return envVars;
	}

}
