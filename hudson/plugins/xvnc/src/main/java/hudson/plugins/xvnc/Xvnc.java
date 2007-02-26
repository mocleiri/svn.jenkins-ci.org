package hudson.plugins.xvnc;

import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildWrapper;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link BuildWrapper} that runs <tt>xvnc</tt>.
 * 
 * @author Kohsuke Kawaguchi
 */
public class Xvnc extends BuildWrapper {
    public Environment setUp(Build build, final Launcher launcher, BuildListener listener) throws IOException {
        final PrintStream logger = listener.getLogger();

        String cmd = Util.nullify(DESCRIPTOR.xvnc);
        if(cmd==null)
            cmd = "vncserver :$DISPLAY_NUMBER";

        final int displayNumber = allocator.allocate();
        final String actualCmd = Util.replaceMacro(cmd, Collections.singletonMap("DISPLAY_NUMBER",String.valueOf(displayNumber)));

        logger.println("Starting xvnc");

        final Proc proc = launcher.launch(actualCmd, new String[0], logger, build.getProject().getWorkspace());
        Matcher m = Pattern.compile("([^ ]*vncserver ).*:\\d+.*").matcher(actualCmd);
        final String vncserverCommand;
        if (m.matches()) {
            // Command just started the server; -kill will stop it.
            vncserverCommand = m.group(1);
            int exit = proc.join();
            if (exit != 0) {
                // Do not release it; it may be "stuck" until cleaned up by an administrator.
                //allocator.free(displayNumber);
                throw new IOException("Failed to run '" + actualCmd + "' (exit code " + exit + "), blacklisting display #" + displayNumber +
                                      "; consider adding to your Hudson launch script: killall Xvnc; rm -fv /tmp/.X*-lock /tmp/.X11-unix/X*");
            }
        } else {
            vncserverCommand = null;
        }

        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.put("DISPLAY",":"+displayNumber);
            }

            public boolean tearDown(Build build, BuildListener listener) throws IOException {
                logger.println("Terminating xvnc");
                if (vncserverCommand != null) {
                    // #173: stopping the wrapper script will accomplish nothing. It has already exited, in fact.
                    launcher.launch(vncserverCommand + "-kill :" + displayNumber, new String[0], logger, build.getProject().getWorkspace()).join();
                } else {
                    // Assume it can be shut down by being killed.
                    proc.kill();
                }
                allocator.free(displayNumber);

                return true;
            }
        };
    }

    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Manages display numbers in use.
     */
    private static final DisplayAllocator allocator = new DisplayAllocator();
    
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
        /**
         * xvnc command line. This can include macro.
         *
         * If null, the default will kick in.
         */
        public String xvnc;

        DescriptorImpl() {
            super(Xvnc.class);
            load();
        }

        public String getDisplayName() {
            return "Run Xvnc during build";
        }

        public boolean configure(StaplerRequest req) throws FormException {
            req.bindParameters(this,"xvnc.");
            save();
            return true;
        }

        public String getHelpFile() {
            return "/plugin/xvnc/help-projectConfig.html";
        }

        public Xvnc newInstance(StaplerRequest req) throws FormException {
            return new Xvnc();
        }
    }
}
