package hudson;

import hudson.model.ExternalJob;
import hudson.model.ExternalRun;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Result;
import hudson.util.DualOutputStream;
import hudson.util.EncodingStream;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point to Hudson from command line.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) {
        try {
            System.exit(run(args));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static int run(String[] args) throws Exception {
        String home = getHudsonHome();
        if(home==null) {
            System.err.println("HUDSON_HOME is not set.");
            return -1;
        }

        if(home.startsWith("http")) {
            return remotePost(args);
        } else {
            return localPost(args);
        }
    }

    private static String getHudsonHome() {
        return Hudson.masterEnvVars.get("HUDSON_HOME");
    }

    /**
     * Run command and place the result directly into the local installation of Hudson.
     */
    public static int localPost(String[] args) throws Exception {
        Hudson app = new Hudson(new File(getHudsonHome()));

        Job job = app.getJob(args[0]);
        if(!(job instanceof ExternalJob)) {
            System.err.println(args[0]+" is not a valid external job name in Hudson");
            return -1;
        }
        ExternalJob ejob = (ExternalJob) job;

        ExternalRun run = ejob.newBuild();

        // run the command
        List<String> cmd = new ArrayList<String>();
        for( int i=1; i<args.length; i++ )
            cmd.add(args[i]);
        run.run(cmd.toArray(new String[cmd.size()]));

        return run.getResult()==Result.SUCCESS?0:1;
    }

    /**
     * Run command and place the result to a remote Hudson installation
     */
    public static int remotePost(String[] args) throws Exception {
        String projectName = args[0];

        // start a remote connection
        HttpURLConnection con = (HttpURLConnection) new URL(getHudsonHome()+"job/"+projectName+"/postBuildResult").openConnection();
        con.setDoOutput(true);
        con.connect();
        OutputStream os = con.getOutputStream();
        Writer w = new OutputStreamWriter(os,"UTF-8");
        w.write("<?xml version='1.0' encoding='UTF-8'?>");
        w.write("<run><log encoding='hexBinary'>");
        w.flush();

        // run the command
        List<String> cmd = new ArrayList<String>();
        for( int i=1; i<args.length; i++ )
            cmd.add(args[i]);
        Proc proc = new Proc(cmd.toArray(new String[0]),(String[])null,System.in,
            new DualOutputStream(System.out,new EncodingStream(os)));

        int ret = proc.join();

        w.write("</log><result>"+ret+"</result></run>");
        w.close();

        if(con.getResponseCode()!=200) {
            Util.copyStream(con.getErrorStream(),System.err);
        }

        return ret;
    }
}
