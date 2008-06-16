package hudson.tasks;

import hudson.CopyOnWrite;
import hudson.Launcher;
import hudson.StructuredForm;
import hudson.Util;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormFieldValidator;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Ant launcher.
 *
 * @author Kohsuke Kawaguchi
 */
public class Ant extends Builder {
    /**
     * The targets, properties, and other Ant options.
     * Either separated by whitespace or newline.
     */
    private final String targets;

    /**
     * Identifies {@link AntInstallation} to be used.
     */
    private final String antName;

    /**
     * ANT_OPTS if not null.
     */
    private final String antOpts;

    /**
     * Optional build script path relative to the workspace.
     * Used for the Ant '-f' option.
     */
    private final String buildFile;

    /**
     * Optional properties to be passed to Ant. Follows {@link Properties} syntax.
     */
    private final String properties;
    
    @DataBoundConstructor
    public Ant(String targets,String antName, String antOpts, String buildFile, String properties) {
        this.targets = targets;
        this.antName = antName;
        this.antOpts = Util.fixEmptyAndTrim(antOpts);
        this.buildFile = Util.fixEmptyAndTrim(buildFile);
        this.properties = Util.fixEmptyAndTrim(properties);
    }

	public String getBuildFile() {
		return buildFile;
	}

	public String getProperties() {
		return properties;
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

    /**
     * Gets the ANT_OPTS parameter, or null.
     */
    public String getAntOpts() {
        return antOpts;
    }

    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        AbstractProject proj = build.getProject();

        ArgumentListBuilder args = new ArgumentListBuilder();

        String execName;
        if(launcher.isUnix())
            execName = "ant";
        else
            execName = "ant.bat";

        String normalizedTarget = targets.replaceAll("[\t\r\n]+"," ");

        AntInstallation ai = getAnt();
        if(ai==null)
            args.add(execName);
        else
            args.add(ai.getExecutable(launcher));


        FilePath buildFilePath = buildFilePath(proj.getModuleRoot());

        if(!buildFilePath.exists()) {
            // because of the poor choice of getModuleRoot() with CVS/Subversion, people often get confused
            // with where the build file path is relative to. Now it's too late to change this behavior
            // due to compatibility issue, but at least we can make this less painful by looking for errors
            // and diagnosing it nicely. See HUDSON-1782

            // first check if this appears to be a valid relative path from workspace root
            FilePath buildFilePath2 = buildFilePath(proj.getWorkspace());
            if(buildFilePath2.exists()) {
                // This must be what the user meant. Let it continue.
                buildFilePath = buildFilePath2;
            } else {
                // neither file exists. So this now really does look like an error.
                listener.fatalError("Unable to find build script at "+buildFilePath);
                return false;
            }
        }

        if(buildFile!=null) {
            args.add("-file", buildFilePath.getName());
        }

        args.addKeyValuePairs("-D",build.getBuildVariables());

        if (properties != null) {
            Properties p = new Properties();
            try {
                p.load(new StringReader(properties));
            } catch (NoSuchMethodError e) {
                // load(Reader) method is only available on JDK6.
                // this fall back version doesn't work correctly with non-ASCII characters,
                // but there's no other easy ways out it seems.
                p.load(new ByteArrayInputStream(properties.getBytes()));
            }

            for (Entry<Object,Object> entry : p.entrySet()) {
                args.add("-D" + entry.getKey() + "=" + entry.getValue());
            }
        }

        args.addTokenized(normalizedTarget);

        Map<String,String> env = build.getEnvVars();
        if(ai!=null)
            env.put("ANT_HOME",ai.getAntHome());
        if(antOpts!=null)
            env.put("ANT_OPTS",antOpts);

        if(!launcher.isUnix()) {
            // on Windows, executing batch file can't return the correct error code,
            // so we need to wrap it into cmd.exe.
            // double %% is needed because we want ERRORLEVEL to be expanded after
            // batch file executed, not before. This alone shows how broken Windows is...
            args.add("&&","exit","%%ERRORLEVEL%%");

            // on Windows, proper double quote handling requires extra surrounding quote.
            // so we need to convert the entire argument list once into a string,
            // then build the new list so that by the time JVM invokes CreateProcess win32 API,
            // it puts additional double-quote. See issue #1007
            // the 'addQuoted' is necessary because Process implementation for Windows (at least in Sun JVM)
            // is too clever to avoid putting a quote around it if the argument begins with "
            // see "cmd /?" for more about how cmd.exe handles quotation.
            args = new ArgumentListBuilder().add("cmd.exe","/C").addQuoted(args.toStringWithQuote());
        }

        long startTime = System.currentTimeMillis();
        try {
            int r = launcher.launch(args.toCommandArray(),env,listener.getLogger(),buildFilePath.getParent()).join();
            return r==0;
        } catch (IOException e) {
            Util.displayIOException(e,listener);

            String errorMessage = Messages.Ant_ExecFailed();
            if(ai==null && (System.currentTimeMillis()-startTime)<1000) {
                if(DESCRIPTOR.getInstallations()==null)
                    // looks like the user didn't configure any Ant installation
                    errorMessage += Messages.Ant_GlobalConfigNeeded();
                else
                    // There are Ant installations configured but the project didn't pick it
                    errorMessage += Messages.Ant_ProjectConfigNeeded();
            }
            e.printStackTrace( listener.fatalError(errorMessage) );
            return false;
        }
    }

