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

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.plugins.clearcase.ucm.UcmCommon;

import java.util.List;

public class ClearCaseReportAction implements Action {

    private AbstractBuild<?,?> build;
    private ClearCaseDataAction clearCaseDataAction;
    private static String urlName = "clearcaseInformation";
	
	public ClearCaseReportAction(AbstractBuild<?,?> build) {
        this.build = build;
    }

    public String getIconFileName(){
        return "gear2.gif";
    }
    
    public String getDisplayName(){
        return "ClearCase Information";
    }
    
    public String getUrlName(){
         return urlName;
    }
    
    public static String getUrlNameStat(){
        return urlName;
    }

    // Used by the index.jelly of this class to include the sidebar.jelly
    public AbstractBuild<?, ?> getOwner() {
        return build;
    }

    public String getConfigSpecHtml() {
        String configSpecHtml = getConfigSpec();
        configSpecHtml = configSpecHtml.replaceAll("\n","<br/>");
        return configSpecHtml;
    }
    
    public List<UcmCommon.Baseline> getBaselines() {
        ClearCaseDataAction clearCaseDataAction = getClearCaseDataAction();
        
        if (clearCaseDataAction != null) {
            return clearCaseDataAction.getLatestBlsOnConfiguredStream();
        } else {
            return null;
        }
    }

    private ClearCaseDataAction getClearCaseDataAction() {
        if (clearCaseDataAction == null) {
            clearCaseDataAction = build.getAction(ClearCaseDataAction.class);
        }
        return clearCaseDataAction;
    }
    
    public boolean isBaselineInfo() {
        ClearCaseDataAction dataAction = getClearCaseDataAction();
        if (dataAction != null) {
            return dataAction.hasBaselinesInformation();
        }
        return false;
    }
    
	public String getStreamSelector() {
        ClearCaseDataAction dataAction = getClearCaseDataAction();
        if (dataAction != null) {
            return dataAction.getStreamSelector();
        }
        return null;
	}
	
	public String getConfigSpec() {
        ClearCaseDataAction dataAction = getClearCaseDataAction();
        if (dataAction != null) {
            return dataAction.getConfigSpec();
        }
        return null;
	}
    
}
