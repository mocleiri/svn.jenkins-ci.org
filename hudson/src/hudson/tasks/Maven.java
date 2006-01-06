package hudson.tasks;

import hudson.Proc;
import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Project;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Build by using Maven.
 *
 * @author Kohsuke Kawaguchi
 */
public class Maven implements BuildStep {

    private final String targets;

    /**
     * Identifies {@link MavenInstallation} to be used.
     */
    private final String mavenName;

    public Maven(String targets,String mavenName) {
        this.targets = targets;
        this.mavenName = mavenName;
    }

    public String getTargets() {
        return targets;
    }

    /**
     * Gets the Maven to invoke,
     * or null to invoke the default one.
     */
    public MavenInstallation getMaven() {
        for( MavenInstallation i : DESCRIPTOR.getInstallations() ) {
            if(mavenName !=null && i.getName().equals(mavenName))
                return i;
        }
        return null;
    }

    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build build, BuildListener listener) {
        Project proj = build.getProject();

        String cmd;

        String execName;
        if(File.separatorChar=='\\')
            execName = "maven.bat";
        else
            execName = "maven";

        MavenInstallation ai = getMaven();
        if(ai==null)
            cmd = execName+' '+targets;
        else {
            File exec = ai.getExecutable();
            if(!exec.exists()) {
                listener.fatalError(exec+" doesn't exist");
                return false;
            }
            cmd = exec.getPath()+' '+targets;
        }

        Map<String,String> env = build.getEnvVars();
        if(ai!=null)
            env.put("MAVEN_HOME",ai.getMavenHome());

        listener.getLogger().println("$ "+cmd);

        try {
            int r = new Proc(cmd,env,listener.getLogger(),proj.getModuleRoot()).join();
            return r==0;
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            e.printStackTrace( listener.fatalError("command execution failed") );
            return false;
        }
    }

    public BuildStepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor DESCRIPTOR = new Descriptor();

    public static final class Descriptor extends BuildStepDescriptor {
        private Descriptor() {
            super(Maven.class);
        }

        public String getDisplayName() {
            return "Invoke top-level Maven targets";
        }

        public MavenInstallation[] getInstallations() {
            MavenInstallation[] r = (MavenInstallation[]) getProperties().get("installations");

            if(r==null)
                return new MavenInstallation[0];

            return r.clone();
        }

        public boolean configure(HttpServletRequest req) {
            boolean r = true;

            int i;
            String[] names = req.getParameterValues("maven_name");
            String[] homes = req.getParameterValues("maven_home");
            int len;
            if(names!=null && homes!=null)
                len = Math.min(names.length,homes.length);
            else
                len = 0;
            MavenInstallation[] insts = new MavenInstallation[len];

            for( i=0; i<len; i++ ) {
                if(names[i].length()==0)    continue;
                insts[i] = new MavenInstallation(names[i],homes[i]);
            }

            getProperties().put("installations",insts);

            save();

            return r;
        }

        public BuildStep newInstance(HttpServletRequest req) {
            return new Maven(req.getParameter("maven_targets"),req.getParameter("maven_version"));
        }
    }

    public static final class MavenInstallation {
        private final String name;
        private final String mavenHome;

        public MavenInstallation(String name, String mavenHome) {
            this.name = name;
            this.mavenHome = mavenHome;
        }

        /**
         * install directory.
         */
        public String getMavenHome() {
            return mavenHome;
        }

        /**
         * Human readable display name.
         */
        public String getName() {
            return name;
        }

        public File getExecutable() {
            File exe = getExeFile("maven");
            if(exe.exists())
                return exe;
            exe = getExeFile("mvn");
            if(exe.exists())
                return exe;
            return null;
        }

        private File getExeFile(String execName) {
            if(File.separatorChar=='\\')
                execName += ".bat";
            return new File(getMavenHome(), "bin/" + execName);
        }

        /**
         * Returns true if the executable exists.
         */
        public boolean getExists() {
            return getExecutable()!=null;
        }
    }
}
