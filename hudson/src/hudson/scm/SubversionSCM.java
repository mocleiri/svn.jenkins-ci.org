package hudson.scm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.digester.Digester;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

/**
 * Subversion.
 *
 * Check http://svn.collab.net/repos/svn/trunk/subversion/svn/schema/ for
 * various output formats.
 *
 * @author Kohsuke Kawaguchi
 */
public class SubversionSCM extends AbstractCVSFamilySCM {
    private final String modules;
    private boolean useUpdate;
    private String username;
    private String otherOptions;

    SubversionSCM( String modules, boolean useUpdate, String username, String otherOptions ) {
        StringBuilder normalizedModules = new StringBuilder();
        StringTokenizer tokens = new StringTokenizer(modules);
        while(tokens.hasMoreTokens()) {
            if(normalizedModules.length()>0)    normalizedModules.append(' ');
            String m = tokens.nextToken();
            if(m.endsWith("/"))
                // the normalized name is always without the trailing '/'
                m = m.substring(0,m.length()-1);
            normalizedModules.append(m);
       }

        this.modules = normalizedModules.toString();
        this.useUpdate = useUpdate;
        this.username = nullify(username);
        this.otherOptions = nullify(otherOptions);
    }

    public String getModules() {
        return modules;
    }

    public boolean isUseUpdate() {
        return useUpdate;
    }

    public String getUsername() {
        return username;
    }

    public String getOtherOptions() {
        return otherOptions;
    }

    private Collection<String> getModuleDirNames() {
        List<String> dirs = new ArrayList<String>();
        StringTokenizer tokens = new StringTokenizer(modules);
        while(tokens.hasMoreTokens()) {
            dirs.add(getLastPathComponent(tokens.nextToken()));
        }
        return dirs;
    }

    private boolean calcChangeLog(Build build, File changelogFile, Launcher launcher, BuildListener listener) throws IOException {
        if(build.getPreviousBuild()==null) {
            // nothing to compare against
            return createEmptyChangeLog(changelogFile, listener, "log");
        }

        PrintStream logger = listener.getLogger();

        Map<String,Integer> previousRevisions = parseRevisionFile(build.getPreviousBuild());
        Map<String,Integer> thisRevisions     = parseRevisionFile(build);

        Map env = createEnvVarMap(true);

        for( String module : getModuleDirNames() ) {
            Integer prevRev = previousRevisions.get(module);
            if(prevRev==null) {
                logger.println("no revision recorded for "+module+" in the previous build");
                continue;
            }
            Integer thisRev = thisRevisions.get(module);
            if(thisRev!=null && thisRev.equals(prevRev)) {
                logger.println("no change for "+module+" since the previous build");
                continue;
            }

            String cmd = DESCRIPTOR.getSvnExe()+" log -v --xml --non-interactive -r "+(prevRev+1)+":BASE "+module;
            OutputStream os = new BufferedOutputStream(new FileOutputStream(changelogFile));
            try {
                int r = launcher.launch(cmd,env,os,build.getProject().getWorkspace()).join();
                if(r!=0) {
                    listener.fatalError("revision check failed");
                    // report the output
                    FileInputStream log = new FileInputStream(changelogFile);
                    try {
                        Util.copyStream(log,listener.getLogger());
                    } finally {
                        log.close();
                    }
                    return false;
                }
            } finally {
                os.close();
            }
        }

        return true;
    }

