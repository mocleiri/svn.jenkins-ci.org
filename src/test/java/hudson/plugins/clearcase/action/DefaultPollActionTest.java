package hudson.plugins.clearcase.action;

import static org.junit.Assert.*;
import hudson.plugins.clearcase.ClearTool;

import java.io.StringReader;
import java.util.Date;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class DefaultPollActionTest {

    private Mockery context;
    private ClearTool cleartool;

    @Before
    public void setUp() throws Exception {
        context = new Mockery();
        cleartool = context.mock(ClearTool.class);
    }

    @Test
    public void assertSeparateBranchCommands() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branchone")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader("")));
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branchtwo")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader("\"20071015.151822\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\" \"mkelem\" ")));
            }
        });
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        boolean hasChange = action.getChanges(null, "view", new String[]{"branchone", "branchtwo"}, new String[]{"vobpath"});
        assertTrue("The getChanges() method did not report a change", hasChange);
    }

    @Test
    public void assertSuccessfulParse() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader(
                        "\"20071015.151822\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" "
                      + "\"20071015.151822\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\"  \"mkelem\" ")));
            }
        });
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        boolean hasChange = action.getChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertTrue("The getChanges() method did not report a change", hasChange);
    }

    @Test
    public void assertIgnoringErrors() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader("cleartool: Error: Not an object in a vob: \"view.dat\".\n")));
            }
        });
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        boolean hasChange = action.getChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertFalse("The getChanges() method reported a change", hasChange);
    }

    @Test
    public void assertIgnoringVersionZero() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader("\"20071015.151822\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\0\" \"create version\"  \"mkelem\" ")));
            }
        });
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        boolean hasChange = action.getChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertFalse("The getChanges() method reported a change", hasChange);
    }
}
