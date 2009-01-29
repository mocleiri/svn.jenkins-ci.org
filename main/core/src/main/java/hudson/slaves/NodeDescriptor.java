package hudson.slaves;

import hudson.Functions;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.util.DescribableList;
import hudson.util.DescriptorList;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link Descriptor} for {@link Slave}.
 *
 * <h2>Views</h2>
 * <p>
 * This object needs to have <tt>newInstanceDetail.jelly</tt> view, which shows up in
 * <tt>http://server/hudson/computers/new</tt> page as an explanation of this job type.
 *
 * <h2>Other Implementation Notes</h2>
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class NodeDescriptor extends Descriptor<Node> {
    protected NodeDescriptor(Class<? extends Node> clazz) {
        super(clazz);
    }

    protected NodeDescriptor() {
    }

    @Override
	public Node newInstance(StaplerRequest req, JSONObject formData)
			throws FormException {
		Node node = super.newInstance(req, formData);
		DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties = new DescribableList<NodeProperty<?>, NodePropertyDescriptor>(
				Hudson.getInstance()); // TODO verify owner
		nodeProperties.rebuild(req, formData, Functions
				.getNodePropertyDescriptors(node));
		node.setNodeProperties(nodeProperties);
		return node;
	}
    
    public final String newInstanceDetailPage() {
        return '/'+clazz.getName().replace('.','/').replace('$','/')+"/newInstanceDetail.jelly";
    }

    @Override
    public String getConfigPage() {
        return getViewPage(clazz, "configure-entries.jelly");
    }

    /**
     * All the registered instances.
     */
    public static final DescriptorList<Node> ALL = new DescriptorList<Node>();

    static {
        ALL.load(DumbSlave.class);
    }
}
