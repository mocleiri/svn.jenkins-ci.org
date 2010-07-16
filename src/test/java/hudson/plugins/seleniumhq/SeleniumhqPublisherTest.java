package hudson.plugins.seleniumhq;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.SingleFileSCM;

public class SeleniumhqPublisherTest extends HudsonTestCase
{
	/**
	 * Test du constructeur
	 */
	public void testSeleniumhqPublisher()
	{
		SeleniumhqPublisher publisher = new SeleniumhqPublisher("*.html", false);
		assertTrue(publisher instanceof Publisher);
	}
	
	/**
	 * Test de la m�thode getTestResults
	 */
	public void testGetTestResults()
	{
		SeleniumhqPublisher publisher = new SeleniumhqPublisher("*.html", false);
		assertEquals("*.html", publisher.getTestResults());
	}
	
	/**
	 * Test de la m�thode getDescriptor
	 */
	public void testGetDescriptor()
	{
		SeleniumhqPublisher publisher = new SeleniumhqPublisher("*.html", false);
		Descriptor<Publisher> descriptor =  publisher.getDescriptor();
		assertTrue(descriptor instanceof SeleniumhqPublisher.DescriptorImpl);
		assertEquals("/plugin/seleniumhq/help-publisher.html", descriptor.getHelpFile());
		assertEquals("Publish Selenium Report", descriptor.getDisplayName());
	}
	
	/**
	 * Test de la m�thode getProjectAction
	 */
	public void testGetProjectAction() throws IOException
	{
		SeleniumhqPublisher publisher = new SeleniumhqPublisher("*.html", false);
		assertTrue(publisher.getProjectAction((AbstractProject)createFreeStyleProject()) instanceof SeleniumhqProjectAction);
	}
	
