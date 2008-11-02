package hudson.plugins.ec2.ssh;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.StreamGobbler;
import com.xerox.amazonws.ec2.ConsoleOutput;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;
import hudson.model.Descriptor;
import hudson.plugins.ec2.EC2Computer;
import hudson.plugins.ec2.EC2Cloud;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.util.StreamTaskListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Kohsuke Kawaguchi
 */
public class EC2UnixLauncher extends ComputerLauncher {
    public void launch(SlaveComputer _computer, StreamTaskListener listener) {
        EC2Computer computer = (EC2Computer)_computer;

        Instance inst = null;
        Jec2 ec2 = EC2Cloud.get().connect();

        // test console output
        ConsoleOutput console;
        while(true) {
            console = ec2.getConsoleOutput(inst.getInstanceId());
            String output = console.getOutput();
            if(output ==null || output.length()==0) {
                log.info("Waiting while the console output is posted");
                Thread.sleep(5000);
                continue;
            }
            break;
        }

        // attempt to connect
        Connection conn = new Connection(inst.getDnsName());
        conn.connect(new HostKeyVerifierImpl(console));

        boolean isAuthenticated = conn.authenticateWithPublicKey("root", new File("/home/kohsuke/.ec2/thekey.private"), "");

        if (!isAuthenticated)
            throw new IOException("Authentication failed.");

        // send some file
        SCPClient scp = conn.createSCPClient();
        scp.put("Hello, world!".getBytes(),"hello.txt","/tmp");

        // execute stuff
        Session sess = conn.openSession();
        sess.execCommand("uname -a && date && uptime && who");

        InputStream stdout = new StreamGobbler(sess.getStdout());
        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
        _computer.setChannel();

        System.out.println("Here is some information about the remote host:");

        while (true) {
            String line = br.readLine();
            if (line == null)
                break;
            System.out.println(line);
        }

        // try scp

        sess.close();
        conn.close();

    }

    public Descriptor<ComputerLauncher> getDescriptor() {
        // TODO
        throw new UnsupportedOperationException();
    }
}
