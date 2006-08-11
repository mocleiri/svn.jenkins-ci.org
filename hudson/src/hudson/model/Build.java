package hudson.model;

import hudson.Launcher;
import hudson.Proc;
import static hudson.model.Hudson.isWindows;
import hudson.scm.CVSChangeLogParser;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.tasks.BuildStep;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.triggers.SCMTrigger;
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
     * SCM used for this build.
     * Maybe null, for historical reason, in which case CVS is assumed.
     */
    private ChangeLogParser scm;

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
    public ChangeLogSet getChangeSet() {
        if(scm==null)
            scm = new CVSChangeLogParser();

        File changelogFile = new File(getRootDir(), "changelog.xml");
        if(!changelogFile.exists())
            return ChangeLogSet.EMPTY;

        try {
            return scm.parse(this,changelogFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return ChangeLogSet.EMPTY;
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


                if(!project.checkout(Build.this,launcher,listener,new File(getRootDir(),"changelog.xml")))
                    return Result.FAILURE;

                SCM scm = project.getScm();

                Build.this.scm = scm.createChangeLogParser();

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
                for( Publisher bs : project.getPublishers().values() )
                    bs.perform(Build.this, launcher, listener);
            }

            private boolean build(BuildListener listener, Map<?, Builder> steps) {
                for( Builder bs : steps.values() )
                    if(!bs.perform(Build.this, launcher, listener))
                        return false;
                return true;
            }

            private boolean preBuild(BuildListener listener,Map<?,? extends BuildStep> steps) {
                for( BuildStep bs : steps.values() )
                    if(!bs.prebuild(Build.this,listener))
                        return false;
                return true;
            }
        });
    }

    @Override
    protected void onStartBuilding() {
        SCMTrigger t = (SCMTrigger)project.getTriggers().get(SCMTrigger.DESCRIPTOR);
        if(t==null) {
            super.onStartBuilding();
        } else {
            synchronized(t) {
                try {
                    t.abort();
                } catch (InterruptedException e) {
                    // handle the interrupt later
                    Thread.currentThread().interrupt();
                }
                super.onStartBuilding();
            }
        }
    }

    @Override
    protected void onEndBuilding() {
        SCMTrigger t = (SCMTrigger)project.getTriggers().get(SCMTrigger.DESCRIPTOR);
        if(t==null) {
            super.onEndBuilding();
        } else {
            synchronized(t) {
                super.onEndBuilding();
                t.startPolling();
            }
        }
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
