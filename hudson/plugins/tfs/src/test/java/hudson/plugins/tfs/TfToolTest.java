package hudson.plugins.tfs;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

public class TfToolTest {
    private FilePath workspace;
    @Mock private Launcher launcher;
    @Mock private Proc proc;
    @Mock private TaskListener taskListener;
    
    private TfTool tool;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        workspace = Util.createTempFilePath();
        tool = new TfTool("tf", launcher, taskListener, workspace);
    }
    
    @After
    public void teardown() throws Exception {
        workspace.deleteRecursive();
    }

    @Test(expected=AbortException.class)
    public void assertUnexpectReturnCodeThrowsAbortException() throws Exception {
        stub(proc.join()).toReturn(1);
        stub(launcher.launch(isA(String[].class), isA(String[].class), (InputStream) isNull(), isA(OutputStream.class), isA(FilePath.class))).toReturn(proc);

        tool.execute(new String[]{"history"});
    }

    @Test
    public void assertExecutableReturnsWithReader() throws Exception {
        stub(launcher.launch(isA(String[].class), isA(String[].class), (InputStream) isNull(), isA(OutputStream.class), isA(FilePath.class))).toReturn(proc);

        Reader reader = tool.execute(new String[]{"history"});
        assertNotNull("Reader should not be null", reader);
        
        verify(launcher).launch(AdditionalMatchers.aryEq(new String[]{"tf", "history"}), (String[])anyObject(), (InputStream)anyObject(), (OutputStream)anyObject(), (FilePath)anyObject());
    }
}
