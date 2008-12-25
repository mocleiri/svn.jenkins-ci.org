package hudson.model;

import hudson.Util;
import hudson.slaves.NodeProvisioner;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Group of {@link Node}s.
 * 
 * @author Kohsuke Kawaguchi
 * @see Hudson#getLabels()
 * @see Hudson#getLabel(String) 
 */
@ExportedBean
public class Label implements Comparable<Label>, ModelObject {
    private final String name;
    private volatile Set<Node> nodes;

    public final LoadStatistics load;
    public final NodeProvisioner nodeProvisioner;

    public Label(String name) {
        this.name = name;
         // passing these causes an infinite loop - getTotalExecutors(),getBusyExecutors());
        this.load = new LoadStatistics(0,0) {
            @Override
            public int computeIdleExecutors() {
                return Label.this.getIdleExecutors();
            }
            @Override
            public int computeQueueLength() {
                return Hudson.getInstance().getQueue().countBuildableItemsFor(Label.this);
            }
        };
        this.nodeProvisioner = new NodeProvisioner(this,load);
    }

    @Exported
    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return name;
    }

    /**
     * Returns true if this label is a "self label",
     * which means the label is the name of a {@link Node}.
     */
    public boolean isSelfLabel() {
        Set<Node> nodes = getNodes();
        return nodes.size() == 1 && nodes.iterator().next().getSelfLabel() == this;
    }

    /**
     * Gets all {@link Node}s that belong to this label.
     */
    @Exported
    public Set<Node> getNodes() {
        if(nodes==null) {
            Set<Node> r = new HashSet<Node>();
            Hudson h = Hudson.getInstance();
            if(h.getAssignedLabels().contains(this))
                r.add(h);
            for (Slave s : h.getSlaves()) {
                if(s.getAssignedLabels().contains(this))
                    r.add(s);
            }
            nodes = Collections.unmodifiableSet(r);
        }
        return nodes;
    }

    /**
     * Number of total {@link Executor}s that belong to this label that are functioning.
     * <p>
     * This excludes executors that belong to offline nodes.
     */
    @Exported
    public int getTotalExecutors() {
        int r=0;
        for (Node n : getNodes()) {
            Computer c = n.toComputer();
            if(c!=null && c.isOnline())
                r += c.countExecutors();
        }
        return r;
    }

    /**
     * Number of busy {@link Executor}s that are carrying out some work right now.
     */
    @Exported
    public int getBusyExecutors() {
        int r=0;
        for (Node n : getNodes()) {
            Computer c = n.toComputer();
            if(c!=null && c.isOnline())
                r += c.countBusy();
        }
        return r;
    }

    /**
     * Number of idle {@link Executor}s that can start working immediately.
     */
    @Exported
    public int getIdleExecutors() {
        int r=0;
        for (Node n : getNodes()) {
            Computer c = n.toComputer();
            if(c!=null && (c.isOnline() || c.isConnecting()))
                r += c.countIdle();
        }
        return r;
    }

    /**
     * Returns true if all the nodes of this label is offline.
     */
    @Exported
    public boolean isOffline() {
        for (Node n : getNodes()) {
            if(n.toComputer() != null && !n.toComputer().isOffline())
                return false;
        }
        return true;
    }

    /**
     * Returns a human readable text that explains this label.
     */
    @Exported
    public String getDescription() {
        Set<Node> nodes = getNodes();
        if(nodes.isEmpty())
            return "invalid label";
        if(nodes.size()==1) {
            return nodes.iterator().next().getNodeDescription();
        }

        StringBuilder buf = new StringBuilder("group of ");
        boolean first=true;
        for (Node n : nodes) {
            if(buf.length()>80) {
                buf.append(",...");
                break;
            }
            if(!first)  buf.append(',');
            else        first=false;
            buf.append(n.getNodeName());
        }
        return buf.toString();
    }

    /**
     * Returns projects that are tied on this node.
     */
    @Exported
    public List<AbstractProject> getTiedJobs() {
        List<AbstractProject> r = new ArrayList<AbstractProject>();
        for( AbstractProject p : Util.filter(Hudson.getInstance().getItems(),AbstractProject.class) ) {
            if(this.equals(p.getAssignedLabel()))
                r.add(p);
        }
        return r;
    }

    public boolean contains(Node node) {
        return getNodes().contains(node);
    }

    public boolean isEmpty() {
        return getNodes().isEmpty();
    }
    
    /*package*/ void reset() {
        nodes = null;
    }

    /**
     * Expose this object to the remote API.
     */
    public Api getApi() {
        return new Api(this);
    }

    public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return name.equals(((Label)that).name);

    }

    public int hashCode() {
        return name.hashCode();
    }

    public int compareTo(Label that) {
        return this.name.compareTo(that.name);
    }
}
