package hudson.tasks.junit;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

/**
 * Test cases for parsing JUnit report XML files.
 * As there are no XML schema for JUnit xml files, Hudson needs to handle
 * varied xml files.
 * 
 * @author Erik Ramfelt
 */
public class SuiteResultTest extends TestCase {

    /**
     * Verifying fix for issue #1233.
     * https://hudson.dev.java.net/issues/show_bug.cgi?id=1233
     */
    public void testIssue1233() throws Exception {
        File file = new File(SuiteResultTest.class.getResource("junit-report-1233.xml").toURI());
        SuiteResult result = new SuiteResult(file);
        
        List<CaseResult> cases = result.getCases();
        assertEquals("Class name is incorrect", "test.foo.bar.DefaultIntegrationTest", cases.get(0).getClassName());
        assertEquals("Class name is incorrect", "test.foo.bar.BundleResolverIntegrationTest", cases.get(1).getClassName());
        assertEquals("Class name is incorrect", "test.foo.bar.BundleResolverIntegrationTest", cases.get(2).getClassName());
        assertEquals("Class name is incorrect", "test.foo.bar.ProjectSettingsTest", cases.get(3).getClassName());
        assertEquals("Class name is incorrect", "test.foo.bar.ProjectSettingsTest", cases.get(4).getClassName());
    }
    
    /**
     * Verifying fix for issue #1463.
     * JUnit report file is generated by SoapUI Pro 1.7.6
     * https://hudson.dev.java.net/issues/show_bug.cgi?id=1463
     */
    public void testIssue1463() throws Exception {
        File file = new File(SuiteResultTest.class.getResource("junit-report-1463.xml").toURI());
        SuiteResult result = new SuiteResult(file);

        List<CaseResult> cases = result.getCases();
        for (CaseResult caseResult : cases) {
            assertEquals("Test class name is incorrect in " + caseResult.getDisplayName(), "WLI-FI-Tests-Fake", caseResult.getClassName());            
        }
        assertEquals("Test name is incorrect", "IF_importTradeConfirmationToDwh", cases.get(0).getName());
        assertEquals("Test name is incorrect", "IF_getAmartaDisbursements", cases.get(1).getName());
        assertEquals("Test name is incorrect", "IF_importGLReconDataToDwh", cases.get(2).getName());
        assertEquals("Test name is incorrect", "IF_importTradeInstructionsToDwh", cases.get(3).getName());
        assertEquals("Test name is incorrect", "IF_getDeviationTradeInstructions", cases.get(4).getName());
        assertEquals("Test name is incorrect", "IF_getDwhGLData", cases.get(5).getName());
    }
}
