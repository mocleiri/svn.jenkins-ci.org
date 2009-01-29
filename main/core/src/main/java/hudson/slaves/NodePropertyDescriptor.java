package hudson.slaves;

import hudson.model.Descriptor;
import hudson.model.JobProperty;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import net.sf.json.JSONObject;

import org.jvnet.tiger_types.Types;
import org.kohsuke.stapler.StaplerRequest;

public abstract class NodePropertyDescriptor extends Descriptor<NodeProperty<?>> {

	protected NodePropertyDescriptor() {}
	
    /**
     * {@inheritDoc}
     *
     * @return
     *      null to avoid setting an instance of {@link JobProperty} to the target project.
     */
    public NodeProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        return super.newInstance(req, formData);
    }
	
    /**
     * Returns true if this {@link JobProperty} type is applicable to the
     * given job type.
     * 
     * <p>
     * The default implementation of this method checks if the given job type is assignable to 'J' of
     * {@link JobProperty}<tt>&lt;J></tt>, but subtypes can extend this to change this behavior.
     *
     * @return
     *      true to indicate applicable, in which case the property will be
     *      displayed in the configuration screen of this job.
     */
    public boolean isApplicable(Class<? extends NodeProperty> nodeType) {
        Type parameterization = Types.getBaseClass(clazz, NodeProperty.class);
        if (parameterization instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) parameterization;
            Class applicable = Types.erasure(Types.getTypeArgument(pt, 0));
            return applicable.isAssignableFrom(nodeType);
        } else {
            throw new AssertionError(clazz+" doesn't properly parameterize NodeProperty. The isApplicable() method must be overriden.");
        }
    }

}