    /*package*/ static Map<String,Integer> parseRevisionFile(Build build) throws IOException {
        Map<String,Integer> revisions = new HashMap<String,Integer>(); // module -> revision
        {// read the revision file of the last build
            File file = getRevisionFile(build);
            if(!file.exists())
                // nothing to compare against
                return revisions;

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line=br.readLine())!=null) {
                int index = line.indexOf('/');
                if(index<0) {
                    continue;   // invalid line?
                }
                try {
                    revisions.put(line.substring(0,index), Integer.parseInt(line.substring(index+1)));
                } catch (NumberFormatException e) {
                    // perhaps a corrupted line. ignore
                }
            }
        }

        return revisions;
    }

    public boolean checkout(Build build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException {
        boolean result;

        if(useUpdate && isUpdatable(workspace.getLocal(),listener)) {
            result = update(launcher,workspace,listener);
            if(!result)
                return false;
        } else {
            workspace.deleteContents();
            StringTokenizer tokens = new StringTokenizer(modules);
            while(tokens.hasMoreTokens()) {
                ArgumentListBuilder cmd = new ArgumentListBuilder();
                cmd.add(DESCRIPTOR.getSvnExe(),"co","-q","--non-interactive");
                if(username!=null)
                    cmd.add("--username",username);
                if(otherOptions!=null)
                    cmd.add(Util.tokenize(otherOptions));
                cmd.add(tokens.nextToken());
                
                result = run(launcher,cmd,listener,workspace);
                if(!result)
                    return false;
            }
        }

        // write out the revision file
        PrintWriter w = new PrintWriter(new FileOutputStream(getRevisionFile(build)));
        try {
            Map<String,SvnInfo> revMap = buildRevisionMap(workspace,listener);
            for (Entry<String,SvnInfo> e : revMap.entrySet()) {
                w.println( e.getKey() +'/'+ e.getValue().revision );
            }
        } finally {
            w.close();
        }

        return calcChangeLog(build, changelogFile, launcher, listener);
    }

    /**
     * Output from "svn info" command.
     */
    public static class SvnInfo {
        /** The remote URL of this directory */
        String url;
        /** Current workspace revision. */
        int revision = -1;

        private SvnInfo() {}

        /**
         * Returns true if this object is fully populated.
         */
        public boolean isComplete() {
            return url!=null && revision!=-1;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setRevision(int revision) {
            this.revision = revision;
        }

        /**
         * Executes "svn info" command and returns the parsed output
         *
         * @param subject
         *      The target to run "svn info". Either local path or remote URL.
         */
        public static SvnInfo parse(String subject, Map env, FilePath workspace, TaskListener listener) throws IOException {
            String cmd = DESCRIPTOR.getSvnExe()+" info --xml "+subject;
            listener.getLogger().println("$ "+cmd);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            int r = new Proc(cmd,env,baos,workspace.getLocal()).join();
            if(r!=0) {
                // failed. to allow user to diagnose the problem, send output to log
                listener.getLogger().write(baos.toByteArray());
                throw new IOException("revision check failed");
            }

            SvnInfo info = new SvnInfo();

            Digester digester = new Digester();
            digester.push(info);

            digester.addBeanPropertySetter("info/entry/url");
            digester.addSetProperties("info/entry/commit","revision","revision");  // set attributes. in particular @revision

            try {
                digester.parse(new ByteArrayInputStream(baos.toByteArray()));
            } catch (SAXException e) {
                // failed. to allow user to diagnose the problem, send output to log
                listener.getLogger().write(baos.toByteArray());
                e.printStackTrace(listener.fatalError("Failed to parse Subversion output"));
                throw new IOException("Unabled to parse svn info output");
            }

            if(!info.isComplete())
                throw new IOException("No revision in the svn info output");

            return info;
        }

    }

    /**
     * Checks .svn files in the workspace and finds out revisions of the modules
     * that the workspace has.
     *
     * @return
     *      null if the parsing somehow fails. Otherwise a map from module names to revisions.
     */
    private Map<String,SvnInfo> buildRevisionMap(FilePath workspace, TaskListener listener) throws IOException {
        PrintStream logger = listener.getLogger();

        Map<String/*module name*/,SvnInfo> revisions = new HashMap<String,SvnInfo>();

        Map env = createEnvVarMap(false);

        // invoke the "svn info"
        for( String module : getModuleDirNames() ) {
            // parse the output
            SvnInfo info = SvnInfo.parse(module,env,workspace,listener);
            revisions.put(module,info);
            logger.println("Revision:"+info.revision);
        }

        return revisions;
    }

    /**
     * Gets the file that stores the revision.
     */
    private static File getRevisionFile(Build build) {
        return new File(build.getRootDir(),"revision.txt");
    }

    public boolean update(Launcher launcher, FilePath remoteDir, BuildListener listener) throws IOException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add(DESCRIPTOR.getSvnExe(), "update", "-q", "--non-interactive");

        if(username!=null)
            cmd.add(" --username ",username);
        if(otherOptions!=null)
            cmd.add(Util.tokenize(otherOptions));

        StringTokenizer tokens = new StringTokenizer(modules);
        while(tokens.hasMoreTokens()) {
            if(!run(launcher,cmd,listener,new FilePath(remoteDir,getLastPathComponent(tokens.nextToken()))))
                return false;
        }
        return true;
    }

    /**
     * Returns true if we can use "svn update" instead of "svn checkout"
     */
    private boolean isUpdatable(File dir,BuildListener listener) {
        StringTokenizer tokens = new StringTokenizer(modules);
        while(tokens.hasMoreTokens()) {
            String url = tokens.nextToken();
            File module = new File(dir,getLastPathComponent(url));
            File svn = new File(module,".svn/entries");
            if(!svn.exists()) {
                listener.getLogger().println("Checking out a fresh workspace because "+svn+" doesn't exist.");
                return false;
            }

            // check wc-entries/entry/@url
            synchronized(spf) {
                try {
                    SAXParser parser = spf.newSAXParser();
                    Checker checker = new Checker(url);
                    parser.parse(svn,checker);
                    if(!checker.found()) {
                        listener.getLogger().println("Checking out a fresh workspace because the workspace is not "+url);
                        return false;
                    }
                } catch (ParserConfigurationException e) {
                    // impossible
                    throw new Error(e);
                } catch (SAXException e) {
                    // corrupt file? don't use update to be safe
                    failedToParse(listener, svn, e);
                    return false;
                } catch (IOException e) {
                    // corrupt file? don't use update to be safe
                    failedToParse(listener, svn, e);
                    return false;
                }
            }
        }
        return true;
    }

    private void failedToParse(BuildListener listener, File svn, Exception e) {
        listener.getLogger().println("Checking out a fresh workspace because Hudson failed to parse "+svn);
        e.printStackTrace(listener.error(e.getMessage()));
    }

    public boolean pollChanges(Project project, Launcher launcher, FilePath workspace, TaskListener listener) throws IOException {
        // current workspace revision
        Map<String,SvnInfo> wsRev = buildRevisionMap(workspace,listener);

        Map env = createEnvVarMap(false);

        // check the corresponding remote revision
        for (SvnInfo localInfo : wsRev.values()) {
            SvnInfo remoteInfo = SvnInfo.parse(localInfo.url,env,workspace,listener);
            if(remoteInfo.revision > localInfo.revision)
                return true;    // change found
        }

        return false; // no change
    }

    /**
     * Looks for /wc-entries/entry/@url and see if it matches the expected URL.
     */
    private static final class Checker extends DefaultHandler {
        private final String url;

        private boolean matched = false;

        public Checker(String url) {
            this.url = url;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if(!qName.equals("entry"))
                return;

            String n = attributes.getValue("name");
            if(n==null || n.length()>0)     return;

            String url = attributes.getValue("url");
            if(url!=null && url.equals(this.url))
                matched = true;
        }

        public boolean found() {
            return matched;
        }
    }

    public ChangeLogParser createChangeLogParser() {
        return new SubversionChangeLogParser();
    }


    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    public void buildEnvVars(Map env) {
        // no environment variable
    }

    public FilePath getModuleRoot(FilePath workspace) {
        String s;

        // if multiple URLs are specified, pick the first one
        int idx = modules.indexOf(' ');
        if(idx>=0)  s = modules.substring(0,idx);
        else        s = modules;

        return workspace.child(getLastPathComponent(s));
    }

    private String getLastPathComponent(String s) {
        String[] tokens = s.split("/");
        return tokens[tokens.length-1]; // return the last token
    }

    /**
     * Shared instance. Note that it is not namespace aware.
     */
    static final SAXParserFactory spf = SAXParserFactory.newInstance();

    static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<SCM> {
        DescriptorImpl() {
            super(SubversionSCM.class);
        }

        public String getDisplayName() {
            return "Subversion";
        }

        public SCM newInstance(HttpServletRequest req) {
            return new SubversionSCM(
                req.getParameter("svn_modules"),
                req.getParameter("svn_use_update")!=null,
                req.getParameter("svn_username"),
                req.getParameter("svn_other_options")
            );
        }

        public String getSvnExe() {
            String value = (String)getProperties().get("svn_exe");
            if(value==null)
                value = "svn";
            return value;
        }

        public void setSvnExe(String value) {
            getProperties().put("svn_exe",value);
            save();
        }

        public boolean configure( HttpServletRequest req ) {
            setSvnExe(req.getParameter("svn_exe"));
            return true;
        }
    }
}
