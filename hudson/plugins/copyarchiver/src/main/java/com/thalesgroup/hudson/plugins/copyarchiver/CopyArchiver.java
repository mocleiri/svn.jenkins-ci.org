/*******************************************************************************
* Copyright (c) 2009 Thales Corporate Services SAS                             *
* Author : Gregory Boissinot                                                   *
*                                                                              *
* Permission is hereby granted, free of charge, to any person obtaining a copy *
* of this software and associated documentation files (the "Software"), to deal*
* in the Software without restriction, including without limitation the rights *
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
* copies of the Software, and to permit persons to whom the Software is        *
* furnished to do so, subject to the following conditions:                     *
*                                                                              *
* The above copyright notice and this permission notice shall be included in   *
* all copies or substantial portions of the Software.                          *
*                                                                              *
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
* THE SOFTWARE.                                                                *
*******************************************************************************/

package com.thalesgroup.hudson.plugins.copyarchiver;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.Result;
import hudson.tasks.Publisher;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.StaplerRequest;

/**
 * 
 * @author Gregory Boissinot
 */
public class CopyArchiver extends Publisher implements Serializable{

	private static final long serialVersionUID = 1L;

	public static final CopyArchiverDescriptor DESCRIPTOR = new CopyArchiverDescriptor();
		
	private String sharedDirectoryPath;
	
	private boolean useTimestamp;
	
	private String datePattern;
	
    private final List<ArchivedJobEntry> archivedJobList   = new ArrayList<ArchivedJobEntry>();	
	    
    public String getSharedDirectoryPath() {
		return sharedDirectoryPath;
	}

	public void setSharedDirectoryPath(String sharedDirectoryPath) {
		this.sharedDirectoryPath = sharedDirectoryPath;
	}

	public boolean getUseTimestamp() {
		return useTimestamp;
	}

	public void setUseTimestamp(boolean useTimestamp) {
		this.useTimestamp = useTimestamp;
	}	

	public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

	public List<ArchivedJobEntry> getArchivedJobList() {
		return archivedJobList;
	}    
        
	public String getDatePattern() {
		return datePattern;
	}

	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
	}
		
    public static final class CopyArchiverDescriptor extends Descriptor<Publisher>{

    	//CopyOnWriteList
    	private List<AbstractProject> jobs;
    	
        public CopyArchiverDescriptor() {
            super(CopyArchiver.class);
        }

        @Override
        public String getDisplayName() {
            return "Aggregate the archived artifacts";
        }

        @Override
        public Publisher newInstance(StaplerRequest req) throws FormException {
            CopyArchiver pub = new CopyArchiver();
            req.bindParameters(pub, "copyarchiver.");
            pub.getArchivedJobList().addAll(req.bindParametersToList(ArchivedJobEntry.class, "copyarchiver.entry."));
            return pub;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/copyarchiver/help.html";
        }
        
        
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        	return true;	
        }

		public List<AbstractProject> getJobs() {
			 return Hudson.getInstance().getItems(AbstractProject.class);
		}      
                      
    }


    // Deletes all files and subdirectories under dir.
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
    
        // The directory is now empty so delete it
        return dir.delete();
    } 
    
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
    	
    	try{
    		
    		if (build.getResult().equals(Result.UNSTABLE) || build.getResult().equals(Result.SUCCESS)){
    	    		
    			listener.getLogger().println("Starting copy archived artifacts in the shared directory.");
    			
    			File destDir = null;
    			Map vars = new HashMap();
    			if (useTimestamp){
    				
    				if (datePattern==null || datePattern.trim().isEmpty()){
    					build.setResult(Result.FAILURE);
    					throw new AbortException("The option 'Change the date format' is activated. You must provide a new date pattern.");    					
    				}
    				
    		        SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
    		        final String newBuildIdStr = sdf.format(build.getTimestamp().getTime());         			
        			vars.put("BUILD_ID", newBuildIdStr);
    			}

    			String sharedDirectoryPathParsed = Util.replaceMacro(sharedDirectoryPath,  vars);    				
    			
    			destDir = new File(sharedDirectoryPathParsed);
    			listener.getLogger().println("Copying archived artifacts in the shared directory '" + destDir + "'.");    		    			
    			deleteDir(destDir);    			
    			destDir.mkdirs();
    			FilePath destDirFilePath = new FilePath(destDir);
    			
    			for (ArchivedJobEntry archivedJobEntry:archivedJobList){    				
    				File lastSuccessfulDir = Project.findNearest(archivedJobEntry.jobName).getLastSuccessfulBuild().getArtifactsDir();
    				FilePath lastSuccessfulDirFilePath = new FilePath(lastSuccessfulDir);    				
    				lastSuccessfulDirFilePath.copyRecursiveTo(archivedJobEntry.pattern, destDirFilePath);    				
    			}
    			
    			listener.getLogger().println("Stop copying archived artifacts in the shared directory.");    
    		}    
        } 
    	catch (Exception e) {
            e.printStackTrace(listener.fatalError("error"));
            build.setResult(Result.FAILURE);
            return true;
        }        
            
		return true;
	}
  
}
