/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
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
package hudson.plugins.clearcase;

import static hudson.Util.fixEmpty;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.plugins.clearcase.ClearCaseSCM.ClearCaseScmDescriptor;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.action.UcmDynamicCheckoutAction;
import hudson.plugins.clearcase.action.UcmSnapshotCheckoutAction;
import hudson.plugins.clearcase.history.HistoryAction;
import hudson.plugins.clearcase.ucm.UcmChangeLogParser;
import hudson.plugins.clearcase.ucm.UcmCommon;
import hudson.plugins.clearcase.ucm.UcmHistoryAction;
import hudson.plugins.clearcase.ucm.UcmSaveChangeLogAction;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.VariableResolver;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * SCM for ClearCaseUCM. This SCM will create a UCM view from a stream and apply a list of load rules to it.
 */
public class ClearCaseUcmSCM extends AbstractClearCaseScm {

    private static final String STREAM_PREFIX = "stream:";

    //private final static String AUTO_ALLOCATE_VIEW_NAME = "${STREAM}_${JOB_NAME}_bs_hudson_view";

    private final String stream;
    transient private String paramStream;
    private final String overrideBranchName;
    private boolean allocateViewName;

    @DataBoundConstructor
    public ClearCaseUcmSCM(String stream, String loadrules, String viewname, boolean usedynamicview, String viewdrive, String mkviewoptionalparam,
            boolean filterOutDestroySubBranchEvent, boolean useUpdate, boolean rmviewonrename, String excludedRegions, String multiSitePollBuffer,
            String overrideBranchName, boolean createDynView, String winDynStorageDir, String unixDynStorageDir, boolean freezeCode, boolean recreateView,
            boolean allocateViewName) {
        super(viewname, mkviewoptionalparam, filterOutDestroySubBranchEvent, useUpdate, rmviewonrename, excludedRegions, usedynamicview, viewdrive, loadrules,
                multiSitePollBuffer, createDynView, winDynStorageDir, unixDynStorageDir, freezeCode, recreateView);
        this.stream = shortenStreamName(stream);
        this.allocateViewName = allocateViewName;
        this.paramStream = "";
        this.overrideBranchName = overrideBranchName;
    }

    @Deprecated
    public ClearCaseUcmSCM(String stream, String loadrules, String viewname, boolean usedynamicview, String viewdrive, String mkviewoptionalparam,
            boolean filterOutDestroySubBranchEvent, boolean useUpdate, boolean rmviewonrename) {
        this(stream, loadrules, viewname, usedynamicview, viewdrive, mkviewoptionalparam, filterOutDestroySubBranchEvent, useUpdate, rmviewonrename, "", null,
                "", false, null, null, false, false, false);
    }

    /**
     * Return the default stream configured for the project.
     * 
     * @return string containing the stream selector.
     */
    public String getStream() {
        return stream;
        //return StringUtils.defaultIfEmpty(paramStream, stream);
    }
    
    /**
     * Return the stream associated with a build.
     * 
     * @return string containing the build stream
     */
    public String getBuildStream() {
        return StringUtils.defaultIfEmpty(paramStream, stream);
    }

    public boolean isAllocateViewName() {
        return allocateViewName;
    }

    public void setAllocateViewName(boolean allocateViewName) {
        this.allocateViewName = allocateViewName;
    }

    /**
     * Return the branch type used for changelog and polling. By default this will be the empty string, and the stream
     * will be split to get the branch.
     * 
     * @return string containing the branch type.
     */
    public String getOverrideBranchName() {
        return overrideBranchName;
    }

    @Override
    public ClearCaseUcmScmDescriptor getDescriptor() {
        return PluginImpl.UCM_DESCRIPTOR;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new UcmChangeLogParser();
    }

    @Override
    public String[] getBranchNames() {
        if (StringUtils.isNotEmpty(overrideBranchName)) {
            return new String[] { overrideBranchName };
        } else {
            String branch = getStream();
            int indexOfAt = branch.indexOf("@");
            if (indexOfAt > -1) {
                branch = branch.substring(0, indexOfAt);
            }
            return new String[] { branch };
        }
    }

