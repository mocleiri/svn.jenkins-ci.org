/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Red Hat, Inc., Victor Glushenkov
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

import hudson.AbortException;
import hudson.Util;
import hudson.EnvVars;
import hudson.scm.ChangeLogSet;
import hudson.FilePath.FileCallable;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Environment;
import hudson.model.Fingerprint;
import hudson.model.Hudson;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Ant.AntInstallation;
import hudson.util.StreamTaskListener;
import hudson.util.VariableResolver;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.ivy.Ivy;
import org.apache.ivy.Ivy.IvyCallback;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.sort.SortOptions;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.version.VersionMatcher;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * {@link Build} for {@link IvyModuleSet}.
 * 
 * <p>
 * A "build" of {@link IvyModuleSet} consists of:
 * 
 * <ol>
 * <li>Update the workspace.
 * <li>Parse ivy.xml files
 * <li>Trigger module builds.
 * </ol>
 * 
 * This object remembers the changelog and what {@link IvyBuild}s are done on
 * this.
 * 
 * @author Timothy Bingaman
 */
public class IvyModuleSetBuild extends AbstractIvyBuild<IvyModuleSet, IvyModuleSetBuild> {
    /**
     * {@link IvyReporter}s that will contribute project actions. Can be null if
     * there's none.
     */
    /* package */List<IvyReporter> projectActionReporters;

    public IvyModuleSetBuild(IvyModuleSet job) throws IOException {
        super(job);
    }

    public IvyModuleSetBuild(IvyModuleSet project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    /**
     * Exposes {@code ANT_OPTS} to forked processes.
     * 
     * When we fork Ant, we do so directly by executing Java, thus this
     * environment variable is pointless (we have to tweak JVM launch option
     * correctly instead, which can be seen in {@link IvyProcessFactory}), but
     * setting the environment variable explicitly is still useful in case this
     * Ant forks other Ant processes via normal way. See HUDSON-3644.
     */
    @Override
    public EnvVars getEnvironment(TaskListener log) throws IOException, InterruptedException {
        EnvVars envs = super.getEnvironment(log);
        String opts = project.getAntOpts();
        if (opts != null)
            envs.put("ANT_OPTS", opts);
        return envs;
    }

    /**
     * Displays the combined status of all modules.
     * <p>
     * More precisely, this picks up the status of this build itself, plus all
     * the latest builds of the modules that belongs to this build.
     */
    @Override
    public Result getResult() {
        Result r = super.getResult();

        for (IvyBuild b : getModuleLastBuilds().values()) {
            Result br = b.getResult();
            if (r == null)
                r = br;
            else if (br == Result.NOT_BUILT)
                continue; // UGLY: when computing combined status, ignore the
            // modules that were not built
            else if (br != null)
                r = r.combine(br);
        }

        return r;
    }

    /**
     * Returns the filtered changeset entries that match the given module.
     */
    /* package */List<ChangeLogSet.Entry> getChangeSetFor(final IvyModule mod) {
        return new ArrayList<ChangeLogSet.Entry>() {
            {
                for (ChangeLogSet.Entry e : getChangeSet()) {
                    if (isDescendantOf(e, mod)) {
                        add(e);
                    }
                }
            }

            /**
             * Does this change happen somewhere in the given module or its
             * descendants?
             */
            private boolean isDescendantOf(ChangeLogSet.Entry e, IvyModule mod) {
                for (String path : e.getAffectedPaths())
                    if (path.startsWith(mod.getRelativePathToModuleRoot()))
                        return true;
                return false;
            }
        };
    }

    /**
     * Computes the module builds that correspond to this build.
     * <p>
     * A module may be built multiple times (by the user action), so the value
     * is a list.
     */
    public Map<IvyModule, List<IvyBuild>> getModuleBuilds() {
        Collection<IvyModule> mods = getParent().getModules();

        // identify the build number range. [start,end)
        IvyModuleSetBuild nb = getNextBuild();
        int end = nb != null ? nb.getNumber() : Integer.MAX_VALUE;

        // preserve the order by using LinkedHashMap
        Map<IvyModule, List<IvyBuild>> r = new LinkedHashMap<IvyModule, List<IvyBuild>>(mods.size());

        for (IvyModule m : mods) {
            List<IvyBuild> builds = new ArrayList<IvyBuild>();
            IvyBuild b = m.getNearestBuild(number);
            while (b != null && b.getNumber() < end) {
                builds.add(b);
                b = b.getNextBuild();
            }
            r.put(m, builds);
        }

        return r;
    }

    @Override
    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        // map corresponding module build under this object
        if (token.indexOf('$') > 0) {
            IvyModule m = getProject().getModule(token);
            if (m != null)
                return m.getBuildByNumber(getNumber());
        }
        return super.getDynamic(token, req, rsp);
    }

