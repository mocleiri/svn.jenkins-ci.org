package org.jvnet.hudson.plugins.port_allocator;

import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.ResourceActivity;
import hudson.tasks.BuildWrapper;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class PortAllocator extends BuildWrapper /* implements ResourceActivity */
{
    private final String portVariables;

    private PortAllocator(String portVariables){
        this.portVariables = portVariables;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getPortVariables() {
        return portVariables;
    }

    public Environment setUp(Build build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        final String[] portVars = portVariables.split(" ");

        final Computer cur = Executor.currentExecutor().getOwner();

        AllocatedPortAction prevAlloc = build.getPreviousBuild().getAction(AllocatedPortAction.class);

        final PortAllocationManager pam = PortAllocationManager.getManager(cur);
        final HashMap<String,Integer> portMap = new HashMap<String,Integer>();

        // TODO: allocation here

        build.addAction(new AllocatedPortAction(portMap));

        return new Environment() {

            @Override
            public void buildEnvVars(Map<String, String> env) {
                for(String portVar: portVars){
                    int freeport = pam.allocate(0);
                    portMap.put(portVar, freeport);
                    //set the environment variable
                    env.put(portVar, String.valueOf(freeport));
                }

            }

            public boolean tearDown(Build build, BuildListener listener) throws IOException, InterruptedException {
                for(String portVar: portVars){
                    pam.free(portMap.get(portVar));
                    portMap.remove(portVar);
                }
                return true;
            }
        };
    }

    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }


    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

        DescriptorImpl() {
            super(PortAllocator.class);
            load();
        }

        public String getDisplayName() {
            return "Run Port Allocator during build";
        }

        public String getHelpFile() {
            return "/plugin/port-allocator/help.html";
        }

        public boolean configure(StaplerRequest req) throws FormException {
            req.bindParameters(this,"portallocator.");
            save();
            return true;
        }

        public PortAllocator newInstance(StaplerRequest req) throws FormException {
            return new PortAllocator(req.getParameter("portallocator.portVariables"));
        }

        private static final long serialVersionUID = 1L;
    }

}
