package hudson.tasks;

import hudson.Launcher;
import hudson.Util;
import hudson.util.FormFieldValidator;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Kohsuke Kawaguchi
 */
public class Ant extends Builder {

    private final String targets;

    /**
     * Identifies {@link AntInstallation} to be used.
     */
    private final String antName;

    public Ant(String targets,String antName) {
        this.targets = targets;
        this.antName = antName;
    }

    public String getTargets() {
        return targets;
    }

    /**
     * Gets the Ant to invoke,
     * or null to invoke the default one.
     */
    public AntInstallation getAnt() {
        for( AntInstallation i : DESCRIPTOR.getInstallations() ) {
            if(antName!=null && i.getName().equals(antName))
                return i;
        }
        return null;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, Launcher launcher, BuildListener listener) {
        Project proj = build.getProject();

        String cmd;

        String execName;
        if(onWindows)
            execName = "ant.bat";
        else
            execName = "ant";

        AntInstallation ai = getAnt();
        if(ai==null)
            cmd = execName+' '+targets;
        else {
            File exec = ai.getExecutable();
            if(!ai.getExists()) {
                listener.fatalError(exec+" doesn't exist");
                return false;
            }
            cmd = exec.getPath()+' '+targets;
        }

        Map<String,String> env = build.getEnvVars();
        if(ai!=null)
            env.put("ANT_HOME",ai.getAntHome());

        if(onWindows) {
            // on Windows, executing batch file can't return the correct error code,
            // so we need to wrap it into cmd.exe.
            // double %% is needed because we want ERRORLEVEL to be expanded after
            // batch file executed, not before. This alone shows how broken Windows is...
            cmd = "cmd.exe /C "+cmd+" && exit %%ERRORLEVEL%%";
        }

        try {
            int r = launcher.launch(cmd,env,listener.getLogger(),proj.getModuleRoot()).join();
            return r==0;
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            e.printStackTrace( listener.fatalError("command execution failed") );
            return false;
        }
    }

    public Action getProjectAction(Project project) {
        return null;
    }

    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<Builder> {
        private DescriptorImpl() {
            super(Ant.class);
        }

        public String getHelpFile() {
            return "/help/project-config/ant.html";
        }

        public String getDisplayName() {
            return "Invoke top-level Ant targets";
        }

        public AntInstallation[] getInstallations() {
            AntInstallation[] r = (AntInstallation[]) getProperties().get("installations");

            if(r==null)
                return new AntInstallation[0];

            return r.clone();
        }

        public boolean configure(HttpServletRequest req) {
            boolean r = true;

            int i;
            String[] names = req.getParameterValues("ant_name");
            String[] homes = req.getParameterValues("ant_home");
            int len;
            if(names!=null && homes!=null)
                len = Math.min(names.length,homes.length);
            else
                len = 0;
            AntInstallation[] insts = new AntInstallation[len];

            for( i=0; i<len; i++ ) {
                if(names[i].length()==0 || homes[i].length()==0)    continue;
                insts[i] = new AntInstallation(names[i],homes[i]);
            }

            getProperties().put("installations",insts);

            save();

            return r;
        }

        public Builder newInstance(StaplerRequest req) {
            return new Ant(req.getParameter("ant_targets"),req.getParameter("ant_version"));
        }

    //
    // web methods
    //
        /**
         * Checks if the ANT_HOME is valid.
         */
        public void doCheckAntHome( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
            // this can be used to check the existence of a file on the server, so needs to be protected
            new FormFieldValidator(req,rsp,true) {
                public void check() throws IOException, ServletException {
                    File f = getFileParameter("value");
                    if(!f.isDirectory()) {
                        error(f+" is not a directory");
                        return;
                    }

                    File antJar = new File(f,"lib/ant.jar");
                    if(!antJar.exists()) {
                        error(f+" doesn't look like an Ant directory");
                        return;
                    }

                    ok();
                }
            }.process();
        }
    }

    public static final class AntInstallation {
        private final String name;
        private final String antHome;

        public AntInstallation(String name, String antHome) {
            this.name = name;
            this.antHome = antHome;
        }

        /**
         * install directory.
         */
        public String getAntHome() {
            return antHome;
        }

        /**
         * Human readable display name.
         */
        public String getName() {
            return name;
        }

        public File getExecutable() {
            String execName;
            if(File.separatorChar=='\\')
                execName = "ant.bat";
            else
                execName = "ant";

            return new File(getAntHome(),"bin/"+execName);
        }

        /**
         * Returns true if the executable exists.
         */
        public boolean getExists() {
            return getExecutable().exists();
        }
    }

    private static final boolean onWindows = File.separatorChar == '\\';
}
