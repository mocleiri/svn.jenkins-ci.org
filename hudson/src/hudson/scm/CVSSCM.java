package hudson.scm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.cvslib.ChangeLogTask;
import org.apache.tools.ant.types.FileSet;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
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

    public boolean checkout(Build build, Launcher launcher, FilePath dir, BuildListener listener) throws IOException {
        boolean result;

        if(canUseUpdate && isUpdatable(dir.getLocal()))
            result = update(launcher,dir,listener);
        else {
            dir.deleteContents();

            String cmd = MessageFormat.format("cvs -Q -z9 -d {0} co {1} {2} {3}",
                cvsroot,
                branch!=null?"-r "+branch:"",
                flatten?"-d "+dir.getName():"",
                module
            );

            result = run(launcher,cmd,listener,
                flatten ? dir.getParent() : dir);
        }

        if(!result)
           return false;


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

        return true;
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

    private static final Pattern CVS_ENTRIES = Pattern.compile("(?:A )?D?/([^/]+)/.*");

    /**
     * Parses the CVS/Entries file and adds file/directory names to the list.
     */
    private static void parseCVSEntries(File entries, List<String> knownFiles) throws IOException {
        if(!entries.exists())
            return;

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(entries)));
        try {
            String line;
            while((line=in.readLine())!=null) {
                Matcher m = CVS_ENTRIES.matcher(line);
                if (m.matches()) {
                    knownFiles.add(m.group(1));
                }
            }
        } finally {
            in.close();
        }
    }

    public boolean update(Launcher launcher, FilePath workspace, BuildListener listener) throws IOException {
        String cmd = "cvs -q -z9 update -PdC";
        if(flatten) {
            return run(launcher,cmd,listener,workspace);
        } else {
            StringTokenizer tokens = new StringTokenizer(module);
            while(tokens.hasMoreTokens()) {
                if(!run(launcher,cmd,listener,new FilePath(workspace,tokens.nextToken())))
                    return false;
            }
            return true;
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

    public boolean calcChangeLog(Build build, File changelogFile, Launcher launcher, BuildListener listener) {
        if(build.getPreviousBuild()==null) {
            // nothing to compare against
            return createEmptyChangeLog(changelogFile,listener, "changelog");
        }

        ChangeLogTask task = new ChangeLogTask() {
            {
                setOutputStream(System.out);
                setErrorStream(System.err);
            }
        };
        task.setProject(new org.apache.tools.ant.Project());
        File dir = build.getProject().getWorkspace().getLocal();
        task.setDir(dir);
        if(DESCRIPTOR.getCvspassFile().length()!=0)
            task.setPassfile(new File(DESCRIPTOR.getCvspassFile()));
        task.setCvsRoot(cvsroot);
        task.setCvsRsh(cvsRsh);
        task.setFailOnError(true);
        task.setDestfile(changelogFile);
        Date start = build.getPreviousBuild().getTimestamp().getTime();
        task.setStart(start);
        task.setEnd(build.getTimestamp().getTime());
        List<String> args = new LinkedList<String>();
        // Try to avoid querying whole repo. Just check files which have a mod time newer than prev build.
        // Note: will not find deleted files. Listing *dirs* which are newer usually would, but this has
        // three disadvantages:
        // 1. Slower (must log untouched sister files).
        // 2. Will log dirs in which no CVS-controlled files were touched,
        //    e.g. top-level if build.xml does <mkdir dir="build"/>.
        // 3. Does not work anyway if every file in the dir is deleted (since using cvs up -P).
        // 4. ChangeLogTask.addFileset only looks for files, not dirs.
        // Parsing output from cvs up would solve #1-#3 but not #4; for that, would need a custom changelog parser.
        try {
            listener.getLogger().println("# looking for files relevant to changelog, this could take a while...");
            Set<String> newFiles = findNewerFiles(dir, start.getTime());
            if (newFiles.isEmpty()) {
                // Maybe nothing was changed, but might have been file deletions...
                // Anyway this is suspicious, fall back to slow way.
                listener.getLogger().println("# no CVS-controlled files apparently changed in " + dir + " since " + start + "; will run cvs log the slow way");
            } else {
                // Cannot just use task.addCommandArgument; filenames must be after -d DATE.
                // This wastes some time (ChangeLogTask will recalculate the list) but oh well.
                FileSet fs = new FileSet();
                fs.setDir(dir);
                for (String s : newFiles) {
                    fs.createInclude().setName(s);
                }
                task.addFileset(fs);
                args.addAll(newFiles);
            }
        } catch (IOException e) {
            e.printStackTrace(listener.error(e.getMessage()));
            // continue the slow way
        }
        if (args.isEmpty() && !flatten) {
            task.setPackage(module);
            args.add(module);
        }

        // Would prefer to log the actual command line but probably not possible to extract that via API.
        listener.getLogger().print("[" + dir.getName() + "] $ cvs log <date-or-branch-args...>");
        for (String s : args) {
            listener.getLogger().print(" " + s);
        }
        listener.getLogger().println();

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

    /**
     * Find CVS-controlled files whose last-modified time is after the given date.
     * Paths given as relative w/ native separator.
     */
    private Set<String> findNewerFiles(File dir, long date) throws IOException {
        Set<String> files = new TreeSet<String>();
        findNewerFiles0(dir, "", date, files);
        return files;
    }
    private void findNewerFiles0(File dir, String prefix, long date, Set<String> files) throws IOException {
        List<String> controlled = new LinkedList<String>();
        File entries = new File(new File(dir, "CVS"), "Entries");
        if (entries.isFile()) {
            parseCVSEntries(entries, controlled);
            parseCVSEntries(new File(new File(dir, "CVS"), "Entries.Log"), controlled);
            parseCVSEntries(new File(new File(dir, "CVS"), "Entries.Extra"), controlled);
        }
        String[] kids = dir.list();
        if (kids == null) {
            throw new IOException("Could not list dir " + dir);
        }
        for (String sub : kids) {
            File f = new File(dir, sub);
            // Do not prune dirs unmentioned in CVS; can break logging for some kinds of checkouts.
            if (f.isDirectory() && (controlled.contains(sub))) {
                findNewerFiles0(f, prefix + sub + File.separatorChar, date, files);
            } else if (f.isFile() && controlled.contains(sub) && f.lastModified() > date) {
                files.add(prefix + sub);
            }
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
