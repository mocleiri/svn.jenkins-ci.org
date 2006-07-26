package hudson.scm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.util.WriterOutputStream;
import hudson.util.ForkOutputStream;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import hudson.model.Project;
import hudson.model.TaskListener;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.cvslib.ChangeLogTask;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Reader;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * CVS.
 *
 * @author Kohsuke Kawaguchi
 */
public class CVSSCM extends AbstractCVSFamilySCM {
    /**
     * CVSSCM connection string.
     */
    private String cvsroot;

    /**
     * Module names.
     *
     * This could be a whitespace-separate list of multiple modules.
     */
    private String module;

    private String branch;

    private String cvsRsh;

    private boolean canUseUpdate;

    /**
     * True to avoid creating a sub-directory inside the workspace.
     * (Works only when there's just one module.)
     */
    private boolean flatten;


    public CVSSCM(String cvsroot, String module,String branch,String cvsRsh,boolean canUseUpdate, boolean flatten) {
        this.cvsroot = cvsroot;
        this.module = module.trim();
        this.branch = nullify(branch);
        this.cvsRsh = nullify(cvsRsh);
        this.canUseUpdate = canUseUpdate;
        this.flatten = flatten && module.indexOf(' ')==-1;
    }

    public String getCvsRoot() {
        return cvsroot;
    }

    /**
     * If there are multiple modules, return the module directory of the first one.
     * @param workspace
     */
    public FilePath getModuleRoot(FilePath workspace) {
        if(flatten)
            return workspace;

        int idx = module.indexOf(' ');
        if(idx>=0)  return workspace.child(module.substring(0,idx));
        else        return workspace.child(module);
    }

    public ChangeLogParser createChangeLogParser() {
        return new CVSChangeLogParser();
    }

    public String getAllModules() {
        return module;
    }

    public String getBranch() {
        return branch;
    }

    public String getCvsRsh() {
        return cvsRsh;
    }

    public boolean getCanUseUpdate() {
        return canUseUpdate;
    }

    public boolean isFlatten() {
        return flatten;
    }

    public boolean pollChanges(Project project, Launcher launcher, FilePath dir, TaskListener listener) throws IOException {
        List<String> changedFiles = update(true, launcher, dir, listener);

        return changedFiles!=null && !changedFiles.isEmpty();
    }

    public boolean checkout(Build build, Launcher launcher, FilePath dir, BuildListener listener, File changelogFile) throws IOException {
        List<String> changedFiles = null; // files that were affected by update. null this is a check out

        if(canUseUpdate && isUpdatable(dir.getLocal())) {
            changedFiles = update(false,launcher,dir,listener);
            if(changedFiles==null)
                return false;   // failed
        } else {
            dir.deleteContents();

            String cmd = MessageFormat.format("cvs -Q -z9 -d {0} co {1} {2} {3}",
                cvsroot,
                branch!=null?"-r "+branch:"",
                flatten?"-d "+dir.getName():"",
                module
            );

            if(!run(launcher,cmd,listener, flatten ? dir.getParent() : dir))
                return false;
        }

        // archive the workspace to support later tagging
        // TODO: doing this partially remotely would be faster
        File archiveFile = getArchiveFile(build);
        ZipOutputStream zos = new ZipOutputStream(archiveFile);
        if(flatten) {
            archive(build.getProject().getWorkspace().getLocal(), module, zos);
        } else {
            StringTokenizer tokens = new StringTokenizer(module);
            while(tokens.hasMoreTokens()) {
                String m = tokens.nextToken();
                archive(new File(build.getProject().getWorkspace().getLocal(),m),m,zos);
            }
        }
        zos.close();

        // contribute the tag action
        build.getActions().add(new TagAction(build));

        if(changedFiles==null)
            // nothing to compare against
            return createEmptyChangeLog(changelogFile,listener, "changelog");
        else
            return calcChangeLog(build, changedFiles, changelogFile, listener);
    }

    /**
     * Returns the file name used to archive the build.
     */
    private static File getArchiveFile(Build build) {
        return new File(build.getRootDir(),"workspace.zip");
    }

