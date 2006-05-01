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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
            String s = new BufferedReader(new FileReader(file)).readLine();
            if(s==null)     return false;
            return s.trim().equals(contents.trim());
        } catch (IOException e) {
            return false;
        }
    }

    public boolean calcChangeLog(Build build, File changelogFile, Launcher launcher, BuildListener listener) {
        if(build.getPreviousBuild()==null) {
            // nothing to compare against
            return createEmptyChangeLog(changelogFile,listener);
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
        if(!flatten)
            task.setPackage(module);
        task.setFailOnError(true);
        task.setDestfile(changelogFile);
        task.setStart(build.getPreviousBuild().getTimestamp().getTime());
        task.setEnd(build.getTimestamp().getTime());

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

        public boolean configure( HttpServletRequest req ) {
            setCvspassFile(req.getParameter("cvs_cvspass"));
            return true;
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
