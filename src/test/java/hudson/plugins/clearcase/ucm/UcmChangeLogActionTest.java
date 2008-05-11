package hudson.plugins.clearcase.ucm;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.List;

import hudson.plugins.clearcase.ClearTool;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class UcmChangeLogActionTest {

    private Mockery context;
    private ClearTool cleartool;

    @Before
    public void setUp() throws Exception {
        context = new Mockery();
        cleartool = context.mock(ClearTool.class);
    }
    
    @Test
    public void assertParsingOfCommandOutput() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory("FORMAT", null, "IGNORED", "Usi_Release_2_1_int", "vobs/projects/Usi_Server");                
                will(returnValue(new StringReader(
                        "\"20080427.134932\" " +
                        "\"vobs/projects/Usi_Server/adp-war/src/main/java/net/apmoller/crb/usi/adp/AdpServlet.java\" " +
                        "\"/main/Usi_Main_Int/Usi_Release_2_1_int/2\" " +
                        "\"deliver.shared_development_2_1.20080427.154853\" " +
                        "\"create version\" " +
                        "\"checkin\" ")));
                one(cleartool).lsactivity(
                        with(equal("deliver.shared_development_2_1.20080427.154853@/vobs/UCM_project")), 
                        with(aNonNull(String.class)));
                will(returnValue(new StringReader("\"deliver.shared_development_2_1.20080427.154853\" " +
                		"\"deliver shared_development_2_1 on 04/27/08 15:48:53.\" " +
                		"\"hlyh_test_ccplugin_servlet hlyh_test_exception_ccplug\" " +
                		"\"user\" " +
                		"\"user name\" ")));
            }
        });
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool);
        List<UcmActivity> activities = action.getChanges(null, "IGNORED", new String[]{"Usi_Release_2_1_int"}, "vobs/projects/Usi_Server");
        assertEquals("There should be 2 activities", 2, activities.size());
        UcmActivity activity = activities.get(0);
        assertEquals("Activity name is incorrect", "deliver.shared_development_2_1.20080427.154853", activity.getName());
        assertEquals("Activity headline is incorrect", "", activity.getHeadline());
        assertEquals("Activity stream is incorrect", "", activity.getStream());
        assertEquals("Activity user is incorrect", "hlh005", activity.getUser());
    }
}
