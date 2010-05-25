package hudson.plugins.sauce_ondemand;

import com.saucelabs.rest.Credential;
import com.saucelabs.rest.SauceTunnel;
import com.saucelabs.rest.SauceTunnelFactory;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.tasks.BuildWrapper;
import hudson.util.IOException2;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

/**
 * {@link BuildWrapper} that sets up the SoD SSH tunnel.
 * @author Kohsuke Kawaguchi
 */
public class SoDBuildWrapper extends BuildWrapper {
    /**
     * Tunnel configuration.
     */
    private List<Tunnel> tunnels;

    @DataBoundConstructor
    public SoDBuildWrapper(List<Tunnel> tunnels) {
        this.tunnels = tunnels;
    }

    public List<Tunnel> getTunnels() {
        return Collections.unmodifiableList(tunnels);
    }

    private boolean hasAutoRemoteHost() {
        for (Tunnel t : tunnels)
            if (t.isAutoRemoteHost())
                return true;
        return false;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Starting Sauce OnDemand SSH tunnels");
        final String autoRemoteHostName = Util.getDigestOf(build.getFullDisplayName());
        final ITunnelHolder tunnels = Computer.currentComputer().getChannel().call(new TunnelStarter(autoRemoteHostName));

        return new Environment() {
            /**
             * If the user wants automatic host name allocation, we expose that via an environment variable.
             */
            @Override
            public void buildEnvVars(Map<String, String> env) {
                if (hasAutoRemoteHost())
                    env.put("SAUCE_ONDEMAND_HOST",autoRemoteHostName);
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                listener.getLogger().println("Shutting down Sauce OnDemand SSH tunnels");
                tunnels.close(listener);
                return true;
            }
        };
    }

    private interface ITunnelHolder {
        public void close(TaskListener listener);
    }

    private static final class TunnelHolder implements ITunnelHolder, Serializable {
        final List<SauceTunnel> tunnels = new ArrayList<SauceTunnel>();

        public Object writeReplace() {
            return Channel.current().export(ITunnelHolder.class,this);
        }

        public void close(TaskListener listener) {
            for (SauceTunnel tunnel : tunnels) {
                try {
                    tunnel.disconnectAll();
                    tunnel.destroy();
                } catch (IOException e) {
                    e.printStackTrace(listener.error("Failed to shut down a tunnel"));
                }
            }
        }
    }

    private final class TunnelStarter implements Callable<ITunnelHolder,IOException> {
        private final String username, key;
        private final int timeout = TIMEOUT;
        private String autoRemoteHostName;

        private TunnelStarter(String randomHostName) {
            PluginImpl p = PluginImpl.get();
            this.username = p.getUsername();
            this.key = Secret.toString(p.getApiKey());
            this.autoRemoteHostName = randomHostName;
        }

        public ITunnelHolder call() throws IOException {
            TunnelHolder r = new TunnelHolder();

            boolean success = false;

            try {
                SauceTunnelFactory stf = new SauceTunnelFactory(new Credential(username, key));
                for (Tunnel tunnel : tunnels) {
                    List<String> domains;
                    if (tunnel.isAutoRemoteHost()) {
                        domains = singletonList(autoRemoteHostName);
                    } else {
                        domains = tunnel.getDomainList();
                    }
                    SauceTunnel t = stf.create(domains);
                    r.tunnels.add(t);
                }
                for (int i = 0; i < tunnels.size(); i++) {
                    Tunnel s = tunnels.get(i);
                    SauceTunnel t = r.tunnels.get(i);
                    try {
                        t.waitUntilRunning(timeout);
                    } catch (InterruptedException e) {
                        throw new IOException2("Aborted",e);
                    }
                    t.connect(s.remotePort, s.localHost, s.localPort);
                }
                success = true;
            } finally {
                if (!success) {
                    // if the tunnel set up failed, revert the ones that are already created
                    for (SauceTunnel t : r.tunnels) {
                        t.destroy();
                    }
                }
            }
            return r;
        }

        private static final long serialVersionUID = 1L;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
        @Override
        public String getDisplayName() {
            return "Sauce OnDemand SSH tunnel";
        }
    }

    /**
     * Time out for how long we wait until the tunnel to be set up.
     */
    public static int TIMEOUT = Integer.getInteger(SoDBuildWrapper.class.getName()+".timeout", 60 * 1000);
}
