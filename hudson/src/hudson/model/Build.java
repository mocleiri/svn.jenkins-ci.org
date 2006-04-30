package hudson.model;

import hudson.Launcher;
import hudson.Proc;
import static hudson.model.Hudson.isWindows;
import hudson.scm.CVSChangeLog;
import hudson.tasks.BuildStep;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public final class Build extends Run<Project,Build> implements Runnable {

    /**
     * Name of the slave this project was built on.
     * Null if built by the master.
     */
    private String builtOn;

    /**
     * Creates a new build.
     */
    Build(Project project) throws IOException {
        super(project);
    }

    public Project getProject() {
        return getParent();
    }

    /**
     * Loads a build from a log file.
     */
    Build(Project project, File buildDir, Build prevBuild ) throws IOException {
        super(project,buildDir,prevBuild);
    }

    /**
     * Gets the changes incorporated into this build.
     */
    public CVSChangeLog[] getChangeSet() {
        try {
            return CVSChangeLog.parse(new File(getRootDir(),"changelog.xml"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return new CVSChangeLog[0];
    }

    public Calendar due() {
        return timestamp;
    }

    /**
     * Returns a {@link Slave} on which this build was done.
     */
    public Node getBuiltOn() {
        if(builtOn==null)
            return Hudson.getInstance();
        else
            return Hudson.getInstance().getSlave(builtOn);
    }

    /**
     * Gets {@link TestResult} associated with this build if any.
     * This may take time on a large test result.
     */
    public TestResult getTestResult() {
        TestResultAction ta = getAction(TestResultAction.class);
        if(ta==null)    return null;
        return ta.getResult();
    }

    /**
     * Gets {@link TestResultAction} associated with this build if any.
     * This works a lot faster than {@link #getTestResult()} on a large test,
     * but it only has a limited amount of information available.
     */
    public TestResultAction getTestResultAction() {
        return getAction(TestResultAction.class);
    }

    /**
     * Performs a build.
     */
    public void run() {
        run(new Runner() {

            /**
             * Since configuration can be changed while a build is in progress,
             * stick to one launcher and use it.
             */
            private Launcher launcher;

            public Result run(BuildListener listener) throws IOException {
                Node node = Executor.currentExecutor().getOwner().getNode();
                assert builtOn==null;
                builtOn = node.getNodeName();

                launcher = node.createLauncher(listener);
                if(node instanceof Slave)
                    listener.getLogger().println("Building remotely on "+node.getNodeName());


                if(!project.checkout(Build.this,launcher,listener))
                    return Result.FAILURE;

                if(!project.getScm().calcChangeLog(Build.this,new File(getRootDir(),"changelog.xml"), launcher, listener))
                    return Result.FAILURE;

                if(!preBuild(listener,project.getBuilders()))
                    return Result.FAILURE;
                if(!preBuild(listener,project.getPublishers()))
                    return Result.FAILURE;

                if(!build(listener,project.getBuilders()))
                    return Result.FAILURE;

                if(!isWindows()) {
                    try {
                        // ignore a failure.
                        new Proc(new String[]{"rm","../lastSuccessful"},new String[0],listener.getLogger(),getProject().getBuildDir()).join();

                        int r = new Proc(new String[]{
                            "ln","-s","builds/"+getId()/*ugly*/,"../lastSuccessful"},
                            new String[0],listener.getLogger(),getProject().getBuildDir()).join();
                        if(r!=0)
                            listener.getLogger().println("ln failed: "+r);
                    } catch (IOException e) {
                        PrintStream log = listener.getLogger();
                        log.println("ln failed");
                        e.printStackTrace( log );
                    }
                }

                return Result.SUCCESS;
            }

            public void post(BuildListener listener) {
                // run all of them even if one of them failed
                for( BuildStep bs : project.getPublishers().values() )
                    bs.perform(Build.this, launcher, listener);
            }

            private boolean build(BuildListener listener, Map<?, BuildStep> steps) {
                for( BuildStep bs : steps.values() )
                    if(!bs.perform(Build.this, launcher, listener))
                        return false;
                return true;
            }

            private boolean preBuild(BuildListener listener,Map<?,BuildStep> steps) {
                for( BuildStep bs : steps.values() )
                    if(!bs.prebuild(Build.this,listener))
                        return false;
                return true;
            }
        });
    }

    @Override
    public Map<String,String> getEnvVars() {
        Map<String,String> env = super.getEnvVars();

        JDK jdk = project.getJDK();
        if(jdk !=null)
            jdk.buildEnvVars(env);
        project.getScm().buildEnvVars(env);
        return env;
    }
}
