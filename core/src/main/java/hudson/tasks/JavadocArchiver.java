package hudson.tasks;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.maven.MavenModuleSet;
import hudson.maven.AbstractMavenProject;
import hudson.model.*;
import hudson.util.FormFieldValidator;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

/**
 * Saves Javadoc for the project and publish them. 
 *
 * @author Kohsuke Kawaguchi
 */
public class JavadocArchiver extends Publisher {
    /**
     * Path to the Javadoc directory in the workspace.
     */
    private final String javadocDir;
    private boolean keepAll;
    
    @DataBoundConstructor
    public JavadocArchiver(String javadoc_dir, boolean keep_all) {
        this.javadocDir = javadoc_dir;
        this.keepAll = keep_all;
    }

    public String getJavadocDir() {
        return javadocDir;
    }
    
    public boolean isKeepAll() {
		return keepAll;
	}

    /**
     * Gets the directory where the Javadoc is stored for the given project.
     */
    private static File getJavadocDir(AbstractItem project) {
        return new File(project.getRootDir(),"javadoc");
    }

    /**
     * Gets the directory where the Javadoc is stored for the given build.
     */
    private static File getJavadocDir(Run run) {
        return new File(run.getRootDir(),"javadoc");
    }

    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        listener.getLogger().println(Messages.JavadocArchiver_Publishing());

        FilePath javadoc = build.getParent().getWorkspace().child(javadocDir);
        FilePath target = new FilePath(getJavadocDir(build));

        try {
            if (javadoc.copyRecursiveTo("**/*",target)==0) {
                if(build.getResult().isBetterOrEqualTo(Result.UNSTABLE)) {
                    // If the build failed, don't complain that there was no javadoc.
                    // The build probably didn't even get to the point where it produces javadoc. 
                    listener.error(Messages.JavadocArchiver_NoMatchFound(javadoc));
                }
                build.setResult(Result.FAILURE);
                return true;
            }
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            e.printStackTrace(listener.error(
            		Messages.JavadocArchiver_UnableToCopy(javadoc,target)));
            return true;
        }
        
        // add build action
        build.addAction(new JavadocBuildAction(build));
        
        // if project level javadoc exists, delete it (left over from pre-1.233 implementation
        File projectJavadocDir = getJavadocDir(build.getProject());
        if (projectJavadocDir.exists()) {
        	try {
        		Util.deleteRecursive(projectJavadocDir);
        	} catch (IOException ioe) {
        		ioe.printStackTrace(listener.error(ioe.getMessage()));
        	}
        }

        // If we should only keep latest, delete previous javadoc
        if(!keepAll) {
        	AbstractProject<?,?> p = build.getProject();
            AbstractBuild<?,?> b = p.getLastSuccessfulBuild();
            while(b != null) {

                // remove old javadoc
                File jd = getJavadocDir(b);
                if(jd.exists()) {
                    listener.getLogger().println(Messages.JavadocArchiver_DeletingOld(b.getDisplayName()));
                    try {
                        Util.deleteRecursive(jd);
                    } catch (IOException e) {
                        e.printStackTrace(listener.error(e.getMessage()));
                    }
                }
                
                b = b.getPreviousNotFailedBuild();
            }
        }

        return true;
    }

    public Action getProjectAction(Project project) {
        return new JavadocAction(project);
    }

    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor<Publisher> DESCRIPTOR = new DescriptorImpl();

    public static class JavadocAction implements ProminentProjectAction {
        private final AbstractItem project;

        public JavadocAction(AbstractItem project) {
            this.project = project;
        }

        public String getUrlName() {
            return "javadoc";
        }

        public String getDisplayName() {
        	if (new File(searchForJavadocDir(), "help-doc.html").exists())
                return Messages.JavadocArchiver_DisplayName_Javadoc();
            else
                return Messages.JavadocArchiver_DisplayName_Generic();
        }

        public String getIconFileName() {
            if(searchForJavadocDir().exists())
                return "help.gif";
            
            // hide it since we don't have javadoc yet.
            return null;
        }
        
        private File searchForJavadocDir() {
    		// Would like to change AbstractItem to AbstractProject, but is 
    		// that a backwards compatible change?
    		if (project instanceof AbstractProject) {
    			AbstractProject abstractProject = (AbstractProject) project;
    			
    			Run run = abstractProject.getLastBuild();
    			while (run != null) {
    				File javadocDir = getJavadocDir(run);
    				
    				if (javadocDir.exists()) {
    					return javadocDir;
    				}
    				
    				run = run.getPreviousBuild();
    			}
    		}
    		
        	return getJavadocDir(project);
        }

        public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
            new DirectoryBrowserSupport(this,project.getDisplayName()+" javadoc")
                .serveFile(req, rsp, new FilePath(searchForJavadocDir()), "help.gif", false);
        }
    }
    
    public static class JavadocBuildAction implements Action {
    	private final AbstractBuild<?,?> build;
    	
    	public JavadocBuildAction(AbstractBuild<?,?> build) {
    		this.build = build;
    	}
    	
    	public String getUrlName() {
    		return "javadoc";
    	}

        public String getDisplayName() {
            if(new File(getJavadocDir(build),"help-doc.html").exists())
                return Messages.JavadocArchiver_DisplayName_Javadoc();
            else
                return Messages.JavadocArchiver_DisplayName_Generic();
        }

        public String getIconFileName() {
            if(getJavadocDir(build).exists())
                return "help.gif";
            else
                // hide it since we don't have javadoc yet.
                return null;
        }

        public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
            new DirectoryBrowserSupport(this, build.getDisplayName()+" javadoc")
                .serveFile(req, rsp, new FilePath(getJavadocDir(build)), "help.gif", false);
        }
    	
    }

    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private DescriptorImpl() {
            super(JavadocArchiver.class);
        }

        public String getDisplayName() {
            return Messages.JavadocArchiver_DisplayName();
        }

        /**
         * Performs on-the-fly validation on the file mask wildcard.
         */
        public void doCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator.WorkspaceDirectory(req,rsp).process();
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            // for Maven, javadoc archiving kicks in automatically
            return !AbstractMavenProject.class.isAssignableFrom(jobType);
        }
    }
}
