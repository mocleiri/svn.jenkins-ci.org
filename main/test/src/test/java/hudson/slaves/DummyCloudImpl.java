package hudson.slaves;

import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Label;
import hudson.slaves.NodeProvisioner.PlannedNode;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * {@link Cloud} implementation useful for testing.
 *
 * <p>
 * This implementation launches "java -jar slave.jar" on the localhost when provisioning a new slave.
 *
 * @author Kohsuke Kawaguchi
*/
public class DummyCloudImpl extends Cloud {
    private final transient HudsonTestCase caller;

    /**
     * Configurable delay between the {@link Cloud#provision(Label,int)} and the actual launch of a slave,
     * to emulate a real cloud that takes some time for provisioning a new system.
     *
     * <p>
     * Number of milliseconds.
     */
    private final int delay;

    // stats counter to perform assertions later
    public int numProvisioned;

    public DummyCloudImpl(HudsonTestCase caller, int delay) {
        super("test");
        this.caller = caller;
        this.delay = delay;
    }

    public Collection<PlannedNode> provision(Label label, int excessWorkload) {
        List<PlannedNode> r = new ArrayList<PlannedNode>();
        while(excessWorkload>0) {
            System.out.println("Provisioning");
            numProvisioned++;
            Future<Node> f = Computer.threadPoolForRemoting.submit(new Launcher(delay));
            r.add(new PlannedNode(name+" #"+numProvisioned,f,1));
            excessWorkload-=1;
        }
        return r;
    }

    private final class Launcher implements Callable<Node> {
        private final long time;
        /**
         * This is so that we can find out the status of Callable from the debugger.
         */
        private volatile Computer computer;

        private Launcher(long time) {
            this.time = time;
        }

        public Node call() throws Exception {
            // simulate the delay in provisioning a new slave,
            // since it's normally some async operation.
            Thread.sleep(time);
            
            System.out.println("launching slave");
            DumbSlave slave = caller.createSlave();
            computer = slave.toComputer();
            computer.connect(false).get();
            synchronized (DummyCloudImpl.this) {
                System.out.println(computer.getName()+" launch"+(computer.isOnline()?"ed successfully":" failed"));
                System.out.println(computer.getLog());
            }
            return slave;
        }
    }

    public Descriptor<Cloud> getDescriptor() {
        throw new UnsupportedOperationException();
    }
}
