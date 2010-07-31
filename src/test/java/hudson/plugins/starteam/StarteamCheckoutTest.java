package hudson.plugins.starteam;

import hudson.plugins.starteam.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.starbase.starteam.Folder;
import org.apache.commons.io.FileUtils;

public class StarteamCheckoutTest {

  public static void main(String[] args) throws IOException, StarTeamSCMException {

    String historyCsv = "d:/temp/build-1/history.csv";  // the checkout history
    String buildName = "D:/temp/build-2";               // the new build folder where metadata is stored
    String workspaceName = "D:/temp/workspace";         // the workspace folder

    String username = "timothy";
    String password = "example";
    String server = "mystarteam";
    int port = 49201;
    String repository = "My Repository";
    String view = "My Repository";
    String label = "Test";
    boolean promotion = false;

    // ----------------------------------------

    String historyContent = FileUtils.readFileToString(new File(historyCsv));

    Map folderMap = StarTeamFunctions.splitCsvString(historyContent);

    StarTeamConnection conn = new StarTeamConnection(server,port,username,password,repository,view,folderMap,label,promotion);

    conn.initialize();

    File workspace = new File(workspaceName);
    File buildFolder = new File(buildName);
    Map<String, Folder> rootFolder = conn.getRootFolder();

    StarTeamChangeSet changeSet = conn.computeChangeSet(rootFolder,workspace,buildFolder,System.out);

    conn.checkOut(changeSet,System.out,buildFolder);

    conn.close();
  }

}
