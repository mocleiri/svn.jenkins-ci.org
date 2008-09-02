package hudson.maven.reporters;

import hudson.FilePath;
import hudson.Util;
import hudson.maven.MavenBuildProxy;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.maven.MojoInfo;
import hudson.model.AbstractItem;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.ProminentProjectAction;
import hudson.model.Result;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

/**
 * Watches out for the execution of maven-site-plugin and records its output.
 * @author Kohsuke Kawaguchi
 */
public class MavenSiteArchiver extends MavenReporter {
    public boolean postExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener, Throwable error) throws InterruptedException, IOException {
        if(!mojo.is("org.apache.maven.plugins","maven-site-plugin","site"))
            return true;

        File destDir;
        try {
            destDir = mojo.getConfigurationValue("outputDirectory", File.class);
        } catch (ComponentConfigurationException e) {
            e.printStackTrace(listener.fatalError("Unable to find the site output directory"));
            build.setResult(Result.FAILURE);
            return true;
        }

        if(destDir.exists()) {
            // be defensive. I suspect there's some interaction with this and multi-module builds
            FilePath target;
            // store at MavenModuleSet level.
            target = build.getModuleSetRootDir().child("site");

            try {
                listener.getLogger().println("[HUDSON] Archiving site");
                new FilePath(destDir).copyRecursiveTo("**/*",target);
            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace(listener.fatalError("Unable to copy site from {0} to {1}",destDir,target));
                build.setResult(Result.FAILURE);
            }

            build.registerAsAggregatedProjectAction(this);
        }

        return true;
    }


    public Action getProjectAction(MavenModule project) {
        return new SiteAction(project);
    }

    public Action getAggregatedProjectAction(MavenModuleSet project) {
        return new SiteAction(project);
    }

    private static File getSiteDir(AbstractItem project) {
        return new File(project.getRootDir(),"site");
    }

    public static class SiteAction implements ProminentProjectAction {
        private final AbstractItem project;

        public SiteAction(AbstractItem project) {
            this.project = project;
        }

        public String getUrlName() {
            return "site";
        }

        public String getDisplayName() {
            return "Maven-generated site";
        }

        public String getIconFileName() {
            if(getSiteDir(project).exists())
                return "help.gif";
            else
                // hide it since we don't have site yet.
                return null;
        }

        public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
            new DirectoryBrowserSupport(this,project.getDisplayName()+" site")
                .serveFile(req, rsp, new FilePath(getSiteDir(project)), "help.gif", false);
        }
    }

    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }

    public static final class DescriptorImpl extends MavenReporterDescriptor {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

        private DescriptorImpl() {
            super(MavenSiteArchiver.class);
        }

        public String getDisplayName() {
            return "Maven site";
        }

        public MavenSiteArchiver newAutoInstance(MavenModule module) {
            return new MavenSiteArchiver();
        }
    }

    private static final long serialVersionUID = 1L;
}