    /**
     * Computes the latest module builds that correspond to this build.
     */
    public Map<IvyModule, IvyBuild> getModuleLastBuilds() {
        Collection<IvyModule> mods = getParent().getModules();

        // identify the build number range. [start,end)
        IvyModuleSetBuild nb = getNextBuild();
        int end = nb != null ? nb.getNumber() : Integer.MAX_VALUE;

        // preserve the order by using LinkedHashMap
        Map<IvyModule, IvyBuild> r = new LinkedHashMap<IvyModule, IvyBuild>(mods.size());

        for (IvyModule m : mods) {
            IvyBuild b = m.getNearestOldBuild(end - 1);
            if (b != null && b.getNumber() >= getNumber())
                r.put(m, b);
        }

        return r;
    }

    public void registerAsProjectAction(IvyReporter reporter) {
        if (projectActionReporters == null)
            projectActionReporters = new ArrayList<IvyReporter>();
        projectActionReporters.add(reporter);
    }

    /**
     * Finds {@link Action}s from all the module builds that belong to this
     * {@link IvyModuleSetBuild}. One action per one {@link IvyModule}, and
     * newer ones take precedence over older ones.
     */
    public <T extends Action> List<T> findModuleBuildActions(Class<T> action) {
        Collection<IvyModule> mods = getParent().getModules();
        List<T> r = new ArrayList<T>(mods.size());

        // identify the build number range. [start,end)
        IvyModuleSetBuild nb = getNextBuild();
        int end = nb != null ? nb.getNumber() - 1 : Integer.MAX_VALUE;

        for (IvyModule m : mods) {
            IvyBuild b = m.getNearestOldBuild(end);
            while (b != null && b.getNumber() >= number) {
                T a = b.getAction(action);
                if (a != null) {
                    r.add(a);
                    break;
                }
                b = b.getPreviousBuild();
            }
        }

        return r;
    }

    public void run() {
        run(new RunnerImpl());
        getProject().updateTransientActions();
    }

    @Override
    public Fingerprint.RangeSet getDownstreamRelationship(AbstractProject that) {
        Fingerprint.RangeSet rs = super.getDownstreamRelationship(that);
        for (List<IvyBuild> builds : getModuleBuilds().values())
            for (IvyBuild b : builds)
                rs.add(b.getDownstreamRelationship(that));
        return rs;
    }

