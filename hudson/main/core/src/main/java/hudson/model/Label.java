package hudson.model;

import hudson.Util;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Group of {@link Node}s.
 * 
 * @author Kohsuke Kawaguchi
 */
public class Label implements Comparable<Label>, ModelObject {
    private final String name;
    private volatile Set<Node> nodes;

    public Label(String name) {
        this.name = name;
    }

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
     * Returns a human readable text that explains this label.
     */
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
    
    /*package*/ void reset() {
        nodes = null;
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