    private FilePath buildFilePath(FilePath base) {
        if(buildFile!=null)     return base.child(buildFile);
        // some users specify the -f option in the targets field, so take that into account as well.
        // see 
        String[] tokens = Util.tokenize(targets);
        for (int i = 0; i<tokens.length-1; i++) {
            String a = tokens[i];
            if(a.equals("-f") || a.equals("-file") || a.equals("-buildfile"))
                return base.child(tokens[i+1]);
        }
        return base.child("build.xml");
    }

    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<Builder> {
        @CopyOnWrite
        private volatile AntInstallation[] installations = new AntInstallation[0];

        private DescriptorImpl() {
            super(Ant.class);
            load();
        }

        protected void convert(Map<String,Object> oldPropertyBag) {
            if(oldPropertyBag.containsKey("installations"))
                installations = (AntInstallation[]) oldPropertyBag.get("installations");
        }

        public String getHelpFile() {
            return "/help/project-config/ant.html";
        }

        public String getDisplayName() {
            return Messages.Ant_DisplayName();
        }

        public AntInstallation[] getInstallations() {
            return installations;
        }

        public boolean configure(StaplerRequest req) {
            installations = req.bindJSONToList(
                    AntInstallation.class,StructuredForm.get(req).get("ant")).toArray(new AntInstallation[0]);
            save();
            return true;
        }

        public Ant newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(Ant.class,formData);
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
                        error(Messages.Ant_NotADirectory(f));
                        return;
                    }

                    File antJar = new File(f,"lib/ant.jar");
                    if(!antJar.exists()) {
                        error(Messages.Ant_NotAntDirectory(f));
                        return;
                    }

                    ok();
                }
            }.process();
        }
    }

    public static final class AntInstallation implements Serializable {
        private final String name;
        private final String antHome;

        @DataBoundConstructor
        public AntInstallation(String name, String home) {
            this.name = name;
            if(home.endsWith("/") || home.endsWith("\\"))
                // see https://issues.apache.org/bugzilla/show_bug.cgi?id=26947
                // Ant doesn't like the trailing slash, especially on Windows
                home = home.substring(0,home.length()-1);
            this.antHome = home;
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

        /**
         * Gets the executable path of this Ant on the given target system.
         */
        public String getExecutable(Launcher launcher) throws IOException, InterruptedException {
            final boolean isUnix = launcher.isUnix();
            return launcher.getChannel().call(new Callable<String,IOException>() {
                public String call() throws IOException {
                    File exe = getExeFile(isUnix);
                    if(exe.exists())
                        return exe.getPath();
                    return null;
                }
            });
        }

        private File getExeFile(boolean isUnix) {
            String execName;
            if(isUnix)
                execName = "ant";
            else
                execName = "ant.bat";

            return new File(getAntHome(),"bin/"+execName);
        }

        /**
         * Returns true if the executable exists.
         */
        public boolean getExists() throws IOException, InterruptedException {
            return getExecutable(new Launcher.LocalLauncher(TaskListener.NULL))!=null;
        }

        private static final long serialVersionUID = 1L;
     }
}
