package hudson.plugins.ssh_cli;

import hudson.Extension;
import hudson.Util;
import hudson.util.IOUtils;

import java.io.IOException;

/**
 * Test command that just echos back the arguments.
 * @author Kohsuke Kawaguchi
 */
@Extension
public class EchoCommand extends LightCLICommand {
    @Override
    public String getShortDescription() {
        return "Echos the arguments";
    }

    @Override
    protected int execute() throws IOException {
        stdout.println(Util.join(arguments," "));
        return 0;
    }
}
