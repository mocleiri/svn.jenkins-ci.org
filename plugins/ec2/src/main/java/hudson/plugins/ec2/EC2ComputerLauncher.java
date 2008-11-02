package hudson.plugins.ec2;

import hudson.slaves.ComputerLauncherFilter;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.util.StreamTaskListener;

import java.io.IOException;
import java.io.PrintStream;

import com.xerox.amazonws.ec2.EC2Exception;

/**
 * {@link ComputerLauncher} for EC2 that waits for the instance to really come up before proceeding to
 * the real user-specified {@link ComputerLauncher}.
 *
 * @author Kohsuke Kawaguchi
 */
public class EC2ComputerLauncher extends ComputerLauncherFilter {
    public EC2ComputerLauncher(ComputerLauncher core) {
        super(core);
    }

    public void launch(SlaveComputer _computer, StreamTaskListener listener) throws IOException, InterruptedException {
        try {
            EC2Computer computer = (EC2Computer) _computer;
            PrintStream logger = listener.getLogger();

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

            super.launch(computer, listener);
        } catch (EC2Exception e) {
            e.printStackTrace(listener.error(e.getMessage()));
        }
    }
}