    private void archive(File dir,String relPath,ZipOutputStream zos) throws IOException {
        List<String> knownFiles = new ArrayList<String>();
        // see http://www.monkey.org/openbsd/archive/misc/9607/msg00056.html for what Entries.Log is for
        parseCVSEntries(new File(dir,"CVS/Entries"),knownFiles);
        parseCVSEntries(new File(dir,"CVS/Entries.Log"),knownFiles);
        parseCVSEntries(new File(dir,"CVS/Entries.Extra"),knownFiles);
        boolean hasCVSdirs = !knownFiles.isEmpty();
        knownFiles.add("CVS");

        File[] files = dir.listFiles();
        if(files==null)
            throw new IOException("No such directory exists: "+dir);

        for( File f : files ) {
            if(hasCVSdirs && !knownFiles.contains(f.getName())) {
                // not controlled in CVS. Skip.
                // but also make sure that we archive CVS/*, which doesn't have CVS/CVS
                continue;
            }
            String name = relPath+'/'+f.getName();
            if(f.isDirectory()) {
                archive(f,name,zos);
            } else {
                zos.putNextEntry(new ZipEntry(name));
                FileInputStream fis = new FileInputStream(f);
                Util.copyStream(fis,zos);
                fis.close();
                zos.closeEntry();
            }
        }
    }

