package hudson.tasks;

import hudson.Launcher;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class Ant implements Builder {

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
        if(File.separatorChar=='\\')
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
                if(names[i].length()==0)    continue;
                insts[i] = new AntInstallation(names[i],homes[i]);
            }

            getProperties().put("installations",insts);

            save();

            return r;
        }

        public Builder newInstance(HttpServletRequest req) {
            return new Ant(req.getParameter("ant_targets"),req.getParameter("ant_version"));
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
}
