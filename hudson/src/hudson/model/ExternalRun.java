package hudson.model;

import hudson.Proc;
import hudson.util.DualOutputStream;
import hudson.util.DecodingStream;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;

/**
 * @author Kohsuke Kawaguchi
 */
public class ExternalRun extends Run<ExternalJob,ExternalRun> {
    /**
     * Loads a run from a log file.
     */
    ExternalRun(ExternalJob owner, File runDir, ExternalRun prevRun ) throws IOException {
        super(owner,runDir,prevRun);
    }

    /**
     * Creates a new run.
     */
    ExternalRun(ExternalJob project) throws IOException {
        super(project);
    }

    /**
     * Instead of performing a build, run the specified command,
     * record the log and its exit code, then call it a build.
     */
    public void run(final String[] cmd) {
        run(new Runner() {
            public Result run(BuildListener listener) throws Exception {
                Proc proc = new Proc(cmd,getEnvVars(),System.in,new DualOutputStream(System.out,listener.getLogger()));
                return proc.join()==0?Result.SUCCESS:Result.FAILURE;
            }

            public void post(BuildListener listener) {
                // do nothing
            }
        });
    }

    /**
     * Instead of performing a build, accept the log and the return code
     * from a remote machine in an XML format of:
     *
     * <pre><xmp>
     * <run>
     *  <log>...console output...</log>
     *  <result>exit code</result>
     * </run>
     * </xmp></pre>
     */
    public void acceptRemoteSubmission(final Reader in) {
        run(new Runner() {
            public Result run(BuildListener listener) throws Exception {
                PrintStream logger = new PrintStream(new DecodingStream(listener.getLogger()));

                XmlPullParser xpp = new MXParser();
                xpp.setInput(in);
                xpp.nextTag();  // get to the <run>
                xpp.nextTag();  // get to the <log>
                while(xpp.nextToken()!=XmlPullParser.END_TAG) {
                    int type = xpp.getEventType();
                    if(type==XmlPullParser.TEXT
                    || type==XmlPullParser.CDSECT)
                        logger.print(xpp.getText());
                }
                xpp.nextTag(); // get to <result>

                return Integer.parseInt(xpp.nextText())==0?Result.SUCCESS:Result.FAILURE;
            }

            public void post(BuildListener listener) {
                // do nothing
            }
        });
    }

}
