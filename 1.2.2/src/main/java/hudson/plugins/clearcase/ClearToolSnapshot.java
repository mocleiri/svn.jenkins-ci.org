/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer, Vincent Latombe
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

import hudson.FilePath;
import hudson.Util;
import hudson.model.Computer;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.util.ArgumentListBuilder;
import hudson.util.VariableResolver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;

public class ClearToolSnapshot extends ClearToolExec {

    private String optionalMkviewParameters;
    private boolean useUpdate;

    public ClearToolSnapshot(VariableResolver<String> variableResolver, ClearToolLauncher launcher) {
        super(variableResolver, launcher);
    }

    public ClearToolSnapshot(VariableResolver<String> variableResolver, ClearToolLauncher launcher, String optionalParameters, final boolean useUpdate) {
        this(variableResolver, launcher);
        this.optionalMkviewParameters = optionalParameters;
        this.useUpdate = useUpdate;
    }

    /**
     * To set the config spec of a snapshot view, you must be in or under the snapshot view root directory.
     * 
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    public void setcs(String viewName, String configSpec) throws IOException, InterruptedException {
        if (configSpec == null) {
            configSpec = "";
        }

        FilePath workspace = launcher.getWorkspace();
        FilePath configSpecFile = workspace.createTextTempFile("configspec", ".txt", configSpec);
        String csLocation = "";

        if (!configSpec.equals("")) {
            csLocation = ".." + File.separatorChar + configSpecFile.getName();
            csLocation = PathUtil.convertPathForOS(csLocation, launcher.getLauncher());
        }

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("setcs");
        if (!csLocation.equals("")) {
            cmd.add(csLocation);
        } else {
            cmd.add("-current");
        }
        String output = runAndProcessOutput(cmd, new ByteArrayInputStream("yes".getBytes()), workspace.child(viewName), false, null);
        configSpecFile.delete();

        if (output.contains("cleartool: Warning: An update is already in progress for view")) {
            throw new IOException("View update failed: " + output);
        }
    }

    /**
     * To set the config spec of a snapshot view, you must be in or under the snapshot view root directory.
     * 
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    public void setcsCurrent(String viewName) throws IOException, InterruptedException {
        FilePath workspace = launcher.getWorkspace();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("setcs");
        cmd.add("-current");
        String output = runAndProcessOutput(cmd, new ByteArrayInputStream("yes".getBytes()), workspace.child(viewName), false, null);

        if (output.contains("cleartool: Warning: An update is already in progress for view")) {
            throw new IOException("View update failed: " + output);
        }
    }

    public void mkview(String viewName, String streamSelector) throws IOException, InterruptedException {
        boolean isOptionalParamContainsHost = false;
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("mkview");
        cmd.add("-snapshot");
        if (streamSelector != null) {
            cmd.add("-stream");
            cmd.add(streamSelector);
        }
        cmd.add("-tag");
        cmd.add(viewName);

        if ((optionalMkviewParameters != null) && (optionalMkviewParameters.length() > 0)) {

            // Somewhat a hack here, since this method may be called more than once with a
            // different viewName each time, we need to do the viewname substitution at usage time.

            final String params = optionalMkviewParameters.replaceAll("\\$\\{CLEARCASE_VIEWNAME\\}", viewName);
            String variabledResolvedParams = Util.replaceMacro(params, this.variableResolver);
            cmd.addTokenized(variabledResolvedParams);
            isOptionalParamContainsHost = optionalMkviewParameters.contains("-host");  
        }
        if (!isOptionalParamContainsHost) {
            cmd.add(viewName);
        }

        launcher.run(cmd.toCommandArray(), null, null, null);
    }

    public void mkview(String viewName, String streamSelector, String defaultStorageDir) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Snapshot view does not support mkview (String, String)");
    }

    public void rmview(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmview");
        cmd.add("-force");
        cmd.add(viewName);

        String output = runAndProcessOutput(cmd, null, null, false, null);

        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to remove view: " + output);
        }

        FilePath viewFilePath = launcher.getWorkspace().child(viewName);
        if (viewFilePath.exists()) {
            launcher.getListener().getLogger().println("Removing view folder as it was not removed when the view was removed.");
            viewFilePath.deleteRecursive();
        }
    }

    @Override
    public void update(String viewName, String[] loadRules) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("update");
        cmd.add("-force");
        cmd.add("-overwrite");
        cmd.add("-log", "NUL");
        if (!ArrayUtils.isEmpty(loadRules)) {
            cmd.add("-add_loadrules");
            for (String loadRule : loadRules) {
                String loadRuleLocation = PathUtil.convertPathForOS(removePrefixLoadRule(loadRule), getLauncher().getLauncher());
                if (loadRuleLocation.matches(".*\\s.*")) {
                    cmd.addQuoted(loadRuleLocation);
                } else {
                    cmd.add(loadRuleLocation);
                }
            }
        }
        
        String output = runAndProcessOutput(cmd, null, getLauncher().getWorkspace().child(viewName), false, null);
        
        if (output.contains("cleartool: Warning: An update is already in progress for view")) {
            throw new IOException("View update failed: " + output);
        }
    }

    private String removePrefixLoadRule(String loadRule) {
        char firstChar = loadRule.charAt(0);
        if (firstChar == '\\' || firstChar == '/') {
            return loadRule.substring(1);
        } else {
            return loadRule;
        }
    }

    @Override
    protected FilePath getRootViewPath(ClearToolLauncher launcher) {
        return launcher.getWorkspace();
    }

    public void startView(String viewTag) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Snapshot view does not support startview");
    }

    public void syncronizeViewWithStream(String viewName, String stream) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Snapshot view does not support syncronize");
    }

    /**
     * Tries to remove all trace of a view, even if the view is corrupted. 
     *
     * Takes the following steps: 
     *
     * <li>rmviewUuid(uuid) 
     * <li>unregisterView(uuid) 
     * <li>rmviewtag(viewName) 
     * <li>attempts to remove storage directory, in addition to the view root
     */
    @Override
    public void wipeView(final String viewName) throws InterruptedException, IOException {
        Properties viewDataPrp = null;

        try {
            // Get the view UUID and storage directory
            viewDataPrp = getViewData(viewName);
        } catch (IOException e) {
            logRedundantCleartoolError(null, e);
            return;
        }
        // Get the view UUID and storage directory
        final String uuid = viewDataPrp.getProperty(ClearToolViewProp.UUID.name());
        final String globalPath = viewDataPrp.getProperty(ClearToolViewProp.GLOBAL_PATH.name()).replace("[\\\\/]?\\.view\\.stg", "");

        if (doesViewExist(viewName)) {
            boolean removed = false;
            // try removing the view the 'easy' way first
            try {
                rmview(viewName);
                removed = true;
            } catch (IOException ex) {
                logRedundantCleartoolError(null, ex);
            }
            if (!removed) {
                try {
                    rmviewUuid(uuid);
                } catch (Exception ex) {
                    logRedundantCleartoolError(null, ex);
                }
                try {
                    unregisterView(uuid);
                } catch (Exception ex) {
                    logRedundantCleartoolError(null, ex);
                }
                try {
                    rmviewtag(viewName);
                } catch (Exception ex) {
                    logRedundantCleartoolError(null, ex);
                }
            }
            // remove storage directory, if exists
            try {
                final FilePath viewStorageDir = new FilePath(Computer.currentComputer().getChannel(), globalPath);
                if (viewStorageDir.exists()) {
                    viewStorageDir.deleteRecursive();
                }
            } catch (Exception ex) {
                logRedundantCleartoolError(null, ex);
            }
            // Remove the view root dir, assuming it is created in the workspace as a directory with the view name.
            // There seems no way to otherwise determine the view root location, so some hope is required here.
            // reference:
            // http://www-01.ibm.com/support/docview.wss?rs=0&uid=swg21148996
            final FilePath viewRootDir = launcher.getWorkspace().child(viewName);
            if (viewRootDir.exists()) {
                try {
                    viewRootDir.deleteRecursive();
                } catch (Exception ex) {
                    logRedundantCleartoolError(null, ex);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareView(String viewName, String stream, boolean createNewView) throws InterruptedException, IOException {
        if (createNewView && doesViewExist(viewName)) {
            try {
                wipeView(viewName);
            } catch (IOException e) {
                // nothing much to do here, still want to try to create the view.
            }
        }
        if (!doesViewExist(viewName)) {
            mkview(viewName, stream);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareView(String viewName, String stream) throws InterruptedException, IOException {
        prepareView(viewName, stream, !useUpdate);
    }
}
