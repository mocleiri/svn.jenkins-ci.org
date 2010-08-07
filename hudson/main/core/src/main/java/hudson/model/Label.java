/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Tom Huybrechts
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.model;

import hudson.Util;
import static hudson.Util.fixNull;

import hudson.model.label.LabelAtom;
import hudson.model.label.LabelExpression;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.Cloud;
import hudson.util.VariableResolver;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.util.TreeSet;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * Group of {@link Node}s.
 * 
 * @author Kohsuke Kawaguchi
 * @see Hudson#getLabels()
 * @see Hudson#getLabel(String) 
 */
@ExportedBean
public abstract class Label implements Comparable<Label>, ModelObject {
    protected final String name;
    private volatile Set<Node> nodes;
    private volatile Set<Cloud> clouds;

    @Exported
    public final LoadStatistics loadStatistics;
    public final NodeProvisioner nodeProvisioner;

    public Label(String name) {
        this.name = name;
         // passing these causes an infinite loop - getTotalExecutors(),getBusyExecutors());
        this.loadStatistics = new LoadStatistics(0,0) {
            @Override
            public int computeIdleExecutors() {
                return Label.this.getIdleExecutors();
            }

            @Override
            public int computeTotalExecutors() {
                return Label.this.getTotalExecutors();
            }

            @Override
            public int computeQueueLength() {
                return Hudson.getInstance().getQueue().countBuildableItemsFor(Label.this);
            }
        };
        this.nodeProvisioner = new NodeProvisioner(this, loadStatistics);
    }

    @Exported
    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return name;
    }

    /**
     * Evaluates whether the label expression is true given the specified value assignment.
     * IOW, returns true if the assignment provided by the resolver matches this label expression.
     */
    public abstract boolean matches(VariableResolver<Boolean> resolver);

    /**
     * Evaluates whether the label expression is true when an entity owns the given set of
     * {@link LabelAtom}s.
     */
    public final boolean matches(final Collection<LabelAtom> labels) {
        return matches(new VariableResolver<Boolean>() {
            public Boolean resolve(String name) {
                for (LabelAtom a : labels)
                    if (a.getName().equals(name))
                        return true;
                return false;
            }
        });
    }

    public final boolean matches(Node n) {
        return matches(n.getAssignedLabels());
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
        Set<Node> nodes = this.nodes;
        if(nodes!=null) return nodes;

        Set<Node> r = new HashSet<Node>();
        Hudson h = Hudson.getInstance();
        if(this.matches(h))
            r.add(h);
        for (Node n : h.getNodes()) {
            if(this.matches(n))
                r.add(n);
        }
        return this.nodes = Collections.unmodifiableSet(r);
    }

    /**
     * Gets all {@link Cloud}s that can launch for this label.
     */
    @Exported
    public Set<Cloud> getClouds() {
        if(clouds==null) {
            Set<Cloud> r = new HashSet<Cloud>();
            Hudson h = Hudson.getInstance();
            for (Cloud c : h.clouds) {
                if(c.canProvision(this))
                    r.add(c);
            }
            clouds = Collections.unmodifiableSet(r);
        }
        return clouds;
    }
    
    /**
     * Can jobs be assigned to this label?
     * <p>
     * The answer is yes if there is a reasonable basis to believe that Hudson can have
     * an executor under this label, given the current configuration. This includes
     * situations such as (1) there are offline slaves that have this label (2) clouds exist
     * that can provision slaves that have this label.
     */
    public boolean isAssignable() {
        for (Node n : getNodes())
            if(n.getNumExecutors()>0)
                return true;
        return !getClouds().isEmpty();
    }

    /**
     * Number of total {@link Executor}s that belong to this label.
     * <p>
     * This includes executors that belong to offline nodes, so the result
     * can be thought of as a potential capacity, whereas {@link #getTotalExecutors()}
     * is the currently functioning total number of executors.
     * <p>
     * This method doesn't take the dynamically allocatable nodes (via {@link Cloud})
     * into account. If you just want to test if there's some executors, use {@link #isAssignable()}.
     */
    public int getTotalConfiguredExecutors() {
        int r=0;
        for (Node n : getNodes())
            r += n.getNumExecutors();
        return r;
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
        if(nodes.isEmpty()) {
            Set<Cloud> clouds = getClouds();
            if(clouds.isEmpty())
                return Messages.Label_InvalidLabel();

            return Messages.Label_ProvisionedFrom(toString(clouds));
        }

        if(nodes.size()==1)
            return nodes.iterator().next().getNodeDescription();

        return Messages.Label_GroupOf(toString(nodes));
    }

    private String toString(Collection<? extends ModelObject> model) {
        boolean first=true;
        StringBuilder buf = new StringBuilder();
        for (ModelObject c : model) {
            if(buf.length()>80) {
                buf.append(",...");
                break;
            }
            if(!first)  buf.append(',');
            else        first=false;
            buf.append(c.getDisplayName());
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

    /**
     * If there's no such label defined in {@link Node} or {@link Cloud}.
     * This is usually used as a signal that this label is invalid.
     */
    public boolean isEmpty() {
        return getNodes().isEmpty() && getClouds().isEmpty();
    }
    
    /*package*/ void reset() {
        nodes = null;
        clouds = null;
    }

    /**
     * Expose this object to the remote API.
     */
    public Api getApi() {
        return new Api(this);
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return name.equals(((Label)that).name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public int compareTo(Label that) {
        return this.name.compareTo(that.name);
    }

    @Override
    public String toString() {
        return name;
    }

    public static final class ConverterImpl implements Converter {
        public ConverterImpl() {
        }

        public boolean canConvert(Class type) {
            return Label.class.isAssignableFrom(type);
        }

        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            Label src = (Label) source;
            writer.setValue(src.getName());
        }

        public Object unmarshal(HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return Hudson.getInstance().getLabel(reader.getValue());
        }
    }

    /**
     * Convers a whitespace-separate list of tokens into a set of {@link Label}s.
     *
     * @param labels
     *      Strings like "abc def ghi". Can be empty or null.
     * @return
     *      Can be empty but never null. A new writable set is always returned,
     *      so that the caller can add more to the set.
     * @since 1.308
     */
    public static Set<LabelAtom> parse(String labels) {
        Set<LabelAtom> r = new TreeSet<LabelAtom>();
        labels = fixNull(labels);
        if(labels.length()>0)
            for( String l : labels.split(" +"))
                r.add(Hudson.getInstance().getLabelAtom(l));
        return r;
    }

    /**
     * Obtains a label by its {@linkplain #getName() name}.
     */
    public static Label get(String l) {
        return Hudson.getInstance().getLabel(l);
    }

    /**
     * Parses the expression into a label expression tree.
     *
     * TODO: replace this with a real parser later
     */
    public static Label parseExpression(String labelExpression)  {
        Label l = null;
        for (String atom : labelExpression.split("&&")) {
            atom = atom.trim();
            Label r = new LabelAtom(atom);
            if (l==null)    l = r;
            else            l = new LabelExpression.And(l,r);
        }
        return l;
    }
}
