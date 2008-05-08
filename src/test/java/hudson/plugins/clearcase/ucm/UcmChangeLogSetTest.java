package hudson.plugins.clearcase.ucm;

import static org.junit.Assert.*;
import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearCaseChangeLogSet;
import hudson.plugins.clearcase.ClearCaseChangeLogEntry.FileElement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

public class UcmChangeLogSetTest {
    
    @Test
    public void assertSavedLogSetCanBeParsed() throws Exception {
        
        UcmActivity activity = new UcmActivity();
        activity.setHeadline("headline");
        activity.setName("name");
        activity.setStream("stream");
        activity.setUser("user");
        activity.setView("view");
        
        UcmActivity.File activityFile = new UcmActivity.File();
        activityFile.setComment("file-comment");
        activityFile.setVersion("version1");
        activityFile.setName("file1");
        activityFile.setOperation("file-operation");
        activityFile.setEvent("file-event");
        activity.addFile(activityFile);

        File tempLogFile = File.createTempFile("clearcase", "xml");
        tempLogFile.deleteOnExit();
        FileOutputStream fileOutputStream = new FileOutputStream(tempLogFile);
        
        List<UcmActivity> activities = new ArrayList<UcmActivity>();
        activities.add(activity);
        UcmChangeLogSet.saveToChangeLog(fileOutputStream, activities);
        fileOutputStream.close();

        FileInputStream fileInputStream = new FileInputStream(tempLogFile);
        UcmChangeLogParser parser = new UcmChangeLogParser();
        UcmChangeLogSet logSet = parser.parse(null, fileInputStream);
        fileInputStream.close();
        
        List<UcmActivity> logs = logSet.getLogs();
        Assert.assertEquals("The number of activities is incorrect", 1, logs.size());
        Assert.assertEquals("The number of files in the first activity is incorrect", 1, logs.get(0).getFiles().size());
        Assert.assertEquals("The first file name is incorrect", "file1", logs.get(0).getFiles().get(0).getName());
        Assert.assertEquals("The first version is incorrect", "version1", logs.get(0).getFiles().get(0).getVersion());
    }
}
