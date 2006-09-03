package hudson.scm;

import static hudson.Util.fixEmpty;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.Proc;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.model.Hudson;
import hudson.model.ModelObject;
import hudson.org.apache.tools.ant.taskdefs.cvslib.ChangeLogTask;
import hudson.util.ArgumentListBuilder;
import hudson.util.ForkOutputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            ArgumentListBuilder cmd = new ArgumentListBuilder();
            cmd.add("cvs","-Q","-z9","-d",cvsroot,"co");
            if(branch!=null)
                cmd.add("-r",branch);
            if(flatten)
                cmd.add("-d",dir.getName());
            cmd.addTokenized(module);

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

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("cvs","-q","-z9");
        if(dryRun)
            cmd.add("-n");
        cmd.add("update","-PdC");

        if(flatten) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            if(!run(launcher,cmd,listener,workspace,
                new ForkOutputStream(baos,listener.getLogger())))
                return null;

            parseUpdateOutput("",baos, changedFileNames);
        } else {
            StringTokenizer tokens = new StringTokenizer(module);
            while(tokens.hasMoreTokens()) {
                String moduleName = tokens.nextToken();

                // capture the output during update
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                if(!run(launcher,cmd,listener,
                    new FilePath(workspace, moduleName),
                    new ForkOutputStream(baos,listener.getLogger())))
                    return null;

                // we'll run one "cvs log" command with workspace as the base,
                // so use path names that are relative to moduleName.
                parseUpdateOutput(moduleName+'/',baos, changedFileNames);
            }
        }

        return changedFileNames;
    }

    // see http://www.network-theory.co.uk/docs/cvsmanual/cvs_153.html for the output format.
    // we don't care '?' because that's not in the repository
    private static final Pattern UPDATE_LINE = Pattern.compile("[UPARMC] (.+)");

    private static final Pattern REMOVAL_LINE = Pattern.compile("cvs (server|update): (.+) is no longer in the repository");
    private static final Pattern NEWDIRECTORY_LINE = Pattern.compile("cvs server: New directory `(.+)' -- ignored");

    /**
     * Parses the output from CVS update and list up files that might have been changed.
     *
     * @param result
     *      list of file names whose changelog should be checked. This may include files
     *      that are no longer present. The path names are relative to the workspace,
     *      hence "String", not {@link File}.
     */
    private void parseUpdateOutput(String baseName, ByteArrayOutputStream output, List<String> result) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(
            new ByteArrayInputStream(output.toByteArray())));
        String line;
        while((line=in.readLine())!=null) {
            Matcher matcher = UPDATE_LINE.matcher(line);
            if(matcher.matches()) {
                result.add(baseName+matcher.group(1));
                continue;
            }

            matcher= REMOVAL_LINE.matcher(line);
            if(matcher.matches()) {
                result.add(baseName+matcher.group(2));
                continue;
            }

            // this line is added in an attempt to capture newly created directories in the repository,
            // but it turns out that this line always hit if the workspace is missing a directory
            // that the server has, even if that directory contains nothing in it
            //matcher= NEWDIRECTORY_LINE.matcher(line);
            //if(matcher.matches()) {
            //    result.add(baseName+matcher.group(1));
            //}
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
     * <p>
     * When we update the workspace, we'll compute the changelog by using its output to
     * make it faster. In general case, we'll fall back to the slower approach where
     * we check all files in the workspace.
     *
     * @param changedFiles
     *      Files whose changelog should be checked for updates.
     *      This is provided if the previous operation is update, otherwise null,
     *      which means we have to fall back to the default slow computation.
     */
    private boolean calcChangeLog(Build build, List<String> changedFiles, File changelogFile, final BuildListener listener) {
        if(build.getPreviousBuild()==null || (changedFiles!=null && changedFiles.isEmpty())) {
            // nothing to compare against, or no changes
            // (note that changedFiles==null means fallback, so we have to run cvs log.
            listener.getLogger().println("$ no changes detected");
            return createEmptyChangeLog(changelogFile,listener, "changelog");
        }

        listener.getLogger().println("$ computing changelog");

        final StringWriter errorOutput = new StringWriter();
        final boolean[] hadError = new boolean[1];

        ChangeLogTask task = new ChangeLogTask() {
            public void log(String msg, int msgLevel) {
                // send error to listener. This seems like the route in which the changelog task
                // sends output
                if(msgLevel==org.apache.tools.ant.Project.MSG_ERR) {
                    hadError[0] = true;
                    errorOutput.write(msg);
                    errorOutput.write('\n');
                }
            }
        };
        task.setProject(new org.apache.tools.ant.Project());
        File baseDir = build.getProject().getWorkspace().getLocal();
        task.setDir(baseDir);
        if(DESCRIPTOR.getCvspassFile().length()!=0)
            task.setPassfile(new File(DESCRIPTOR.getCvspassFile()));
        task.setCvsRoot(cvsroot);
        task.setCvsRsh(cvsRsh);
        task.setFailOnError(true);
        task.setDestfile(changelogFile);
        task.setStart(build.getPreviousBuild().getTimestamp().getTime());
        task.setEnd(build.getTimestamp().getTime());
        if(changedFiles!=null) {
            // if the directory doesn't exist, cvs changelog will die, so filter them out.
            // this means we'll lose the log of those changes
            for (String filePath : changedFiles) {
                if(new File(baseDir,filePath).getParentFile().exists())
                    task.addFile(filePath);
            }
        } else {
            // fallback
            if(!flatten)
                task.setPackage(module);
        }

        try {
            task.execute();
            if(hadError[0]) {
                // non-fatal error must have occurred, such as cvs changelog parsing error.s
                listener.getLogger().print(errorOutput);
            }
            return true;
        } catch( BuildException e ) {
            // capture output from the task for diagnosis
            listener.getLogger().print(errorOutput);
            // then report an error
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

    public static final class DescriptorImpl extends Descriptor<SCM> implements ModelObject {
        DescriptorImpl() {
            super(CVSSCM.class);
        }

        public String getDisplayName() {
            return "CVS";
        }

        public SCM newInstance(StaplerRequest req) {
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

    //
    // web methods
        //

        public void doCvsPassCheck(StaplerRequest req, StaplerResponse rsp) throws IOException {
            // this method can be used to check if a file exists anywhere in the file system,
            // so it should be protected.
            if(!Hudson.adminCheck(req,rsp))
                return;

            rsp.setStatus(HttpServletResponse.SC_OK);
            rsp.setContentType("text/html");
            PrintWriter w = rsp.getWriter();

            String v = fixEmpty(req.getParameter("value"));
            if(v==null) {
                // default.
                w.print("<div/>");
            } else {
                File cvsPassFile = new File(v);

                if(cvsPassFile.exists()) {
                    w.println("<div/>");
                } else {
                    w.println("<div class=error>No such file exists</div>");
                }
            }
        }

        /**
         * Displays "cvs --version" for trouble shooting.
         */
        public void doVersion(StaplerRequest req, StaplerResponse rsp) throws IOException {
            rsp.setContentType("text/plain");
            Proc proc = Hudson.getInstance().createLauncher(TaskListener.NULL).launch(
                new String[]{"cvs", "--version"}, new String[0], rsp.getOutputStream(), FilePath.RANDOM);
            proc.join();
        }

        /**
         * Checks the entry to the CVSROOT field.
         * <p>
         * Also checks if .cvspass file contains the entry for this.
         */
        public void doCvsrootCheck(StaplerRequest req, StaplerResponse rsp) throws IOException {
            rsp.setStatus(HttpServletResponse.SC_OK);
            rsp.setContentType("text/html");
            PrintWriter w = rsp.getWriter();

            String v = fixEmpty(req.getParameter("value"));
            if(v==null) {
                w.print("<div class=error>CVSROOT is mandatory</div>");
                return;
            }

            // CVSROOT format isn't really that well defined. So it's hard to check this rigorously.
            if(v.startsWith(":pserver") || v.startsWith(":ext")) {
                if(!CVSROOT_PSERVER_PATTERN.matcher(v).matches()) {
                    w.print("<div class=error>Invalid CVSROOT string</div>");
                    return;
                }
                // I can't really test if the machine name exists, either.
                // some cvs, such as SOCKS-enabled cvs can resolve host names that Hudson might not
                // be able to. If :ext is used, all bets are off anyway.
            }

            // check .cvspass file to see if it has entry.
            // CVS handles authentication only if it's pserver.
            if(v.startsWith(":pserver")) {
                String cvspass = getCvspassFile();
                File passfile;
                if(cvspass.equals("")) {
                    passfile = new File(new File(System.getProperty("user.home")),".cvspass");
                } else {
                    passfile = new File(cvspass);
                }

                if(passfile.exists()) {
                    // It's possible that we failed to locate the correct .cvspass file location,
                    // so don't report an error if we couldn't locate this file.
                    //
                    // if this is explicitly specified, then our system config page should have
                    // reported an error.
                    if(!scanCvsPassFile(passfile, v)) {
                        w.print("<div class=error>It doesn't look like this CVSROOT has its password set." +
                            " Would you like to set it now?</div>");
                        return;
                    }
                }
            }

            // all tests passed so far
            w.print("<div/>");
        }

        /**
         * Checks if the given pserver CVSROOT value exists in the pass file.
         */
        private boolean scanCvsPassFile(File passfile, String cvsroot) throws IOException {
            cvsroot += ' ';
            BufferedReader in = new BufferedReader(new FileReader(passfile));
            try {
                String line;
                while((line=in.readLine())!=null) {
                    if(line.startsWith(cvsroot))
                        return true;
                }
                return false;
            } finally {
                in.close();
            }
        }

        private static final Pattern CVSROOT_PSERVER_PATTERN =
            Pattern.compile(":(ext|pserver):[^@:]+@[^:]+:(\\d+:)?.+");

        /**
         * Runs cvs login command.
         *
         * TODO: this apparently doesn't work. Probably related to the fact that
         * cvs does some tty magic to disable ecoback or whatever.
         */
        public void doPostPassword(StaplerRequest req, StaplerResponse rsp) throws IOException {
            if(!Hudson.adminCheck(req,rsp))
                return;

            String cvsroot = req.getParameter("cvsroot");
            String password = req.getParameter("password");

            if(cvsroot==null || password==null) {
                rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            rsp.setContentType("text/plain");
            Proc proc = Hudson.getInstance().createLauncher(TaskListener.NULL).launch(
                new String[]{"cvs", "-d",cvsroot,"login"}, new String[0],
                new ByteArrayInputStream((password+"\n").getBytes()),
                rsp.getOutputStream());
            proc.join();
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
                return "alreadyTagged.jelly";
            if(workerThread!=null)
                return "inProgress.jelly";
            return "tagForm.jelly";
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
                        ArgumentListBuilder cmd = new ArgumentListBuilder();
                        cmd.add("cvs","tag","-R",tagName);
                        if(!CVSSCM.this.run(new Launcher(listener),cmd,listener,new FilePath(destdir).child(m))) {
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
