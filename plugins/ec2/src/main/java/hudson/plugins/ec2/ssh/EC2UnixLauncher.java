package hudson.plugins.ec2.ssh;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.ec2.EC2Computer;
import hudson.plugins.ec2.EC2ComputerLauncher;
import hudson.remoting.Channel;
import hudson.remoting.Channel.Listener;
import hudson.slaves.ComputerLauncher;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * {@link ComputerLauncher} that connects to a Unix slave on EC2 by using SSH.
 * 
 * @author Kohsuke Kawaguchi
 */
public class EC2UnixLauncher extends EC2ComputerLauncher {
    protected void launch(EC2Computer computer, PrintStream logger, Instance inst) throws IOException, EC2Exception, InterruptedException {
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
    }

    public Descriptor<ComputerLauncher> getDescriptor() {
        throw new UnsupportedOperationException();
    }
}
