package test;

import groovy.lang.Binding;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.GroovyShell;
import hudson.cli.CLI;
import hudson.remoting.Channel;
import hudson.remoting.FastPipedInputStream;
import hudson.remoting.FastPipedOutputStream;
import hudson.remoting.Which;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class Droovy extends GroovyObjectSupport implements Serializable {

    public final URL hudson;
    /**
     * Channel to Hudson.
     */
    private transient final CLI cli;

    private transient final ExecutorService exec;

    private transient final Set<Server> servers = new HashSet<Server>();

    public Droovy(URL hudson) throws IOException, InterruptedException {
        this.hudson = hudson;
        exec = Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
        cli = new CLI(hudson,exec);    
    }

    public static void main(String[] args) throws Exception {
        Droovy droovy = new Droovy(new URL(args[0]));
        try {
            droovy.execute(System.in);
        } finally {
            droovy.close();
        }
    }

    private void execute(InputStream in) {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setScriptBaseClass(ClosureScript.class.getName());

        Binding binding = new Binding();
        binding.setVariable("droovy",this);
        GroovyShell groovy = new GroovyShell(binding,cc);

        ClosureScript s = (ClosureScript)groovy.parse(in);
        s.setDelegate(this);
        s.run();
    }

    public void close() throws IOException, InterruptedException {
        for (Server s : servers)
            s.close();
        cli.close();
        exec.shutdown();
    }

    /**
     * Provisions a new node.
     */
    public Server connect(String name) throws IOException {
        FastPipedOutputStream p1 = new FastPipedOutputStream();
        final FastPipedInputStream p1i = new FastPipedInputStream(p1);

        final FastPipedOutputStream p2 = new FastPipedOutputStream();
        FastPipedInputStream p2i = new FastPipedInputStream(p2);

        exec.submit(new Callable() {
            public Object call() throws Exception {

                try {
                    return cli.execute(Arrays.asList(
                            "dist-fork",
                            "-f",
                            "remoting.jar=" + Which.jarFile(Channel.class),
                            "java",
    //                        "-Xrunjdwp:transport=dt_socket,server=y,address=8000", // debug opt
                            "-jar",
                            "remoting.jar"),
                            p1i,
                            p2, System.err);
//                        new TeeOutputStream(p2,new FileOutputStream("/tmp/incoming")), System.err);
                } catch (IOException e) {
                    System.err.println(e);
                    return null;
                }
            }
        });

        Server s = new test.Server(this, new Channel(name, exec, p2i,
//                new TeeOutputStream(p1,new FileOutputStream("/tmp/outgoing"))));
                p1));
        servers.add(s);
        return s;
    }
}