    /**
     * Called when a module build that corresponds to this module set build has
     * completed.
     */
    /* package */void notifyModuleBuild(IvyBuild newBuild) {
        try {
            // update module set build number
            getParent().updateNextBuildNumber();

            // update actions
            Map<IvyModule, List<IvyBuild>> moduleBuilds = getModuleBuilds();

            // actions need to be replaced atomically especially
            // given that two builds might complete simultaneously.
            synchronized (this) {
                boolean modified = false;

                List<Action> actions = getActions();
                Set<Class<? extends AggregatableAction>> individuals = new HashSet<Class<? extends AggregatableAction>>();
                for (Action a : actions) {
                    if (a instanceof IvyAggregatedReport) {
                        IvyAggregatedReport mar = (IvyAggregatedReport) a;
                        mar.update(moduleBuilds, newBuild);
                        individuals.add(mar.getIndividualActionType());
                        modified = true;
                    }
                }

                // see if the new build has any new aggregatable action that we
                // haven't seen.
                for (AggregatableAction aa : newBuild.getActions(AggregatableAction.class)) {
                    if (individuals.add(aa.getClass())) {
                        // new AggregatableAction
                        IvyAggregatedReport mar = aa.createAggregatedAction(this, moduleBuilds);
                        mar.update(moduleBuilds, newBuild);
                        actions.add(mar);
                        modified = true;
                    }
                }

                if (modified) {
                    save();
                    getProject().updateTransientActions();
                }
            }

            // symlink to this module build
            String moduleFsName = newBuild.getProject().getModuleName().toFileSystemName();
            Util.createSymlink(getRootDir(), "../../modules/" + moduleFsName + "/builds/" + newBuild.getId() /*
                                                                                                              * ugly!
                                                                                                              */, moduleFsName,
                    new StreamTaskListener());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to update " + this, e);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Failed to update " + this, e);
        }
    }

    /**
     * The sole job of the {@link IvyModuleSet} build is to update SCM and
     * triggers module builds.
     */
    private class RunnerImpl extends AbstractRunner {

        protected Result doRun(final BuildListener listener) throws Exception {
            PrintStream logger = listener.getLogger();
            try {
                EnvVars envVars = getEnvironment(listener);
                AntInstallation ant = project.getAnt();
                if (ant == null)
                    throw new AbortException("An Ant installation needs to be available for this project to be built.\n"
                            + "Either your server has no Ant installations defined, or the requested Ant version does not exist.");

                parsePoms(listener, logger, envVars);

                if (!project.isAggregatorStyleBuild()) {
                    // start module builds
                } else {
                    // do builds here
                    try {
                        List<BuildWrapper> wrappers = new ArrayList<BuildWrapper>();
                        for (BuildWrapper w : project.getBuildWrappersList())
                            wrappers.add(w);
                        ParametersAction parameters = getAction(ParametersAction.class);
                        if (parameters != null)
                            parameters.createBuildWrappers(IvyModuleSetBuild.this, wrappers);

                        for (BuildWrapper w : wrappers) {
                            Environment e = w.setUp(IvyModuleSetBuild.this, launcher, listener);
                            if (e == null)
                                return Result.FAILURE;
                            buildEnvironments.add(e);
                            e.buildEnvVars(envVars); // #3502: too late for
                            // getEnvironment to do
                            // this
                        }

                        if (!preBuild(listener, project.getPublishers()))
                            return Result.FAILURE;

                        SplittableBuildListener slistener = new SplittableBuildListener(listener);
                        List<String> changedModules = new ArrayList<String>();
                        
                        List<IvyBuild> ivyBuilds = new ArrayList<IvyBuild>();

                        for (IvyModule m : project.sortedActiveModules) {
                            IvyBuild mb = m.newBuild();

                            // Check if incrementalBuild is selected and that
                            // there are changes -
                            // we act as if incrementalBuild is not set if there
                            // are no changes.
                            if (!IvyModuleSetBuild.this.getChangeSet().isEmptySet() && project.isIncrementalBuild()) {
                                // If there are changes for this module, add it.
                                // Also add it if we've never seen this module
                                // before,
                                // or if the previous build of this module
                                // failed or was unstable.
                                if ((mb.getPreviousBuiltBuild() == null) || (!getChangeSetFor(m).isEmpty())
                                        || (mb.getPreviousBuiltBuild().getResult().isWorseThan(Result.SUCCESS))) {
                                    changedModules.add(m.getModuleName().toString());
                                }
                            }

                            mb.setWorkspace(getModuleRoot().child(m.getRelativePathToModuleRoot()));
                            ivyBuilds.add(mb);
                        }
                        
                        Project project = new Project();
                        project.init();
                        for (IvyBuild ivyBuild : ivyBuilds) {
                            String buildFile = envVars.expand(ivyBuild.getProject().getParent().getBuildFile());
                            File antBuildFile = new File(ivyBuild.getWorkspace().getRemote(), buildFile);
                            ProjectHelper.configureProject(project, antBuildFile);

                            VariableResolver<String> vr = ivyBuild.getBuildVariableResolver();

                            String targets = Util.replaceMacro(envVars.expand(ivyBuild.getProject().getTargets()), vr);
                            Vector<String> targetVector = new Vector<String>(Arrays.asList(targets.split("[\t\r\n]+")));
                            try {
                                project.executeTargets(targetVector);
                                ivyBuild.setResult(Result.SUCCESS);
                            } catch (BuildException e) {
                                ivyBuild.setResult(Result.FAILURE);
                            }
                        }
                        
                    } finally {
                        // tear down in reverse order
                        boolean failed = false;
                        for (int i = buildEnvironments.size() - 1; i >= 0; i--) {
                            if (!buildEnvironments.get(i).tearDown(IvyModuleSetBuild.this, listener)) {
                                failed = true;
                            }
                        }
                        buildEnvironments = null;
                        // WARNING The return in the finally clause will trump
                        // any return before
                        if (failed)
                            return Result.FAILURE;
                    }
                }

                return null;
            } catch (AbortException e) {
                if (e.getMessage() != null)
                    listener.error(e.getMessage());
                return Result.FAILURE;
            } catch (InterruptedIOException e) {
                e.printStackTrace(listener.error("Aborted Ivy execution for InterruptedIOException"));
                return Result.ABORTED;
            } catch (InterruptedException e) {
                e.printStackTrace(listener.error("Aborted Ivy execution for InterruptedException"));
                return Result.ABORTED;
            } catch (IOException e) {
                e.printStackTrace(listener.error(Messages.IvyModuleSetBuild_FailedToParseIvyXml()));
                return Result.FAILURE;
            } catch (RunnerAbortedException e) {
                return Result.FAILURE;
            } catch (RuntimeException e) {
                // bug in the code.
                e.printStackTrace(listener.error("Processing failed due to a bug in the code. Please report this to users@hudson.dev.java.net"));
                logger.println("project=" + project);
                logger.println("project.getModules()=" + project.getModules());
                throw e;
            }
        }

        private void parsePoms(BuildListener listener, PrintStream logger, EnvVars envVars) throws IOException, InterruptedException {
            logger.println("Parsing POMs");

            List<IvyModuleInfo> poms;
            try {
                poms = getModuleRoot().act(new IvyXmlParser(listener, null, project));
            } catch (IOException e) {
                if (e.getCause() instanceof AbortException)
                    throw (AbortException) e.getCause();
                throw e;
            }

            // update the module list
            Map<ModuleName, IvyModule> modules = project.modules;
            synchronized (modules) {
                Map<ModuleName, IvyModule> old = new HashMap<ModuleName, IvyModule>(modules);
                List<IvyModule> sortedModules = new ArrayList<IvyModule>();

                modules.clear();
                for (IvyModuleInfo pom : poms) {
                    IvyModule mm = old.get(pom.name);
                    if (mm != null) {// found an existing matching module
                        if (debug)
                            logger.println("Reconfiguring " + mm);
                        mm.reconfigure(pom);
                        modules.put(pom.name, mm);
                    } else {// this looks like a new module
                        logger.println(Messages.IvyModuleSetBuild_DiscoveredModule(pom.name, pom.displayName));
                        mm = new IvyModule(project, pom, getNumber());
                        modules.put(mm.getModuleName(), mm);
                    }
                    sortedModules.add(mm);
                    mm.save();
                }
                
                // at this point the list contains all the live modules
                project.sortedActiveModules = sortedModules;

                // remaining modules are no longer active.
                old.keySet().removeAll(modules.keySet());
                for (IvyModule om : old.values()) {
                    if (debug)
                        logger.println("Disabling " + om);
                    om.makeDisabled(true);
                }
                modules.putAll(old);
            }

            // we might have added new modules
            Hudson.getInstance().rebuildDependencyGraph();

            // module builds must start with this build's number
            for (IvyModule m : modules.values())
                m.updateNextBuildNumber(getNumber());
        }

        protected void post2(BuildListener listener) throws Exception {
            // asynchronous executions from the build might have left some
            // unsaved state,
            // so just to be safe, save them all.
            for (IvyBuild b : getModuleLastBuilds().values())
                b.save();

            performAllBuildStep(listener, project.getPublishers(), true);
            performAllBuildStep(listener, project.getProperties(), true);

            // aggregate all module fingerprints to us,
            // so that dependencies between module builds can be understood as
            // dependencies between module set builds.
            // TODO: we really want to implement this as a publisher,
            // but we don't want to ask for a user configuration, nor should it
            // show up in the persisted record.
            // IvyFingerprinter.aggregate(IvyModuleSetBuild.this);
        }

        @Override
        public void cleanUp(BuildListener listener) throws Exception {
            if (project.isAggregatorStyleBuild()) {
                // schedule downstream builds. for non aggregator style builds,
                // this is done by each module
                scheduleDownstreamBuilds(listener);
            }

            performAllBuildStep(listener, project.getPublishers(), false);
            performAllBuildStep(listener, project.getProperties(), false);
        }
    }

    /**
     * Runs Ant and builds the project.
     * 
     * This is only used for {@link IvyModuleSet#isAggregatorStyleBuild() the
     * aggregator style build}.
     */
    private static final class Builder extends IvyBuilder {
        public Builder(BuildListener listener, Collection<IvyModule> modules, List<String> goals, Map<String, String> systemProps) {
            super(listener, goals, systemProps);
        }

        private static final long serialVersionUID = 1L;

        public Result call() throws IOException {
            return null;
        }
    }

    /**
     * Used to tunnel exception from Ant through remoting.
     */
    private static final class AntExecutionException extends RuntimeException {
        private AntExecutionException(Exception cause) {
            super(cause);
        }

        @Override
        public Exception getCause() {
            return (Exception) super.getCause();
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Executed on the slave to parse ivy.xml files and extract information into
     * {@link IvyModuleInfo}, which will be then brought back to the master.
     */
    private static final class IvyXmlParser implements FileCallable<List<IvyModuleInfo>> {
        private final BuildListener listener;
        /**
         * Capture the value of the static field so that the debug flag takes an
         * effect even when {@link IvyXmlParser} runs in a slave.
         */
        private final boolean verbose = debug;
        private final AntInstallation antHome;
        private final String ivyFilePattern;
        private final String alternateSettings;

        public IvyXmlParser(BuildListener listener, AntInstallation antHome, IvyModuleSet project) {
            // project cannot be shipped to the remote JVM, so all the relevant
            // properties need to be captured now.
            this.listener = listener;
            this.antHome = antHome;
            this.ivyFilePattern = project.getIvyFilePattern();
            this.alternateSettings = project.getAlternateSettings();
        }

        public List<IvyModuleInfo> invoke(File ws, VirtualChannel channel) throws IOException {
            FileSet ivyFiles = Util.createFileSet(ws, ivyFilePattern);

            Ivy ivy = getIvy();
            HashMap<ModuleDescriptor, String> moduleDescriptors = new HashMap<ModuleDescriptor, String>();
            for (String ivyFilePath : ivyFiles.getDirectoryScanner().getIncludedFiles()) {
                final File ivyFile = new File(ws, ivyFilePath);

                ModuleDescriptor module = (ModuleDescriptor) ivy.execute(new IvyCallback() {
                    public Object doInIvyContext(Ivy ivy, IvyContext context) {
                        try {
                            return ModuleDescriptorParserRegistry.getInstance().parseDescriptor(ivy.getSettings(), ivyFile.toURI().toURL(),
                                    ivy.getSettings().doValidate());
                        } catch (MalformedURLException e) {
                            LOGGER.log(Level.WARNING, "The URL is malformed : " + ivyFile, e);
                            return null;
                        } catch (ParseException e) {
                            LOGGER.log(Level.WARNING, "Parsing error while reading the ivy file " + ivyFile, e);
                            return null;
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "I/O error while reading the ivy file " + ivyFile, e);
                            return null;
                        }
                    }
                });
                moduleDescriptors.put(module, ivyFilePath);
            }
            
            List<IvyModuleInfo> infos = new ArrayList<IvyModuleInfo>();
            List<ModuleDescriptor> sortedModuleDescriptors = ivy.sortModuleDescriptors(moduleDescriptors.keySet(), SortOptions.DEFAULT);
            for (ModuleDescriptor moduleDescriptor : sortedModuleDescriptors) {
                infos.add(new IvyModuleInfo(moduleDescriptor, moduleDescriptors.get(moduleDescriptor)));
            }

            return infos;
        }

        /**
         * 
         * @return the Ivy instance based on the {@link #ivyConfName}
         * 
         * @throws ParseException
         * @throws IOException
         */
        public Ivy getIvy() {
            Message.setDefaultLogger(new IvyMessageImpl());
            Ivy ivy = Ivy.newInstance();
            Ivy configured = null;
            try {
                ivy.configureDefault();
                LOGGER.fine("Configured Ivy using default 2.0 settings");
                configured = ivy;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while reading the default Ivy 2.0 settings", e);
            }
            return configured;
        }

        private static final long serialVersionUID = 1L;
    }

    private static final Logger LOGGER = Logger.getLogger(IvyModuleSetBuild.class.getName());

    /**
     * Extra verbose debug switch.
     */
    public static boolean debug = false;

    @Override
    public IvyModuleSet getParent() {// don't know why, but javac wants this
        return super.getParent();
    }
}
