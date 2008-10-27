package hudson.slaves;

import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.model.Node;
import hudson.util.DescriptorList;

/**
 * {@link Descriptor} for {@link Slave}.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class NodeDescriptor extends Descriptor<Node> {
    protected NodeDescriptor(Class<? extends Node> clazz) {
        super(clazz);
    }

    public final String newInstanceDetailPage() {
        return '/'+clazz.getName().replace('.','/').replace('$','/')+"/newInstanceDetail.jelly";
    }

    /**
     * All the registered instances.
     */
    public static final DescriptorList<Node> ALL = new DescriptorList<Node>();

    static {
        ALL.load(DumbSlave.class);
    }
}
