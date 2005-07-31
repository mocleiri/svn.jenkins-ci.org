package hudson.scm;

import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.cvslib.ChangeLogTask;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
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


    public CVSSCM(String cvsroot, String module,String branch,String cvsRsh,boolean canUseUpdate) {
        if(branch==null || branch.trim().length()==0)
            branch = null;
        if(cvsRsh==null || cvsRsh.trim().length()==0)
            cvsRsh = null;
        this.cvsroot = cvsroot;
        this.module = module;
        this.branch = branch;
        this.cvsRsh = cvsRsh;
        this.canUseUpdate = canUseUpdate;
    }

    public String getCvsRoot() {
        return cvsroot;
    }

    /**
     * If there are multiple modules, return the module directory of the first one.
     */
    public String getModule() {
        int idx = module.indexOf(' ');
        if(idx>=0)  return module.substring(0,idx);
        else        return module;
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

    public boolean checkout(File dir, BuildListener listener) throws IOException {
        if(canUseUpdate && isUpdatable(dir))
            return update(dir,listener);

        Util.deleteContentsRecursive(dir);

        String cmd = MessageFormat.format("cvs -Q -z9 -d {0} co {2} {1}",
            cvsroot,
            module,
            branch!=null?"-r "+branch:""
        );

        return run(cmd,listener,dir);
    }

    public boolean update(File dir, BuildListener listener) throws IOException {
        String cmd = "cvs -q -z9 update -PdC";
        StringTokenizer tokens = new StringTokenizer(module);
        while(tokens.hasMoreTokens()) {
            if(!run(cmd,listener,new File(dir,tokens.nextToken())))
                return false;
        }
        return true;
    }

    /**
     * Returns true if we can use "cvs update" instead of "cvs checkout"
     */
    private boolean isUpdatable(File dir) {
        StringTokenizer tokens = new StringTokenizer(module);
        while(tokens.hasMoreTokens()) {
            File module = new File(dir,tokens.nextToken());
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

    public boolean calcChangeLog( Build build, File changelogFile, BuildListener listener ) {
        if(build.getPreviousBuild()==null) {
            // nothing to compare against
            try {
                FileWriter w = new FileWriter(changelogFile);
                w.write("<changelog/>");
                w.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace(listener.error(e.getMessage()));
                return false;
            }
        }

        listener.getLogger().println("$ computing changelog");

        ChangeLogTask task = new ChangeLogTask() {
            {
                setOutputStream(System.out);
                setErrorStream(System.err);
            }
        };
        task.setProject(new Project());
        task.setDir(build.getProject().getWorkspace());
        if(DESCRIPTOR.getCvspassFile().length()!=0)
            task.setPassfile(new File(DESCRIPTOR.getCvspassFile()));
        task.setCvsRoot(cvsroot);
        task.setCvsRsh(cvsRsh);
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

    public SCMDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public void buildEnvVars(Map env) {
        if(cvsRsh!=null)
            env.put("CVS_RSH",cvsRsh);
        String cvspass = DESCRIPTOR.getCvspassFile();
        if(cvspass.length()!=0)
            env.put("CVS_PASSFILE",cvspass);
    }

    static final Descriptor DESCRIPTOR = new Descriptor();

    public static final class Descriptor extends SCMDescriptor {
        Descriptor() {
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
                req.getParameter("cvs_use_update")!=null
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
    };
}
