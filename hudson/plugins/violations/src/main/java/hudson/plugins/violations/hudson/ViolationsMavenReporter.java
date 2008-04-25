package hudson.plugins.violations.hudson;

import hudson.maven.MavenBuild;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.maven.MavenModule;
import hudson.maven.MavenBuildProxy;
import hudson.maven.MojoInfo;

import org.kohsuke.stapler.StaplerRequest;
import java.io.File;
import java.io.IOException;

import hudson.maven.MavenBuildProxy.BuildCallable;

import hudson.plugins.violations.ViolationsConfig;
import hudson.plugins.violations.ViolationsProjectAction;
import hudson.plugins.violations.ViolationsBuildAction;
import hudson.plugins.violations.ViolationsReport;
import hudson.plugins.violations.ViolationsCollector;

import hudson.model.BuildListener;
import hudson.Launcher;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import hudson.model.Action;
import hudson.FilePath;

public class ViolationsMavenReporter extends MavenReporter {
    private static final String VIOLATIONS = "violations";

    public static final ViolationsMavenDescriptor DESCRIPTOR
        = new ViolationsMavenDescriptor();

    private final ViolationsConfig config =
        new  ViolationsConfig();

    
    /**
     * Get the confiation object for this violations publisher.
     * @return the config.
     */
    public ViolationsConfig getConfig() {
        return config;
    }

    /**
     * Get a copy of the configuration.
     * This is used to configure a new publisher in the config.jelly
     * script.
     * @return a copy o the condig.
     */
    public ViolationsConfig getOldConfig() {
        return config.clone();
    }


    /**
     * Called at the end of each maven ?goal?.
     * - so if the maven args are pmd:pmd checkstyle:checkstyle
     * this will get called twice.
     */
    public boolean postExecute(
        MavenBuildProxy build, MavenProject pom, MojoInfo mojo,
        BuildListener listener, Throwable error)
        throws InterruptedException, IOException {
        build.execute(new BuildCallable<Void, IOException>() {
            public Void call(final MavenBuild build)
                throws IOException, InterruptedException {
                // Create the violations build action - if not already built.
                // This needes to be done here, otherwise aggregated actions
                // do not get created.
                // (aggreatedactions get created after the postExecute(), but
                // before the end()
                getCreateBuildAction(build);
                // Need to register the  MavenReporter as a ProjectAction
                build.registerAsProjectAction(ViolationsMavenReporter.this);
                return null;
            }
        });
        return true;
    }

    public boolean end(MavenBuild build, Launcher launcher, BuildListener listener)
        throws InterruptedException, IOException {
        FilePath htmlPath   = new FilePath(
            new File(build.getProject().getRootDir(), VIOLATIONS));
        FilePath targetPath = new FilePath(
            new File(build.getRootDir(), VIOLATIONS));

        ViolationsReport report = build.getProject().getWorkspace().act(
            new ViolationsCollector(listener, targetPath, htmlPath, config));
        report.setConfig(config);
        report.setBuild(build);
        
        ViolationsBuildAction buildAction = getCreateBuildAction(build);
        buildAction.setReport(report);
        build.getActions().add(buildAction);
        return true;
    }

    @Override
    public Action getProjectAction(final MavenModule module) {
        return new ViolationsProjectAction(module);
    }

    /** {@inheritDoc} */
    @Override
    public MavenReporterDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    private ViolationsBuildAction getCreateBuildAction(MavenBuild b) {
        ViolationsBuildAction ret
            = b.getAction(ViolationsBuildAction.class);
        if (ret == null) {
            ret = new ViolationsBuildAction(b);
        }
        return ret;
    }
    
}
