package test;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.StringArray;

import java.io.FileWriter;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import sun.misc.Signal;
import sun.misc.SignalHandler;
import org.apache.commons.io.IOUtils;

/**
 * Daemon programming in Java.
 *
 * See http://www.enderunix.org/docs/eng/daemon.php
 */
public class Daemon 
{
    public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary)Native.loadLibrary("c",CLibrary.class);

        int fork();
        int setsid();
        int umask(int mask);
        int getpid();
        int getppid();
        int chdir(String dir);
        int execv(String file, StringArray args);
        void perror(String msg);
    }


    public static void main( String[] args ) throws Exception
    {
        System.err.print("");
        CLibrary lib = CLibrary.INSTANCE;

        if(args.length==0) {
            // if we are started normally, fork into the daemon
            int pid = lib.getpid();
            String exe = "/proc/" + pid + "/exe";
            String cmdline = IOUtils.toString(new FileInputStream("/proc/" + pid + "/cmdline"));
            // manipulate command line options. we can inrease JVM size, for example.
            List<String> newArgs = new ArrayList<String>();
            newArgs.addAll(Arrays.asList(cmdline.split("\0")));
            newArgs.add(1,"-verbose:gc");
            newArgs.add("nofork");
            args = newArgs.toArray(new String[newArgs.size()]);
            // don't know exactly why, but creating a StringArray upfront prevents
            // mysterious random exec failure. Perhaps it's related to the fact
            // that the garbage collector and other critical system threads are lost
            // after fork?
            StringArray sa = new StringArray(args);

            // run execv once (this will fail) to load all the relevant classes upfront
            lib.execv("",new StringArray(new String[]{""}));

            int i = lib.fork();
            if(i<0) {
                lib.perror("initial fork failed");
                System.exit(-1);
            }
            if(i>0) System.exit(0);     // parent exits

            // with fork, we lose all the other critical threads, to exec to Java again
            lib.execv(exe,sa);
            System.err.println("exec failed");
            lib.perror("initial exec failed");
            System.exit(-1);
            return;
        }

        // start a new process session
        lib.setsid();

        // close inherited descriptors
        System.out.close();
        System.err.close();
        System.in.close();

        // ideally we'd like to close all other descriptors, but that would close
        // jar files used as classpath, and break JVM.

        // restrict the creation mode to 750
        // lib.umask(027);

        lib.chdir("/");
        System.setProperty("user.dir","/");


        FileWriter fw = new FileWriter("/tmp/hudson.pid");
        fw.write(String.valueOf(lib.getpid()));
        fw.close();

        Signal.handle(
                new Signal("ALRM"),
                new SignalHandler() {
                    @Override
                    public void handle(Signal signal) {
                        System.out.println("Got signal");
                        System.exit(-1);
                    }
                });

        while(true) {
            Thread.sleep(1000);
            System.out.println("test");
        }
    }
}