    /**
     * Parses the CVS/Entries file and adds file/directory names to the list.
     */
    private void parseCVSEntries(File entries, List<String> knownFiles) throws IOException {
        if(!entries.exists())
            return;

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(entries)));
        String line;
        while((line=in.readLine())!=null) {
            String[] tokens = line.split("/+");
            if(tokens==null || tokens.length<2)    continue;   // invalid format
            knownFiles.add(tokens[1]);
        }
        in.close();
    }

    /**
     * Updates the workspace as well as locate changes.
     *
     * @return
     *      List of affected file names, relative to the workspace directory.
     *      Null if the operation failed.
     */
    public List<String> update(boolean dryRun, Launcher launcher, FilePath workspace, TaskListener listener) throws IOException {

        List<String> changedFileNames = new ArrayList<String>();    // file names relative to the workspace

        String cmd = "cvs -q "+(dryRun?"-n":"")+" -z9 update -PdC";
        if(flatten) {
            StringWriter output = new StringWriter();
            WriterOutputStream wo = new WriterOutputStream(output);

            if(!run(launcher,cmd,listener,workspace,
                new ForkOutputStream(wo,listener.getLogger())))
                return null;

            wo.close();
            parseUpdateOutput("",output, changedFileNames);
        } else {
            StringTokenizer tokens = new StringTokenizer(module);
            while(tokens.hasMoreTokens()) {
                String moduleName = tokens.nextToken();

                // capture the output during update
                StringWriter output = new StringWriter();
                WriterOutputStream wo = new WriterOutputStream(output);

                if(!run(launcher,cmd,listener,
                    new FilePath(workspace, moduleName),
                    new ForkOutputStream(wo,listener.getLogger())))
                    return null;

                // we'll run one "cvs log" command with workspace as the base,
                // so use path names that are relative to moduleName.
                wo.close();
                parseUpdateOutput(moduleName+'/',output, changedFileNames);
            }
        }

        return changedFileNames;
    }

    // see http://www.network-theory.co.uk/docs/cvsmanual/cvs_153.html for the output format.
    // we don't care '?' because that's not in the repository
    private static final Pattern UPDATE_LINE = Pattern.compile("[UPARMC] (.+)");

    private static final Pattern REMOVAL_LINE = Pattern.compile("cvs (server|update): (.+) is no longer in the repository");

    /**
     * Parses the output from CVS update and list up files that might have been changed.
     *
     * @param result
     *      list of file names whose changelog should be checked. This may include files
     *      that are no longer present. The path names are relative to the workspace,
     *      hence "String", not {@link File}.
     */
    private void parseUpdateOutput(String baseName, StringWriter output, List<String> result) throws IOException {
        BufferedReader in = new BufferedReader(new StringReader(output.toString()));
        String line;
        while((line=in.readLine())!=null) {
            Matcher matcher = UPDATE_LINE.matcher(line);
            if(matcher.matches()) {
                result.add(baseName+matcher.group(1));
            } else {
                matcher= REMOVAL_LINE.matcher(line);
                if(matcher.matches())
                    result.add(baseName+matcher.group(2));
            }
        }
    }

    /**
     * Returns true if we can use "cvs update" instead of "cvs checkout"
     */
    private boolean isUpdatable(File dir) {
        if(flatten) {
            return isUpdatableModule(dir);
        } else {
            StringTokenizer tokens = new StringTokenizer(module);
            while(tokens.hasMoreTokens()) {
                File module = new File(dir,tokens.nextToken());
                if(!isUpdatableModule(module))
                    return false;
            }
            return true;
        }
    }

    private boolean isUpdatableModule(File module) {
        File cvs = new File(module,"CVS");
        if(!cvs.exists())
            return false;

        // check cvsroot
        if(!checkContents(new File(cvs,"Root"),cvsroot))
            return false;
        if(branch!=null) {
            if(!checkContents(new File(cvs,"Tag"),'T'+branch))
                return false;
        } else {
            if(new File(cvs,"Tag").exists())
                return false;
        }

        return true;
    }

    /**
     * Returns true if the contents of the file is equal to the given string.
     *
     * @return false in all the other cases.
     */
    private boolean checkContents(File file, String contents) {
        try {
            Reader r = new FileReader(file);
            try {
                String s = new BufferedReader(r).readLine();
                if (s == null) return false;
                return s.trim().equals(contents.trim());
            } finally {
                r.close();
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Computes the changelog into an XML file.
     *
     * @param changedFiles
     *      Files whose changelog should be checked for updates.
     *      Never null.
     */
    private boolean calcChangeLog(Build build, List<String> changedFiles, File changelogFile, BuildListener listener) {
        if(build.getPreviousBuild()==null || changedFiles.isEmpty()) {
            // nothing to compare against, or no changes
            listener.getLogger().println("$ no changes detected");
            return createEmptyChangeLog(changelogFile,listener, "changelog");
        }

        listener.getLogger().println("$ computing changelog");

        ChangeLogTask task = new ChangeLogTask() {
            {
                setOutputStream(System.out);
                setErrorStream(System.err);
            }
        };
        task.setProject(new org.apache.tools.ant.Project());
        task.setDir(build.getProject().getWorkspace().getLocal());
        if(DESCRIPTOR.getCvspassFile().length()!=0)
            task.setPassfile(new File(DESCRIPTOR.getCvspassFile()));
        task.setCvsRoot(cvsroot);
        task.setCvsRsh(cvsRsh);
        task.setFailOnError(true);
        task.setDestfile(changelogFile);
        task.setStart(build.getPreviousBuild().getTimestamp().getTime());
        task.setEnd(build.getTimestamp().getTime());
        task.setFile(changedFiles);

        try {
            task.execute();
            return true;
        } catch( BuildException e ) {
            e.printStackTrace(listener.error(e.getMessage()));
            return false;
        } catch( RuntimeException e ) {
            // an user reported a NPE inside the changeLog task.
            // we don't want a bug in Ant to prevent a build.
            e.printStackTrace(listener.error(e.getMessage()));
            return true;    // so record the message but continue
        }
    }

    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    public void buildEnvVars(Map env) {
        if(cvsRsh!=null)
            env.put("CVS_RSH",cvsRsh);
        String cvspass = DESCRIPTOR.getCvspassFile();
        if(cvspass.length()!=0)
            env.put("CVS_PASSFILE",cvspass);
    }

    static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<SCM> {
        DescriptorImpl() {
            super(CVSSCM.class);
        }

        public String getDisplayName() {
            return "CVS";
        }

        public SCM newInstance(HttpServletRequest req) {
            return new CVSSCM(
                req.getParameter("cvs_root"),
                req.getParameter("cvs_module"),
                req.getParameter("cvs_branch"),
                req.getParameter("cvs_rsh"),
                req.getParameter("cvs_use_update")!=null,
                req.getParameter("cvs_legacy")==null
            );
        }

        public String getCvspassFile() {
            String value = (String)getProperties().get("cvspass");
            if(value==null)
                value = "";
            return value;
        }

        public void setCvspassFile(String value) {
            getProperties().put("cvspass",value);
            save();
        }

        /**
         * Gets the URL that shows the diff.
         */
        public String getDiffURL(String cvsRoot, String pathName, String oldRev, String newRev) {
            String url = getProperties().get("repository-browser.diff." + cvsRoot).toString();
            if(url==null)   return null;
            return url.replaceAll("%%P",pathName).replace("%%r",oldRev).replace("%%R",newRev);

        }

        public boolean configure( HttpServletRequest req ) {
            setCvspassFile(req.getParameter("cvs_cvspass"));

            Map<String,Object> properties = getProperties();

            int i=0;
            while(true) {
                String root = req.getParameter("cvs_repobrowser_cvsroot" + i);
                if(root==null)  break;

                setBrowser(req.getParameter("cvs_repobrowser"+i), properties, root, "repository-browser.");
                setBrowser(req.getParameter("cvs_repobrowser_diff"+i), properties, root, "repository-browser.diff.");
                i++;
            }

            save();

            return true;
        }

        private void setBrowser(String key, Map<String, Object> properties, String root, String prefi) {
            String value = Util.nullify(key);
            if(value==null) {
                properties.remove(prefi +root);
            } else {
                properties.put(prefi +root,value);
            }
        }

        public Map<String,Object> getProperties() {
            return super.getProperties();
        }
    }

    /**
     * Action for a build that performs the tagging.
     */
    public final class TagAction implements Action {
        private final Build build;

        /**
         * If non-null, that means the build is already tagged.
         */
        private String tagName;

        /**
         * If non-null, that means the tagging is in progress
         * (asynchronously.)
         */
        private transient TagWorkerThread workerThread;

        public TagAction(Build build) {
            this.build = build;
        }

        public String getIconFileName() {
            return "save.gif";
        }

        public String getDisplayName() {
            return "Tag this build";
        }

        public String getUrlName() {
            return "tagBuild";
        }

        public String getTagName() {
            return tagName;
        }

        public TagWorkerThread getWorkerThread() {
            return workerThread;
        }

        public Build getBuild() {
            return build;
        }

        public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            req.setAttribute("build",build);
            req.getView(this,chooseAction()).forward(req,rsp);
        }

        private synchronized String chooseAction() {
            if(tagName!=null)
                return "alreadyTagged.jsp";
            if(workerThread!=null)
                return "inProgress.jsp";
            return "tagForm.jsp";
        }

        /**
         * Invoked to actually tag the workspace.
         */
        public synchronized void doSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            String name = req.getParameter("name");
            if(name==null || name.length()==0) {
                // invalid tag name
                doIndex(req,rsp);
                return;
            }

            if(workerThread==null) {
                workerThread = new TagWorkerThread(name);
                workerThread.start();
            }

            doIndex(req,rsp);
        }

        /**
         * Clears the error status.
         */
        public synchronized void doClearError(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            if(workerThread!=null && !workerThread.isAlive())
                workerThread = null;
            doIndex(req,rsp);
        }

        public final class TagWorkerThread extends Thread {
            private final String tagName;
            // StringWriter is synchronized
            private final StringWriter log = new StringWriter();

            public TagWorkerThread(String tagName) {
                this.tagName = tagName;
            }

            public String getLog() {
                // this method can be invoked from another thread.
                return log.toString();
            }

            public String getTagName() {
                return tagName;
            }

            public void run() {
                BuildListener listener = new StreamBuildListener(log);

                Result result = Result.FAILURE;
                File destdir = null;
                listener.started();
                try {
                    destdir = Util.createTempDir();

                    // unzip the archive
                    listener.getLogger().println("expanding the workspace archive into "+destdir);
                    Expand e = new Expand();
                    e.setProject(new org.apache.tools.ant.Project());
                    e.setDest(destdir);
                    e.setSrc(getArchiveFile(build));
                    e.setTaskType("unzip");
                    e.execute();

                    // run cvs tag command
                    listener.getLogger().println("tagging the workspace");
                    StringTokenizer tokens = new StringTokenizer(CVSSCM.this.module);
                    while(tokens.hasMoreTokens()) {
                        String m = tokens.nextToken();
                        if(!CVSSCM.this.run(new Launcher(listener),"cvs tag -R "+tagName,listener,new FilePath(destdir).child(m))) {
                            listener.getLogger().println("tagging failed");
                            return;
                        }
                    }

                    // completed successfully
                    synchronized(TagAction.this) {
                        TagAction.this.tagName = this.tagName;
                        TagAction.this.workerThread = null;
                    }
                    build.save();

                } catch (Throwable e) {
                    e.printStackTrace(listener.fatalError(e.getMessage()));
                } finally {
                    try {
                        if(destdir!=null) {
                            listener.getLogger().println("cleaning up "+destdir);
                            Util.deleteRecursive(destdir);
                        }
                    } catch (IOException e) {
                        e.printStackTrace(listener.fatalError(e.getMessage()));
                    }
                    listener.finished(result);
                }
            }
        }
    }
}
