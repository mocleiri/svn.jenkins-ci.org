package hudson.maven;

import hudson.Util;
import hudson.model.BuildListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Maven.MavenInstallation;
import hudson.util.DelegatingOutputStream;
import hudson.util.NullStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

/**
 * Hold on to launched Maven processes so that multiple builds
 * can reuse the same Maven JVM, which leads to improved performance.
 *
 * @author Kohsuke Kawaguchi
 */
final class ProcessCache {
    /**
     * Implemented by the caller to create a new process
     * (when a new one is needed.)
     */
    interface Factory {
        /**
         * @param out
         *      The output from the process should be sent to this output stream.
         */
        Channel newProcess(BuildListener listener,OutputStream out) throws IOException, InterruptedException;
        String getMavenOpts();
        MavenInstallation getMavenInstallation();
    }

    class MavenProcess {
        /**
         * Channel connected to the maven process.
         */
        final Channel channel;
        /**
         * MAVEN_OPTS of this VM.
         */
        private final String mavenOpts;
        private final PerChannel parent;
        private final MavenInstallation installation;
        private final RedirectableOutputStream output;
        /**
         * System properties captured right after the process is created.
         * Each time the process is reused, the system properties are reset,
         * since Maven corrupts them as a side-effect of the build.
         */
        private final Properties systemProperties;

        private int age = 0;

        MavenProcess(PerChannel parent, String mavenOpts, MavenInstallation installation, Channel channel, RedirectableOutputStream output) throws IOException, InterruptedException {
            this.parent = parent;
            this.mavenOpts = mavenOpts;
            this.channel = channel;
            this.installation = installation;
            this.output = output;

            systemProperties =  channel.call(new GetSystemProperties());
        }

        boolean matches(String mavenOpts,MavenInstallation installation) {
            return Util.fixNull(this.mavenOpts).equals(Util.fixNull(mavenOpts)) && this.installation==installation;
        }

        public void recycle() throws IOException {
            if(age>=MAX_AGE || maxProcess==0)
                discard();
            else {
                output.set(new NullStream());
                // make room for the new process and reuse.
                synchronized(parent.processes) {
                    while(parent.processes.size()>=maxProcess)
                        parent.processes.removeFirst().discard();
                    parent.processes.add(this);
                }
            }
        }

        public void discard() throws IOException {
            channel.close();
        }
    }

    class PerChannel {
        /**
         * Cached processes.
         */
        private final LinkedList<MavenProcess> processes = new LinkedList<MavenProcess>();
    }

    // use WeakHashMap to avoid keeping VirtualChannel in memory.
    private final Map<VirtualChannel,PerChannel> cache = new WeakHashMap<VirtualChannel,PerChannel>();
    private final int maxProcess;

    /**
     * @param maxProcess
     *      Number of maximum processes to cache.
     */
    protected ProcessCache(int maxProcess) {
        this.maxProcess = maxProcess;
    }

    private synchronized PerChannel get(VirtualChannel owner) {
        PerChannel r = cache.get(owner);
        if(r==null)
            cache.put(owner,r=new PerChannel());
        return r;
    }

    /**
     * Gets or creates a new maven process for launch.
     */
    public MavenProcess get(VirtualChannel owner, BuildListener listener, Factory factory) throws InterruptedException, IOException {
        String mavenOpts = factory.getMavenOpts();
        MavenInstallation installation = factory.getMavenInstallation();

        PerChannel list = get(owner);
        synchronized(list) {
            for (Iterator<MavenProcess> itr = list.processes.iterator(); itr.hasNext();) {
                MavenProcess p =  itr.next();
                if(p.matches(mavenOpts, installation)) {
                    // reset the system property.
                    // this also serves as the sanity check.
                    try {
                        p.channel.call(new SetSystemProperties(p.systemProperties));
                    } catch (IOException e) {
                        p.discard();
                        itr.remove();
                        continue;
                    }

                    listener.getLogger().println("Reusing existing maven process");
                    itr.remove();
                    p.age++;
                    p.output.set(listener.getLogger());
                    return p;
                }
            }
        }

        RedirectableOutputStream out = new RedirectableOutputStream(listener.getLogger());
        return new MavenProcess(list,mavenOpts,installation,factory.newProcess(listener,out),out);
    }



    public static int MAX_AGE = 5;

    /**
     * Noop callable used for checking the sanity of the maven process in the cache.
     */
    private static class SetSystemProperties implements Callable<Object,RuntimeException>, Serializable {
        private final Properties properties;

        public SetSystemProperties(Properties properties) {
            this.properties = properties;
        }

        public Object call() {
            System.setProperties(properties);
            return null;
        }
        private static final long serialVersionUID = 1L;
    }

    private static class GetSystemProperties implements Callable<Properties,RuntimeException>, Serializable {
        public Properties call() {
            return System.getProperties();
        }
        private static final long serialVersionUID = 1L;
    }

    static class RedirectableOutputStream extends DelegatingOutputStream {
        public RedirectableOutputStream(OutputStream out) {
            super(out);
        }

        public void set(OutputStream os) {
            super.out = os;
        }
    }
}