	/**
	 * Test du Publisher avec aucun fichier source
	 * @throws Exception
	 */
	public void test1() throws Exception 
	{
        FreeStyleProject project = createFreeStyleProject();
        project.getPublishersList().add(new SeleniumhqPublisher("*.html", false));
              
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertEquals(Result.FAILURE, build.getResult());

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("Publishing Selenium report..."));
        assertTrue(s.contains("No Test Report Found"));
    }

	/**
	 * Test du Publisher avec 1 fichier source invalide
	 * @throws Exception
	 */
	public void test2() throws Exception 
	{
        FreeStyleProject project = createFreeStyleProject();
        project.getPublishersList().add(new SeleniumhqPublisher("*.html", false));
        
        project.setScm(new SingleFileSCM("badResult.html", getClass().getResource("badResult.html")));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertEquals(Result.FAILURE, build.getResult());

        String s = FileUtils.readFileToString(build.getLogFile());

        assertTrue(s.contains("Staging badResult.html"));
        assertTrue(s.contains("Publishing Selenium report..."));
        assertTrue(s.contains("ERROR: Failed to archive Selenium reports"));
    }
	
	/**
	 * Test du Publisher avec 1 fichier source invalide vide
	 * @throws Exception
	 */
	public void test3() throws Exception 
	{
        FreeStyleProject project = createFreeStyleProject();
        project.getPublishersList().add(new SeleniumhqPublisher("*.html", false));
        
        project.setScm(new SingleFileSCM("emptyResult.html", getClass().getResource("emptyResult.html")));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertEquals(Result.FAILURE, build.getResult());

        String s = FileUtils.readFileToString(build.getLogFile());

        assertTrue(s.contains("Staging emptyResult.html"));
        assertTrue(s.contains("Publishing Selenium report..."));
        assertTrue(s.contains("ERROR: Failed to archive Selenium reports"));
    }
	
	/**
	 * Test du Publisher avec 1 fichier valide
	 * @throws Exception
	 */
	public void test4() throws Exception 
	{		
        FreeStyleProject project = createFreeStyleProject();
        project.getPublishersList().add(new SeleniumhqPublisher("*.html", false));
        
        project.setScm(new SingleFileSCM("testResult.html", getClass().getResource("testResult.html")));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertEquals(Result.SUCCESS, build.getResult());

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("Test failures: 0"));
        assertTrue(s.contains("Test totals  : 7"));          
    }
	
	/**
	 * Test du Publisher avec 1 fichier valide avec failures et numCommandErrors > 0
	 * @throws Exception
	 */
	public void test5() throws Exception 
	{		
        FreeStyleProject project = createFreeStyleProject();
        project.getPublishersList().add(new SeleniumhqPublisher("*.html", false));
        
        project.setScm(new SingleFileSCM("testResultWithFailure.html", getClass().getResource("testResultWithFailure.html")));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertEquals(Result.UNSTABLE, build.getResult());

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("Test failures: 1"));
        assertTrue(s.contains("Test totals  : 3"));   
        assertTrue(s.contains("Command Passes   : 37"));    
        assertTrue(s.contains("Command Failures : 5"));    
        assertTrue(s.contains("Command Errors   : 1")); 
    }
	
	/**
     * Test du Publisher avec 1 fichier valide avec failures et numCommandErrors > 0
     * @throws Exception
     */
    public void test5_2() throws Exception 
    {       
        FreeStyleProject project = createFreeStyleProject();
        project.getPublishersList().add(new SeleniumhqPublisher("*.html", true));
        
        project.setScm(new SingleFileSCM("testResultWithFailure.html", getClass().getResource("testResultWithFailure.html")));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertEquals(Result.FAILURE, build.getResult());

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("Test failures: 1"));
        assertTrue(s.contains("Test totals  : 3"));   
        assertTrue(s.contains("Command Passes   : 37"));    
        assertTrue(s.contains("Command Failures : 5"));    
        assertTrue(s.contains("Command Errors   : 1")); 
    }
	
	/**
	 * Test du Publisher avec 2 fichier valide
	 * @throws Exception
	 */
	public void test6() throws Exception 
	{		
        FreeStyleProject project = createFreeStyleProject();
        project.getPublishersList().add(new SeleniumhqPublisher("*.html", false));
        
        List<SingleFileSCM> files = new ArrayList<SingleFileSCM>(2);
        files.add(new SingleFileSCM("testResult1.html", getClass().getResource("testResult.html")));
        files.add(new SingleFileSCM("testResult2.html", getClass().getResource("testResult.html")));
        project.setScm(new MultiFileSCM(files));     
        
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertEquals(Result.SUCCESS, build.getResult());

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("Test failures: 0"));
        assertTrue(s.contains("Test totals  : 14"));       
    }
	
	/**
	 * Test du Publisher avec 3 fichier 2 valide et 1 avec failure
	 * @throws Exception
	 */
	public void test7() throws Exception 
	{		
        FreeStyleProject project = createFreeStyleProject();
        project.getPublishersList().add(new SeleniumhqPublisher("*.html", false));
        
        List<SingleFileSCM> files = new ArrayList<SingleFileSCM>(2);
        files.add(new SingleFileSCM("testResult1.html", getClass().getResource("testResult.html")));
        files.add(new SingleFileSCM("testResult2.html", getClass().getResource("testResult.html")));
        files.add(new SingleFileSCM("testResult3.html", getClass().getResource("testResultWithFailure.html")));
        
        project.setScm(new MultiFileSCM(files));     
        
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertEquals(Result.UNSTABLE, build.getResult());

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("Test failures: 1"));
        assertTrue(s.contains("Test totals  : 17"));                  
    }
	
	/**
     * Test du Publisher avec 3 fichier 2 valide et 1 avec failure
     * @throws Exception
     */
    public void test7_2() throws Exception 
    {       
        FreeStyleProject project = createFreeStyleProject();
        project.getPublishersList().add(new SeleniumhqPublisher("*.html", true));
        
        List<SingleFileSCM> files = new ArrayList<SingleFileSCM>(2);
        files.add(new SingleFileSCM("testResult1.html", getClass().getResource("testResult.html")));
        files.add(new SingleFileSCM("testResult2.html", getClass().getResource("testResult.html")));
        files.add(new SingleFileSCM("testResult3.html", getClass().getResource("testResultWithFailure.html")));
        
        project.setScm(new MultiFileSCM(files));     
        
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertEquals(Result.FAILURE, build.getResult());

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("Test failures: 1"));
        assertTrue(s.contains("Test totals  : 17"));                  
    }
	
	/**
	 * Test du Publisher avec 1 fichier valide avec 0 test
	 * @throws Exception
	 */
	public void test8() throws Exception 
	{
        FreeStyleProject project = createFreeStyleProject();
        project.getPublishersList().add(new SeleniumhqPublisher("*.html", false));
        
        project.setScm(new SingleFileSCM("testResultNoTest.html", getClass().getResource("testResultNoTest.html")));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertEquals(Result.FAILURE, build.getResult());

        String s = FileUtils.readFileToString(build.getLogFile());

        assertTrue(s.contains("Staging testResultNoTest.html"));
        assertTrue(s.contains("Publishing Selenium report..."));
        assertTrue(s.contains("ERROR: Result does not have test"));
    }
	
	/**
     * Test du Publisher avec 1 fichier valide avec failures et numCommandErrors = 0
     * @throws Exception
     */
    public void test9() throws Exception 
    {       
        FreeStyleProject project = createFreeStyleProject();
        project.getPublishersList().add(new SeleniumhqPublisher("*.html", false));
        
        project.setScm(new SingleFileSCM("testResultWithFailureNoError.html", getClass().getResource("testResultWithFailureNoError.html")));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertEquals(Result.UNSTABLE, build.getResult());

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("Test failures: 1"));
        assertTrue(s.contains("Test totals  : 3"));          
        assertTrue(s.contains("Command Passes   : 37"));    
        assertTrue(s.contains("Command Failures : 5"));    
        assertTrue(s.contains("Command Errors   : 0"));   
    }
	
}
