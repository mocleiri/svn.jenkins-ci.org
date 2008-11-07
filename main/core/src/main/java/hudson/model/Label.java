package hudson.model;

import hudson.Util;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Group of {@link Node}s.
 * 
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public class Label implements Comparable<Label>, ModelObject {
    private final String name;
    private volatile Set<Node> nodes;

    /**
     * Number of busy executors and how it changes over time.
     */
    public final MultiStageTimeSeries busyExecutors;
    /**
     * Number of total executors and how it changes over time.
     */
    public final MultiStageTimeSeries totalExecutors;

    /**
     * With 0.90 decay ratio for every 10sec, half reduction is about 1 min.
     */
    private static final float DECAY = 0.9f;

    public Label(String name) {
        this.name = name;
        this.totalExecutors = new MultiStageTimeSeries(getTotalExecutors(),DECAY);
        this.busyExecutors = new MultiStageTimeSeries(getBusyExecutors(),DECAY);
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
            if(c.isOnline())
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
            if(c.isOnline())
                r += c.countBusy();
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

    /**
     * Start updating the load average.
     */
    /*package*/ static void registerLoadMonitor() {
        Trigger.timer.scheduleAtFixedRate(
            new SafeTimerTask() {
                protected void doRun() {
                    Hudson h = Hudson.getInstance();
                    for( Label l : h.getLabels() ) {
                        l.totalExecutors.update(l.getTotalExecutors());
                        l.busyExecutors .update(l.getBusyExecutors());
                    }
                }
            }, 10*1000, 10*1000
        );                
    }
}
