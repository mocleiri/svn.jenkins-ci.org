package hudson.plugins.clearcase.ucm;

import static org.junit.Assert.*;
import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearToolHistoryParser;
import hudson.plugins.clearcase.ucm.UcmActivity.File;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

public class UcmChangeLogParserTest {

    @Test
    public void assertParseOutput() throws Exception {
        UcmChangeLogParser parser = new UcmChangeLogParser();
        UcmChangeLogSet logSet = parser.parse(null, UcmChangeLogParserTest.class.getResourceAsStream("UcmChangeLog.xml"));
        assertEquals("The log set should only contain 1 entry", 1, logSet.getItems().length);
        
        UcmActivity activity = logSet.getLogs().get(0);
        assertEquals("Activity name is incorrect", "name", activity.getName());
        assertEquals("Activity headline is incorrect", "headline", activity.getHeadline());
        assertEquals("Activity stream is incorrect", "stream", activity.getStream());
        assertEquals("Activity view is incorrect", "view", activity.getView());
        assertEquals("Activity user is incorrect", "user", activity.getUser());
        
        assertEquals("Activity should contain one file", 1, activity.getFiles().size());
        File file = activity.getFiles().get(0);
        assertEquals("File name is incorrect", "file-name", file.getName());
        assertEquals("File comment is incorrect", "file-comment", file.getComment());
        assertEquals("File event is incorrect", "file-event", file.getEvent());
        assertEquals("File operation is incorrect", "file-operation", file.getOperation());
        assertEquals("File version is incorrect", "file-version", file.getVersion());
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(2008, 4, 8, 12, 20, 30);
        assertEquals("File date is incorrect", cal.getTime(), file.getDate());
    }
}
