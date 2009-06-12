package hudson.plugins.distfork;

import hudson.Launcher;
import hudson.Util;
import hudson.Extension;
import hudson.cli.CLICommand;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.Queue;
import hudson.util.StreamTaskListener;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class DistForkCommand extends CLICommand {

    @Option(name="-l",usage="Label for controlling where to execute this command")
    public String label;

    @Option(name="-n",usage="Human readable name that describe this command. Used in Hudson's UI.")
    public String name;

    @Option(name="-d",usage="Estimated duration of this task in milliseconds, or -1 if unknown")
    public long duration = -1;

    @Argument(handler=RestOfArgumentsHandler.class)
    public List<String> commands = new ArrayList<String>();


    public String getShortDescription() {
        return "forks a process on a remote machine and connects to its stdin/stdout";
    }

    protected int run() throws Exception {
        if(commands.isEmpty())
            throw new CmdLineException("No commands are specified");

        Hudson h = Hudson.getInstance();

        Label l = null;
        if (label!=null) {
            l = h.getLabel(label);
            if(l.isEmpty()) {
                stderr.println("No such label: "+label);
                return -1;
            }
        }

        // defaults to the command names
        if (name==null) {
            if(commands.size()>2)
                name = Util.join(commands.subList(0,2)," ")+" ...";
            else
                name = Util.join(commands," ");
        }

        final int[] exitCode = new int[]{-1};

        DistForkTask t = new DistForkTask(l, name, duration, new Runnable() {
            public void run() {
                // TODO: need a way to set environment variables
                // need to be able to control current directory?
                StreamTaskListener listener = new StreamTaskListener(stdout);
                try {
                    Launcher launcher = Computer.currentComputer().getNode().createLauncher(listener);
                    exitCode[0] = launcher.launch(commands.toArray(new String[commands.size()]),
                            new String[0], stdout, null).join();
                } catch (Exception e) {
                    e.printStackTrace(listener.error("Failed to execute a process"));
                    exitCode[0] = -1;
                }
            }
        });

        Queue q = h.getQueue();
        q.add(t,0);

        // TODO: this is stupid
        while(q.contains(t)) {
            Thread.sleep(1000);
        }

        return exitCode[0];
    }
}
