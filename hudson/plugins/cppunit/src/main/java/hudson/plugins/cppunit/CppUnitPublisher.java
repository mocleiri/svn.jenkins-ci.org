package hudson.plugins.cppunit;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.plugins.cppunit.util.Messages;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResultProjectAction;
import hudson.util.FormFieldValidator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;

import javax.servlet.ServletException;
import javax.xml.transform.TransformerException;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Class that records CppUnit test reports into Hudson.
 * 
 * @author Gregory Boissinot
 * 20090323 Correction of the java.io.NotSerializableException with a slave  
 *   
 */
public class CppUnitPublisher extends hudson.tasks.Publisher implements Serializable {

    private static final long serialVersionUID = 1L;

    @Extension
    public static final CppUnitDescriptor DESCRIPTOR = new CppUnitDescriptor();

    private String testResultsPattern;
    
    private boolean useCustomStylesheet;
           
    private String customStylesheet;


    @DataBoundConstructor
    public CppUnitPublisher(String testResultsPattern, boolean useCustomStylesheet, String customStylesheet) {
        this.testResultsPattern = testResultsPattern;
        this.useCustomStylesheet=useCustomStylesheet;
        this.customStylesheet=customStylesheet;
    }

    public String getTestResultsPattern() {
        return testResultsPattern;
    }

    public boolean getUseCustomStylesheet() {
		return useCustomStylesheet;
	}
    
	public String getCustomStylesheet() {
		return customStylesheet;
	}

	@Override
    public Action getProjectAction(hudson.model.Project project) {
         return new TestResultProjectAction(project);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
    	
        boolean result=false;
        PrintStream logger = listener.getLogger();     
        try {
        	   	
            Messages.log(logger,"Recording CppUnit tests results.");
                        
            //Build the transformer
            CppUnitTransformer cppUnitTransformer;            
            if (useCustomStylesheet && customStylesheet!=null){
            	Messages.log(logger,"Use the specified stylesheet.");
            	FilePath customStylesheetFilePath = build.getParent().getModuleRoot().child(customStylesheet);
            	cppUnitTransformer = new CppUnitTransformer(customStylesheetFilePath);	
            }
            else{
            	Messages.log(logger,"Use the default CppUnit plugin stylesheet.");
            	cppUnitTransformer = new CppUnitTransformer();
            }
            
            
    		//Create the temporary target junit dir
    		File parentTargetJunitFile = Util.createTempDir();
    		FilePath junitTargetFilePath = new FilePath(parentTargetJunitFile);
            if (junitTargetFilePath.exists()) {
            	junitTargetFilePath.deleteRecursive();
            }
            junitTargetFilePath.mkdirs();            
            
            //Compute module roots
            final FilePath[] moduleRoots= build.getProject().getModuleRoots();
            final boolean multipleModuleRoots= moduleRoots != null && moduleRoots.length > 1;
            final FilePath moduleRoot= multipleModuleRoots ? build.getProject().getWorkspace() : build.getProject().getModuleRoot();
            
            // Archiving CppunitFile into Junit files
            CppUnitArchiver archiver = new CppUnitArchiver(logger, junitTargetFilePath, testResultsPattern, cppUnitTransformer);
            result = moduleRoot.act(archiver);
            if (!result) {
            	build.setResult(Result.FAILURE);
            } else {
            	 result = recordTestResult(build, listener, junitTargetFilePath, "TEST-*.xml");
            }
            
            //Detroy temporary target junit dir           
            junitTargetFilePath.deleteRecursive();
            
        } 
        catch (TransformerException te) {
        	Messages.log(logger,"Error publishing cppunit results" + te.toString());
        	build.setResult(Result.FAILURE);            
        }

         
        Messages.log(logger,"End recording CppUnit tests results.");        
        return result;
    }

 

    @Override
    public CppUnitDescriptor getDescriptor() {
        return DESCRIPTOR;        
    }
    
    private static FilePath getWorkspace (AbstractProject p) {
        try {
            return p.getWorkspace();
        } catch (NullPointerException e) {
            return null;
        }
    }
 

    public static final class CppUnitDescriptor extends BuildStepDescriptor<Publisher> {

        public CppUnitDescriptor() {
            super(CppUnitPublisher.class);          
        }

        @Override
        public String getDisplayName() {
            return "Publish CppUnit test result report";
        }
        
        @Override
        public boolean isApplicable(Class type) {
            return true;
        }        

