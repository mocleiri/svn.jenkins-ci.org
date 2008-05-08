package hudson.plugins.clearcase.action;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolExecTest;
import hudson.plugins.clearcase.ClearToolHistoryParser;
import hudson.plugins.clearcase.StreamCopyAction;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Calendar;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
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
    public void assertSuccessfulParse() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(null, "view", "branch", "vobpath");                
                will(returnValue(new InputStreamReader( ClearToolExecTest.class.getResourceAsStream("ct-lshistory-1.log"))));
            }
        });
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        List<ClearCaseChangeLogEntry> changes = action.getChanges(null, "view", "branch", "vobpath");
        assertEquals("The history should contain 3 items", 3, changes.size());
    }
}
