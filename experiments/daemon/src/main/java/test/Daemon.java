package test;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.FileWriter;

/**
 * Hello world!
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
        int chdir(String dir);
    }


    public static void main( String[] args ) throws Exception
    {
        CLibrary lib = CLibrary.INSTANCE;
        int i = lib.fork();
        if(i<0) System.exit(-1);    // fork failed
        if(i>0) System.exit(0);     // parent exits

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

        while(true)
            Thread.sleep(1000*10);
    }
}
