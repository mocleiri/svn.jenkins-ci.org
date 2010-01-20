/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.ivy;

import hudson.FilePath;
import hudson.EnvVars;
import hudson.slaves.WorkspaceList;
import hudson.slaves.WorkspaceList.Lease;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.Environment;
import hudson.model.Node;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.Ant;
import hudson.tasks.BuildWrapper;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * {@link Run} for {@link MavenModule}.
 * 
 * @author Kohsuke Kawaguchi
 */
public class IvyBuild extends AbstractIvyBuild<IvyModule, IvyBuild> {
    /**
     * {@link IvyReporter}s that will contribute project actions. Can be null if
     * there's none.
     */
    /* package */List<IvyReporter> projectActionReporters;

    public IvyBuild(IvyModule job) throws IOException {
        super(job);
    }

    public IvyBuild(IvyModule job, Calendar timestamp) {
        super(job, timestamp);
    }

    public IvyBuild(IvyModule project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    @Override
    public String getUpUrl() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req != null) {
            List<Ancestor> ancs = req.getAncestors();
            for (int i = 1; i < ancs.size(); i++) {
                if (ancs.get(i).getObject() == this) {
                    if (ancs.get(i - 1).getObject() instanceof IvyModuleSetBuild) {
                        // if under IvyModuleSetBuild, "up" means IMSB
                        return ancs.get(i - 1).getUrl() + '/';
                    }
                }
            }
        }
        return super.getUpUrl();
    }

    @Override
    public String getDisplayName() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req != null) {
            List<Ancestor> ancs = req.getAncestors();
            for (int i = 1; i < ancs.size(); i++) {
                if (ancs.get(i).getObject() == this) {
                    if (ancs.get(i - 1).getObject() instanceof IvyModuleSetBuild) {
                        // if under MavenModuleSetBuild, display the module name
                        return getParent().getDisplayName();
                    }
                }
            }
        }
        return super.getDisplayName();
    }

    /**
     * Gets the {@link IvyModuleSetBuild} that has the same build number.
     * 
     * @return null if no such build exists, which happens when the module build
     *         is manually triggered.
     * @see #getModuleSetBuild()
     */
    public IvyModuleSetBuild getParentBuild() {
        return getParent().getParent().getBuildByNumber(getNumber());
    }

    /**
     * Gets the "governing" {@link MavenModuleSet} that has set the workspace
     * for this build.
     * 
     * @return null if no such build exists, which happens if the build is
     *         manually removed.
     * @see #getParentBuild()
     */
    public IvyModuleSetBuild getModuleSetBuild() {
        return getParent().getParent().getNearestOldBuild(getNumber());
    }

    @Override
    public ChangeLogSet<? extends Entry> getChangeSet() {
        return new FilteredChangeLogSet(this);
    }

    /**
     * We always get the changeset from {@link IvyModuleSetBuild}.
     */
    @Override
    public boolean hasChangeSetComputed() {
        return true;
    }

    public void registerAsProjectAction(IvyReporter reporter) {
        if (projectActionReporters == null)
            projectActionReporters = new ArrayList<IvyReporter>();
        projectActionReporters.add(reporter);
    }

    @Override
    public void run() {
        run(new RunnerImpl());

        getProject().updateTransientActions();

        IvyModuleSetBuild parentBuild = getModuleSetBuild();
        if (parentBuild != null)
            parentBuild.notifyModuleBuild(this);
    }

    /**
     * Backdoor for {@link IvyModuleSetBuild} to assign workspaces for modules.
     */
    @Override
    protected void setWorkspace(FilePath path) {
        super.setWorkspace(path);
    }

    /**
     * Runs Maven and builds the project.
     */
    private static final class Builder extends IvyBuilder {
        private final IvyReporter[] reporters;

        private long startTime;

        public Builder(BuildListener listener, IvyReporter[] reporters, List<String> goals, Map<String, String> systemProps) {
            super(listener, goals, systemProps);
            this.reporters = reporters;
        }

        private static final long serialVersionUID = 1L;

        public Result call() throws IOException {
            return null;
        }
    }

    private class RunnerImpl extends AbstractRunner {
        private List<IvyReporter> reporters;

        @Override
        protected Lease decideWorkspace(Node n, WorkspaceList wsl) throws InterruptedException, IOException {
            return wsl.allocate(getModuleSetBuild().getModuleRoot().child(getProject().getRelativePath()));
        }

        protected Result doRun(BuildListener listener) throws Exception {
            // pick up a list of reporters to run
            // reporters = getProject().createReporters();
            IvyModuleSet mms = getProject().getParent();
            if (debug)
                listener.getLogger().println("Reporters=" + reporters);

            for (BuildWrapper w : mms.getBuildWrappersList()) {
                Environment e = w.setUp(IvyBuild.this, launcher, listener);
                if (e == null) {
                    return Result.FAILURE;
                }
                buildEnvironments.add(e);
            }

            Ant ant = new Ant(getProject().getTargets(), mms.getAnt().getName(), mms.getAntOpts(), getModuleRoot().child(mms.getBuildFile())
                    .getName(), mms.getAntProperties());
            if (ant.perform(IvyBuild.this, launcher, listener))
                return Result.FAILURE;

            return Result.SUCCESS;
        }

        public void post2(BuildListener listener) throws Exception {
            for (IvyReporter reporter : reporters)
                reporter.end(IvyBuild.this, launcher, listener);
        }

        @Override
        public void cleanUp(BuildListener listener) throws Exception {
            scheduleDownstreamBuilds(listener);
        }
    }

    /**
     * Set true to produce debug output.
     */
    public static boolean debug = false;

    @Override
    public IvyModule getParent() {// don't know why, but javac wants this
        return super.getParent();
    }
}
