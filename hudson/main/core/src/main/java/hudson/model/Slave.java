package hudson.model;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.RemoteLauncher;
import hudson.Util;
import hudson.slaves.ComputerStartMethod;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.CommandStartMethod;
import hudson.slaves.JNLPStartMethod;
import hudson.slaves.SlaveComputer;
import hudson.model.Descriptor.FormException;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tasks.DynamicLabeler;
import hudson.tasks.LabelFinder;
import hudson.util.ClockDifference;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Information about a Hudson slave node.
 *
 * <p>
 * Ideally this would have been in the <tt>hudson.slaves</tt> package,
 * but for compatibility reasons, it can't.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Slave implements Node, Serializable {
    /**
     * Name of this slave node.
     */
    protected final String name;

    /**
     * Description of this node.
     */
    private final String description;

    /**
     * Path to the root of the workspace
     * from the view point of this node, such as "/hudson"
     */
    protected final String remoteFS;

    /**
     * Number of executors of this node.
     */
    private int numExecutors = 2;

    /**
     * Job allocation strategy.
     */
    private Mode mode;

    /**
     * Slave availablility strategy.
     */
    private RetentionStrategy availabilityStrategy;

    /**
     * The starter that will startup this slave.
     */
    private ComputerStartMethod startMethod;

    /**
     * Whitespace-separated labels.
     */
    private String label="";

    /**
     * Lazily computed set of labels from {@link #label}.
     */
    private transient volatile Set<Label> labels;

    private transient volatile Set<Label> dynamicLabels;
    private transient volatile int dynamicLabelsInstanceHash;

    /**
     * @stapler-constructor
     */
    public Slave(String name, String description, String remoteFS, String numExecutors,
                 Mode mode, String label) throws FormException {
        this.name = name;
        this.description = description;
        this.numExecutors = Util.tryParseNumber(numExecutors, 1).intValue();
        this.mode = mode;
        this.remoteFS = remoteFS;
        this.label = Util.fixNull(label).trim();
        getAssignedLabels();    // compute labels now

        if (name.equals(""))
            throw new FormException(Messages.Slave_InvalidConfig_NoName(), null);

        // this prevents the config from being saved when slaves are offline.
        // on a large deployment with a lot of slaves, some slaves are bound to be offline,
        // so this check is harmful.
        //if (!localFS.exists())
        //    throw new FormException("Invalid slave configuration for " + name + ". No such directory exists: " + localFS, null);
        if (remoteFS.equals(""))
            throw new FormException(Messages.Slave_InvalidConfig_NoRemoteDir(name), null);

        if (this.numExecutors<=0)
            throw new FormException(Messages.Slave_InvalidConfig_Executors(name), null);
    }

    public ComputerStartMethod getStartMethod() {
        return startMethod == null ? new JNLPStartMethod() : startMethod;
    }

    public void setStartMethod(ComputerStartMethod startMethod) {
        this.startMethod = startMethod;
    }

    public String getRemoteFS() {
        return remoteFS;
    }

    public String getNodeName() {
        return name;
    }

    public String getNodeDescription() {
        return description;
    }

    public int getNumExecutors() {
        return numExecutors;
    }

    public Mode getMode() {
        return mode;
    }

    public RetentionStrategy getAvailabilityStrategy() {
        return availabilityStrategy == null ? RetentionStrategy.Always.INSTANCE : availabilityStrategy;
    }

    public void setAvailabilityStrategy(RetentionStrategy availabilityStrategy) {
        this.availabilityStrategy = availabilityStrategy;
    }

    public String getLabelString() {
        return Util.fixNull(label).trim();
    }

    public Set<Label> getAssignedLabels() {
        // todo refactor to make dynamic labels a bit less hacky
        if(labels==null || isChangedDynamicLabels()) {
            Set<Label> r = new HashSet<Label>();
            String ls = getLabelString();
            if(ls.length()>0) {
                for( String l : ls.split(" +")) {
                    r.add(Hudson.getInstance().getLabel(l));
                }
            }
            r.add(getSelfLabel());
            r.addAll(getDynamicLabels());
            this.labels = Collections.unmodifiableSet(r);
        }
        return labels;
    }

    /**
     * Check if we should rebuild the list of dynamic labels.
     * @todo make less hacky
     * @return
     */
    private boolean isChangedDynamicLabels() {
        Computer comp = getComputer();
        if (comp == null) {
            return dynamicLabelsInstanceHash != 0;
        } else {
            if (dynamicLabelsInstanceHash == comp.hashCode()) {
                return false;
            }
            dynamicLabels = null; // force a re-calc
            return true;
        }
    }

    /**
     * Returns the possibly empty set of labels that it has been determined as supported by this node.
     *
     * @todo make less hacky
     * @see hudson.tasks.LabelFinder
     *
     * @return
     *      never null.
     */
    public Set<Label> getDynamicLabels() {
        // another thread may preempt and set dynamicLabels field to null,
        // so a care needs to be taken to avoid race conditions under all circumstances.
        Set<Label> labels = dynamicLabels;
        if (labels != null)     return labels;

        synchronized (this) {
            labels = dynamicLabels;
            if (labels != null)     return labels;

            dynamicLabels = labels = new HashSet<Label>();
            Computer computer = getComputer();
            VirtualChannel channel;
            if (computer != null && (channel = computer.getChannel()) != null) {
                dynamicLabelsInstanceHash = computer.hashCode();
                for (DynamicLabeler labeler : LabelFinder.LABELERS) {
                    for (String label : labeler.findLabels(channel)) {
                        labels.add(Hudson.getInstance().getLabel(label));
                    }
                }
            } else {
                dynamicLabelsInstanceHash = 0;
            }

            return labels;
        }
    }

    public Label getSelfLabel() {
        return Hudson.getInstance().getLabel(name);
    }

    public ClockDifference getClockDifference() throws IOException, InterruptedException {
        VirtualChannel channel = getComputer().getChannel();
        if(channel==null)
            throw new IOException(getNodeName()+" is offline");

        long startTime = System.currentTimeMillis();
        long slaveTime = channel.call(new Callable<Long,RuntimeException>() {
            public Long call() {
                return System.currentTimeMillis();
            }
        });
        long endTime = System.currentTimeMillis();

        return new ClockDifference((startTime+endTime)/2 - slaveTime);
    }

    public Computer createComputer() {
        return new SlaveComputer(this);
    }

    public FilePath getWorkspaceFor(TopLevelItem item) {
        FilePath r = getWorkspaceRoot();
        if(r==null)     return null;    // offline
        return r.child(item.getName());
    }

    public FilePath getRootPath() {
        return createPath(remoteFS);
    }

    public FilePath createPath(String absolutePath) {
        VirtualChannel ch = getComputer().getChannel();
        if(ch==null)    return null;    // offline
        return new FilePath(ch,absolutePath);
    }

    /**
     * Root directory on this slave where all the job workspaces are laid out.
     * @return
     *      null if not connected.
     */
    public FilePath getWorkspaceRoot() {
        FilePath r = getRootPath();
        if(r==null) return null;
        return r.child("workspace");
    }

    /**
     * Web-bound object used to serve jar files for JNLP.
     */
    public static final class JnlpJar {
        private final String fileName;

        public JnlpJar(String fileName) {
            this.fileName = fileName;
        }

        public void doIndex( StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            URL res = req.getServletContext().getResource("/WEB-INF/" + fileName);
            if(res==null) {
                // during the development this path doesn't have the files.
                res = new URL(new File(".").getAbsoluteFile().toURL(),"target/generated-resources/WEB-INF/"+fileName);
            }

            URLConnection con = res.openConnection();
            InputStream in = con.getInputStream();
            rsp.serveFile(req, in, con.getLastModified(), con.getContentLength(), "*.jar" );
            in.close();
        }

    }

    public Launcher createLauncher(TaskListener listener) {
        SlaveComputer c = getComputer();
        return new RemoteLauncher(listener, c.getChannel(), c.isUnix());
    }

    /**
     * Gets the corresponding computer object.
     */
    public SlaveComputer getComputer() {
        return (SlaveComputer)Hudson.getInstance().getComputer(this);
    }

    public Computer toComputer() {
        return getComputer();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Slave that = (Slave) o;

        return name.equals(that.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Invoked by XStream when this object is read into memory.
     */
    private Object readResolve() {
        // convert the old format to the new one
        if(command!=null && agentCommand==null) {
            if(command.length()>0)  command += ' ';
            agentCommand = command+"java -jar ~/bin/slave.jar";
        }
        if (startMethod == null) {
            startMethod = (agentCommand == null || agentCommand.trim().length() == 0)
                    ? new JNLPStartMethod()
                    : new CommandStartMethod(agentCommand);
        }
        return this;
    }

//
// backwrad compatibility
//
    /**
     * In Hudson < 1.69 this was used to store the local file path
     * to the remote workspace. No longer in use.
     *
     * @deprecated
     *      ... but still in use during the transition.
     */
    private File localFS;

    /**
     * In Hudson < 1.69 this was used to store the command
     * to connect to the remote machine, like "ssh myslave".
     *
     * @deprecated
     */
    private transient String command;
    /**
     * Command line to launch the agent, like
     * "ssh myslave java -jar /path/to/hudson-remoting.jar"
     */
    private transient String agentCommand;


//    static {
//        ConvertUtils.register(new Converter(){
//            public Object convert(Class type, Object value) {
//                if (value != null) {
//                System.out.println("CVT: " + type + " from (" + value.getClass() + ") " + value);
//                } else {
//                    System.out.println("CVT: " + type + " from " + value);
//                }
//                return null;  //To change body of implemented methods use File | Settings | File Templates.
//            }
//        }, ComputerStartMethod.class);
//    }
}