    public String[] getBuildBranchNames() {
        if (StringUtils.isNotEmpty(overrideBranchName)) {
            return new String[] { overrideBranchName };
        } else {
            String branch = getBuildStream();
            int indexOfAt = branch.indexOf('@');
            if (indexOfAt > -1) {
                branch = branch.substring(1, indexOfAt);
            }
            return new String[] { branch };
        }
    }

    @Override
    public boolean checkout(final AbstractBuild build, final Launcher launcher, final FilePath workspace, final BuildListener listener, final File changelogFile) throws IOException,
            InterruptedException {
        final ClearToolLauncher clearToolLauncher = createClearToolLauncher(listener, workspace, launcher);
        // Create actions
        final VariableResolver<String> variableResolver = new BuildVariableResolver(build, getCurrentComputer());

        final CheckOutAction checkoutAction = createCheckOutAction(variableResolver, clearToolLauncher, build);
        final UcmHistoryAction historyAction = (UcmHistoryAction) createHistoryAction(variableResolver, clearToolLauncher, build);
        historyAction.setStream(getBuildStream());
        final SaveChangeLogAction saveChangeLogAction = createSaveChangeLogAction(clearToolLauncher);

        // Checkout code
        final String coNormalizedViewName = generateNormalizedViewName(build);

        build.addAction(new ClearCaseDataAction());

        if (checkoutAction.checkout(launcher, workspace, coNormalizedViewName)) {

            // Gather change log
            List<? extends ChangeLogSet.Entry> changelogEntries = null;
            if (build.getPreviousBuild() != null) {
                final Run prevBuild = build.getPreviousBuild();
                final Date lastBuildTime = getBuildTime(prevBuild);

                changelogEntries = historyAction.getChanges(lastBuildTime, coNormalizedViewName, getBuildBranchNames(), getViewPaths());
            }

            // Save change log
            if (CollectionUtils.isEmpty(changelogEntries)) {
                // no changes
                return createEmptyChangeLog(changelogFile, listener, "changelog");
            } else {
                saveChangeLogAction.saveChangeLog(changelogFile, changelogEntries);
            }

        } else {
            throw new AbortException();
        }

        return true;
    }

    @Override
    public String generateNormalizedViewName(VariableResolver<String> variableResolver, String modViewName) {
        // Modify the view name in order to support concurrent builds
        if (allocateViewName) {
            modViewName += "_" + UcmCommon.getNoVob(getBuildStream());
            //modViewName = AUTO_ALLOCATE_VIEW_NAME.replace("${STREAM}", UcmCommon.getNoVob(getStream()));
        }
        return super.generateNormalizedViewName(variableResolver, modViewName);
    }