        @Override
        public String getHelpFile() {
            return "/plugin/cppunit/help.html";
        }

        @Override
        public Publisher newInstance(StaplerRequest req) throws FormException {
        	return new CppUnitPublisher( req.getParameter("cppunit_reports.pattern"), 
            							(req.getParameter("cppunit_reports.useCustomStylesheet")!=null),
            							req.getParameter("cppunit_reports.customStylesheet"));
            
        }
        
        
        
        /**
         * Checks if the custom stylesheet location is valid
         */
        public void doCheckValidCustomStylesheetLocation( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
            new FormFieldValidator(req, rsp, true) {
                public void check() throws IOException, ServletException {
                    
                	String value = Util.fixEmptyAndTrim(request.getParameter("value"));
                	if (value!=null){
                	  		
                		String job = Util.fixEmptyAndTrim(request.getParameter("job"));                	
                		FilePath workspace = CppUnitPublisher.getWorkspace(Hudson.getInstance().getItemByFullName(job, AbstractProject.class));
                		File f =null;
                		try{
                			f = new File(new FilePath(workspace,value).toURI());
                		}
                		catch (InterruptedException ie){
                			error(value+" is not a valid file.");
                			return;                		
                		}
                		
                		if (!f.exists()) {
                			error(value+" is not a valid file.");
                			return;
                		}
                	}
                	else {
                		error(" The stylesheet directory is mandatory.");
                	}
                	
                    ok();
                }
            }.process();
        }
    }
    
    /**
     * Record the test results into the current build.
     * @param junitFilePattern
     * @param build
     * @param listener
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private boolean recordTestResult(final AbstractBuild<?,?> build, 
    								 final BuildListener listener, 
    								 final FilePath junitTargetFilePath, 
    								 final String junitFilePattern)
            throws InterruptedException, IOException {
    	
    	
        TestResultAction existingAction = build.getAction(TestResultAction.class);
        TestResultAction action;

        try {
            final long buildTime = build.getTimestamp().getTimeInMillis();

            TestResult existingTestResults = null;
            if (existingAction != null) {
                existingTestResults = existingAction.getResult();
            }
            TestResult result = getTestResult(junitTargetFilePath, junitFilePattern, build, existingTestResults, buildTime);

            if (existingAction == null) {
                action = new TestResultAction(build, result, listener);
            } else {
                action = existingAction;
                action.setResult(result, listener);
            }
            
            if(result.getPassCount()==0 && result.getFailCount()==0){
            	throw new AbortException("None of the test reports contained any result");
            }
                
        } catch (AbortException e) {
            if(build.getResult()==Result.FAILURE)
                // most likely a build failed before it gets to the test phase.
                // don't report confusing error message.
                return true;

            listener.getLogger().println(e.getMessage());
            build.setResult(Result.FAILURE);
            return true;
        }

        if (existingAction == null) {
        	build.getActions().add(action);
        }

        if(action.getResult().getFailCount()>0)
        	build.setResult(Result.UNSTABLE);

        return true;
    }

    /**
     * Collect the test results from the files
     * @param junitFilePattern
     * @param build
     * @param existingTestResults existing test results to add results to
     * @param buildTime
     * @return a test result
     * @throws IOException
     * @throws InterruptedException
     */
    private TestResult getTestResult(final FilePath temporaryJunitFilePath, 
    								 final String junitFilePattern,
    								 final AbstractBuild<?, ?> build,
    								 final TestResult existingTestResults, 
    								 final long buildTime) 
    				throws IOException, InterruptedException {
    	
    	
    	final File temporaryJunitDirFile = new File(temporaryJunitFilePath.toURI());
    	
        TestResult result = build.getProject().getWorkspace().act(new FileCallable<TestResult>() {
            public TestResult invoke(File ws, VirtualChannel channel) throws IOException {
            	
                FileSet fs = Util.createFileSet(temporaryJunitDirFile, junitFilePattern);
                DirectoryScanner ds = fs.getDirectoryScanner();
                String[] files = ds.getIncludedFiles();
                if(files.length==0) {
                    // no test result. Most likely a configuration error or fatal problem
                    throw new AbortException("No test report files were found. Configuration error?");
                }
                if (existingTestResults == null) {
                    return new TestResult(buildTime, ds);
                } else {
                    existingTestResults.parse(buildTime, ds);
                    return existingTestResults;
                }
            }
        });
        return result;
    }        
}
