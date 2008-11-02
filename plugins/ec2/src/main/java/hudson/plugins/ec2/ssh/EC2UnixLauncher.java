package hudson.plugins.ec2.ssh;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.ec2.EC2Computer;
import hudson.remoting.Channel;
import hudson.remoting.Channel.Listener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * {@link ComputerLauncher} that connects to a Unix slave on EC2 by using SSH.
 * 
 * @author Kohsuke Kawaguchi
 */
public class EC2UnixLauncher extends ComputerLauncher {
    public void launch(SlaveComputer _computer, StreamTaskListener listener) {
        try {
            EC2Computer computer = (EC2Computer)_computer;
            PrintStream logger = listener.getLogger();

            Instance inst = computer.describeInstance();

            // wait until EC2 instance comes up and post console output
            boolean reportedWaiting = false;
            OUTER:
            while(true) {
                switch (computer.getState()) {
                    case PENDING:
                    case RUNNING:
                        String console = computer.getConsoleOutput();
                        if(console==null || console.length()==0) {
                            if(!reportedWaiting) {
                                reportedWaiting = true;
                                logger.println("Waiting for the EC2 instance to boot up");
                            }
                            Thread.sleep(5000); // check every 5 secs
                            continue OUTER;
                        }
                        break OUTER;
                    case SHUTTING_DOWN:
                    case TERMINATED:
                        // abort
                        logger.println("The instance "+computer.getInstanceId()+" appears to be shut down. Aborting launch.");
                        return;
                }
            }

            logger.println("Connecting to "+inst.getDnsName());
            final Connection conn = new Connection(inst.getDnsName());
            conn.connect(new HostKeyVerifierImpl(computer.getConsoleOutput()));

            // TODO: where do we store the private key?
            boolean isAuthenticated = conn.authenticateWithPublicKey("root", new File("/home/kohsuke/.ec2/thekey.private"), "");

            if (!isAuthenticated) {
                logger.println("Authentication failed");
                return;
            }

            logger.println("Copying slave.jar");
            SCPClient scp = conn.createSCPClient();
            scp.put(Hudson.getInstance().getJnlpJars("slave.jar").readFully(),
                    "slave.jar","/tmp");

            logger.println("Launching slave agent");
            final Session sess = conn.openSession();
            sess.execCommand("java -jar /tmp/slave.jar");
            computer.setChannel(sess.getStdout(),sess.getStdin(),logger,new Listener() {
                public void onClosed(Channel channel, IOException cause) {
                    sess.close();
                    conn.close();
                }
            });
        } catch (EC2Exception e) {
            e.printStackTrace(listener.error(e.getMessage()));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public Descriptor<ComputerLauncher> getDescriptor() {
        // TODO
        throw new UnsupportedOperationException();
    }
}
