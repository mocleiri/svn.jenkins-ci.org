package hudson.plugins.clearcase;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class ClearCaseUcmSCMTest {

    @Test
    public void testCreateChangeLogParser() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", "option");
        assertNotNull("The change log parser is null", scm.createChangeLogParser());
        assertNotSame("The change log parser is re-used", scm.createChangeLogParser(), scm.createChangeLogParser());
    }
    
    @Test
    public void testGetLoadRules() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", "option");
        assertEquals("The load rules arent correct", "loadrules", scm.getLoadRules());
    }

    @Test
    public void testGetStream() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", "option");
        assertEquals("The stream isnt correct", "stream", scm.getStream());
    }

    @Test
    public void testGetBranchNames() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", "option");
        assertArrayEquals("The branch name array is incorrect", new String[]{"stream"}, scm.getBranchNames());
    }

    /**
     * The stream cant be used as a branch name directly if it contains a vob selector.
     * cleartool lshistory -r <snip/> -branch brtype:shared_development_2_1@/vobs/UCM_project 
     * cleartool: Error: Object is in unexpected VOB: "brtype:shared_development_2_1@/vobs/UCM_project".
     */
    @Test
    public void testGetBranchNamesWithVobSelector() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream@/vob/paths", "loadrules", "viewname", "option");
        assertArrayEquals("The branch name array is incorrect", new String[]{"stream"}, scm.getBranchNames());
    }

    @Test
    public void testGetViewPaths() throws Exception {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", "option");
        assertEquals("The view path is not the same as the load rules", "loadrules", scm.getViewPaths(null)[0]);
    }
    
    @Test
    public void assertLoadRuleIsConvertedToRelativeViewPath() throws Exception {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "\\\\loadrule\\one\n/loadrule/two", "viewname", "option");
        assertEquals("The first view path is not correct", "loadrule\\one", scm.getViewPaths(null)[0]);
        assertEquals("The second view path is not correct", "loadrule/two", scm.getViewPaths(null)[1]);
    }

    @Test 
    public void testShortenStreamName() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream:mystream", "file with space\nanotherfile", "viewname", "option");
        assertEquals("stream name not shortenen correctly", "mystream",scm.getStream());
    }
}
