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
package hudson.plugins.clearcase.action;

import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolSnapshot;
import hudson.plugins.clearcase.UcmView;
import hudson.plugins.clearcase.View;
import hudson.plugins.clearcase.util.PathUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * Check out action that will check out files into a UCM snapshot view. Checking
 * out the files will also update the load rules in the view.
 */
public class UcmSnapshotCheckoutAction extends AbstractCheckoutAction {

	private ClearToolSnapshot cleartool;
    
    private String streamSelector;
    
    private String[] loadRules;
    
    private boolean useUpdate;
    
    private String viewPath;
    
    public UcmSnapshotCheckoutAction(ClearToolSnapshot cleartool, String streamSelector,
            String[] loadRules, boolean useUpdate) {
        this(cleartool, streamSelector, loadRules, useUpdate, null);
    }
    
    public UcmSnapshotCheckoutAction(ClearToolSnapshot cleartool, String streamSelector,
                                     String[] loadRules, boolean useUpdate, String viewPath) {
        super();
        this.cleartool = cleartool;
        this.streamSelector = streamSelector;
        this.loadRules = loadRules;
        this.useUpdate = useUpdate;
        this.viewPath = viewPath;
    }
    
    public View checkout(Launcher launcher, FilePath workspace,
                            String viewName) throws IOException, InterruptedException {
        String path = getViewPath(viewName);
        this.cleartool.setViewPath(path);
    	boolean viewTagExists = cleartool.doesViewExist(viewName);
        FilePath viewPathFilePath = new FilePath(workspace, path);
        boolean viewCreatedDuringThisAction = false;
		if (viewPathFilePath.exists()) {
            if (!viewTagExists) {
                launcher.getListener().getLogger().println("\"" + viewPathFilePath + "\" exists, but not the view tag \"" + viewName + "\". Moving the existing path to \"" + viewPathFilePath + ".keep\"");
                viewPathFilePath.renameTo(getKeepPath(workspace, viewName));
                cleartool.mkview(viewName, streamSelector);
                viewCreatedDuringThisAction = true;
            }
            if (!this.useUpdate) {
                cleartool.rmview(viewName);
                cleartool.mkview(viewName, streamSelector);
                viewCreatedDuringThisAction = true;
            }
        } else {
            if (viewTagExists) {
                launcher.getListener().fatalError("View path " + viewPathFilePath + " does not exist, but the view tag " + viewName + " does.\n"
                                                  + "View cannot be created - build aborting.");
                return null;
            } else {
                cleartool.mkview(viewName, streamSelector);
                viewCreatedDuringThisAction = true;
            }
        }
        // If the view was created during this run, its config spec is already in sync with the stream
        if (!viewCreatedDuringThisAction) {
            // Full update in order to get the new config spec
            cleartool.update(path, null);
        }
        launcher.getListener().getLogger().println("Here is the PREVIOUS config specification used for this view");
        String configSpec = cleartool.catcs(viewName);
        StringBuilder newConfigSpecSb = new StringBuilder();
        newConfigSpecSb.append(getLoadRuleFreeConfigSpec(configSpec)).append(PathUtil.newLineForOS(launcher.isUnix()));
        Set<String> oldLoadRules = extractLoadRules(configSpec);
        Set<String> newLoadRules = new LinkedHashSet<String>();
        Set<String> addedLoadRules = new LinkedHashSet<String>();
        Set<String> removedLoadRules = new HashSet<String>();
        for(String loadRule : loadRules) {
            if (loadRule.startsWith("\\") || loadRule.startsWith("/")) {
                loadRule = loadRule.substring(1);
            }
            newLoadRules.add(loadRule);
            if (!oldLoadRules.contains(loadRule)) {
                addedLoadRules.add(loadRule);
            }
        }
        for(String loadRule : oldLoadRules) {
            if (!newLoadRules.contains(loadRule)) {
                removedLoadRules.add(loadRule);
            }
        }
        if (!removedLoadRules.isEmpty()) {
            // If at least one load rule is removed we must call setcs with a new config spec to remove it
            for (String loadRule : newLoadRules) {
                // Make sure the load rule starts with \ or /, as appropriate
                loadRule = PathUtil.fileSepForOS(launcher.isUnix()) + loadRule;
                newConfigSpecSb.append("load ").append(loadRule.trim()).append(PathUtil.newLineForOS(launcher.isUnix()));
            }
            try {
                // This will remove defect load rules, and add the new ones
                cleartool.setcs(viewName, newConfigSpecSb.toString());
            } catch (IOException e) {
                launcher.getListener().fatalError(e.toString());
                return null;
            }
        } else if (!addedLoadRules.isEmpty()) {
            // If there are only new load rules, we can add them using cleartool update -add_loadrules 
            cleartool.update(viewName, StringUtils.join(addedLoadRules, ";"));
        }
        // Get the latest config spec and print it
        launcher.getListener().getLogger().println("Here is the CURRENT config specification used for this view");
        configSpec = cleartool.catcs(viewName);
        
        return new UcmView(viewName, path, configSpec, streamSelector, null);
    }

    private String getViewPath(String viewName) {
        if (StringUtils.isNotEmpty(this.viewPath)) {
            return viewPath;
        } else {
            return viewName;
        }
    }

    private FilePath getKeepPath(FilePath workspace, String viewName)
            throws IOException, InterruptedException {
        FilePath keepPath = new FilePath(workspace, viewName + ".keep");
        int i = 0;
        while(keepPath.exists() && i < Integer.MAX_VALUE) {
            keepPath = new FilePath(workspace, viewName + ".keep." + ++i);
        }
        return keepPath;
    }
}