    @Override
    protected CheckOutAction createCheckOutAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build) {
        // set value in paramStream (if build is parameterized, support changing the build stream)
        this.paramStream = shortenStreamName((String) build.getBuildVariables().get("STREAM"));

        final CheckOutAction action;
        if (isUseDynamicView()) {
            action = new UcmDynamicCheckoutAction(createClearTool(variableResolver, launcher), getBuildStream(), isCreateDynView(), build, isFreezeCode());
        } else {
            action = new UcmSnapshotCheckoutAction(createClearTool(variableResolver, launcher),getBuildStream(), getViewPaths(), build, isUseUpdate(), isFreezeCode());
        }
        return action;
    }

    @Override
    protected HistoryAction createHistoryAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build) {
        ClearTool ct = createClearTool(variableResolver, launcher);
        // String viewName, String stream, String unixDynStorageDir, String winDynStorageDir, String viewDrive
        
        // The following assumes the view folder will have the same name as the view.  This is not 100% guaranteed, 
        // as it is possible to use a different folder name by specifying options to the 'mkview' command.
        // However, the setting of CLEARTOOL_VIEWPATH seems to also rely on this assumption, so we will use it too.
        final String viewRoot = isUseDynamicView() ? getViewDrive() : launcher.getWorkspace().getRemote();
        UcmHistoryAction action = new UcmHistoryAction(ct, isUseDynamicView(), configureFilters(launcher), getStream(), viewRoot, build, isFreezeCode());

        try {
            String pwv = ct.pwv(generateNormalizedViewName((BuildVariableResolver) variableResolver));

            if (pwv != null) {
                if (pwv.contains("/")) {
                    pwv += "/";
                } else {
                    pwv += "\\";
                }
                action.setExtendedViewPath(pwv);
            }
        } catch (Exception e) {
            Logger.getLogger(ClearCaseUcmSCM.class.getName()).log(Level.WARNING, "Exception when running 'cleartool pwv'", e);
        }

        return action;
    }

    @Override
    protected SaveChangeLogAction createSaveChangeLogAction(ClearToolLauncher launcher) {
        return new UcmSaveChangeLogAction();
    }

    @Override
    protected ClearTool createClearTool(VariableResolver<String> variableResolver, ClearToolLauncher launcher) {
        if (isUseDynamicView()) {
            return new ClearToolDynamicUCM(variableResolver, launcher, getViewDrive(), getMkviewOptionalParam(),
                    getNormalizedUnixDynStorageDir(variableResolver), getNormalizedWinDynStorageDir(variableResolver), isCreateDynView(), isRecreateView());
        } else {
            return super.createClearTool(variableResolver, launcher);
        }
    }

    private String shortenStreamName(final String longStream) {
        if (longStream != null && longStream.startsWith(STREAM_PREFIX)) {
            return longStream.substring(STREAM_PREFIX.length());
        } else {
            return longStream;
        }
    }

    /**
     * ClearCase UCM SCM descriptor
     * 
     * @author Erik Ramfelt
     */
    public static class ClearCaseUcmScmDescriptor extends SCMDescriptor<ClearCaseUcmSCM> implements ModelObject {
        
        private ClearCaseScmDescriptor baseDescriptor;

        public ClearCaseUcmScmDescriptor(ClearCaseScmDescriptor baseDescriptor) {
            super(ClearCaseUcmSCM.class, null);
            this.baseDescriptor = baseDescriptor;
            load();
        }

        public String getDefaultViewName() {
            return baseDescriptor.getDefaultViewName();
        }

        @Override
        public String getDisplayName() {
            return "UCM ClearCase";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) {
            return true;
        }

        public String getDefaultWinDynStorageDir() {
            return baseDescriptor.getDefaultWinDynStorageDir();
        }

        public String getDefaultUnixDynStorageDir() {
            return baseDescriptor.getDefaultUnixDynStorageDir();
        }

        @Override
        public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            ClearCaseUcmSCM scm = new ClearCaseUcmSCM(
                                                      req.getParameter("ucm.stream"),
                                                      req.getParameter("ucm.loadrules"),
                                                      req.getParameter("ucm.viewname"),
                                                      req.getParameter("ucm.usedynamicview") != null,
                                                      req.getParameter("ucm.viewdrive"),
                                                      req.getParameter("ucm.mkviewoptionalparam"),
                                                      req.getParameter("ucm.filterOutDestroySubBranchEvent") != null,
                                                      req.getParameter("ucm.useupdate") != null,
                                                      req.getParameter("ucm.rmviewonrename") != null,
                                                      req.getParameter("ucm.excludedRegions"),
                                                      fixEmpty(req.getParameter("ucm.multiSitePollBuffer")),
                                                      req.getParameter("ucm.overrideBranchName"),
                                                      req.getParameter("ucm.createDynView") != null,
                                                      req.getParameter("ucm.winDynStorageDir"),
                                                      req.getParameter("ucm.unixDynStorageDir"),
                                                      req.getParameter("ucm.freezeCode") != null,
                                                      req.getParameter("ucm.recreateView") != null,
                                                      req.getParameter("ucm.allocateViewName") != null
                                                      );
            return scm;
        }
    }
}
