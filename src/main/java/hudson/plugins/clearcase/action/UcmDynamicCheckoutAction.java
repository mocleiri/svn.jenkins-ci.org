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
import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolLauncher;
import hudson.plugins.clearcase.ClearCaseDataAction;
import hudson.plugins.clearcase.ucm.UcmCommon;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Check out action for dynamic views.
 * This will not update any files from the repository as it is a dynamic view.
 * It only makes sure the view is started as config specs don't exist in UCM
 */
public class UcmDynamicCheckoutAction implements CheckOutAction {
	private static final String CONFIGURED_STREAM_VIEW_SUFFIX = "_hudson_auto_view";
	private static final String BUILD_STREAM_PREFIX = "hudsonbuild_auto_stream.";	
	private static final String BASELINE_NAME = "hudson_co_";
	private static final String BASELINE_COMMENT = "hudson_co_";  
	
    private ClearTool cleartool;
    private String stream;
    private boolean createDynView;
    private String winDynStorageDir;
    private String unixDynStorageDir;
    private AbstractBuild build;
    private boolean freezeCode;
    private boolean recreateView;
    
    public UcmDynamicCheckoutAction(ClearTool cleartool, String stream, boolean createDynView,
    		String winDynStorageDir, String unixDynStorageDir, AbstractBuild build, 
    		boolean freezeCode, boolean recreateView) {
        super();
        this.cleartool = cleartool;
        this.stream = stream;
        this.createDynView = createDynView;
        this.winDynStorageDir = winDynStorageDir;
        this.unixDynStorageDir = unixDynStorageDir;
        this.build = build;
        this.freezeCode = freezeCode;
        this.recreateView = recreateView;
    }

    public boolean checkout(Launcher launcher, FilePath workspace, String viewName) throws IOException, InterruptedException {        
        if (createDynView) {
        	if (freezeCode) {
        		checkoutCodeFreeze(viewName);
        	}
        	else {
            cleartool.mountVobs();
        		recreateView(viewName);
                cleartool.startView(viewName);
                cleartool.syncronizeViewWithStream(viewName, stream);        		
        	}
        		
        }
        else {
            cleartool.startView(viewName);
            cleartool.syncronizeViewWithStream(viewName, stream);        	
        }
        
        // add stream to data action (to be used by ClearCase report)
        ClearCaseDataAction dataAction = build.getAction(ClearCaseDataAction.class);
        if (dataAction != null)
        	dataAction.setStream(stream);     
        
        return true;
    }
    
    public boolean checkoutCodeFreeze(String viewName) throws IOException, InterruptedException {
    	
        // prepare stream and views
        prepareBuildStreamAndViews(viewName, stream);    
        
        // make baselines
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy_HH_mm_ss", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateStr = formatter.format(build.getTimestamp().getTime()).toLowerCase(); 
        
        UcmCommon.makeBaseline(cleartool.getLauncher(), true, getConfiguredStreamViewName(), null, 
        		BASELINE_NAME + dateStr, 
				BASELINE_COMMENT + dateStr, 
				false, false, null);
        
        // get latest baselines on the configured stream
        List<UcmCommon.BaselineDesc> latestBlsOnConfgiuredStream = UcmCommon.getLatestBlsWithCompOnStream(cleartool.getLauncher(), 
        		stream, getConfiguredStreamViewName());
        
        // rebase build stream6
        UcmCommon.rebase(cleartool.getLauncher(), viewName, latestBlsOnConfgiuredStream);
    	
        // add baselines to build - to be later used by getChange 

        ClearCaseDataAction dataAction = build.getAction(ClearCaseDataAction.class);
        if (dataAction != null)
        	dataAction.setLatestBlsOnConfgiuredStream(latestBlsOnConfgiuredStream);       
        
    	return true;
    }
    
	
	private void prepareBuildStreamAndViews(String viewName, String stream) throws IOException, InterruptedException {
		// mount vobs
		cleartool.mountVobs();
		
		// verify that view exists on the configured stream and start it
		String uuid = cleartool.getViewUuid(getConfiguredStreamViewName());
        if (uuid.equals("")) {
        	String dynStorageDir = cleartool.getLauncher().getLauncher().isUnix() ? unixDynStorageDir : winDynStorageDir;
        	cleartool.mkview(getConfiguredStreamViewName(), stream, dynStorageDir);
        }	

        cleartool.startView(getConfiguredStreamViewName());
        
		// do we have build stream? if not create it
        if (! UcmCommon.isStreamExists(cleartool.getLauncher(), getBuildStreamName(stream)))
        	UcmCommon.mkstream(cleartool.getLauncher(), stream, getBuildStreamName(stream));
        
        // create view on build stream
        recreateView(viewName);
      
	}
	
	private void recreateView(String viewName) throws IOException, InterruptedException {
            String uuid = cleartool.getViewUuid(viewName);
            // If we don't find a UUID, then the view tag must not exist, in which case we don't
            // have to delete it anyway.
        if (!uuid.equals("") && recreateView) {
        	try {
        		cleartool.rmviewUuid(uuid);	
        	}
        	catch (Exception ex) {
        		cleartool.logRedundantCleartoolError(null, ex);
        	}
        	
        	try {
        		cleartool.unregisterView(uuid);	
        	}
        	catch (Exception ex) {
        		cleartool.logRedundantCleartoolError(null, ex);
        	}            
        }
        
        // try to remove the view tag in any case. might help in overcoming corrupted views
        if (recreateView) {
            try {
                cleartool.rmviewtag(viewName);
    		} catch (Exception ex) {
    			cleartool.logRedundantCleartoolError(null, ex);
    		}        	
            }
        
            // Now, make the view.
        if (uuid.equals("") || recreateView) {
            String dynStorageDir = cleartool.getLauncher().getLauncher().isUnix() ? unixDynStorageDir : winDynStorageDir; 
            cleartool.mkview(viewName, getBuildStreamName(stream), dynStorageDir);        	
        }
	}
        
	public static String getConfiguredStreamViewName(String stream) {		
		return UcmCommon.getNoVob(stream) + CONFIGURED_STREAM_VIEW_SUFFIX;	
	}	
	
	private String getConfiguredStreamViewName() {		
		return getConfiguredStreamViewName(stream);	
    }
	
	private String getBuildStreamName(String stream) {
		return BUILD_STREAM_PREFIX + stream;
	}


}
