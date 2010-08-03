/**
 *
 */
package hudson.plugins.starteam;

import java.io.*;
import java.util.*;

import com.starbase.starteam.File;
import com.starbase.starteam.Folder;
import com.starbase.starteam.Item;
import com.starbase.starteam.LogonException;
import com.starbase.starteam.Label;
import com.starbase.starteam.Project;
import com.starbase.starteam.PropertyNames;
import com.starbase.starteam.PromotionState;
import com.starbase.starteam.Server;
import com.starbase.starteam.ServerConfiguration;
import com.starbase.starteam.ServerInfo;
import com.starbase.starteam.Status;
import com.starbase.starteam.View;
import com.starbase.starteam.ViewConfiguration;
import com.starbase.util.OLEDate;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * StarTeamActor is a class that implements connecting to a StarTeam repository,
 * to a given project, view and folder.
 *
 * @author Ilkka Laukkanen <ilkka.s.laukkanen@gmail.com>
 */
public class StarTeamConnection {

  public static final String FILE_POINT_FILENAME = "starteam-filepoints.csv";

  private final ServerInfo serverinfo;

  private final String username;

  private final String password;

  private final String projectname;

  private final String viewname;

  private final Map<String, String> folderMap;

  private Server server = null;

  private View baseView = null;

  private View effectiveView = null;

  private Map<String, Folder> rootFolderMap = new HashMap<String, Folder>();

  private Project project = null;

  private String labelname;         // the name of the label

  private boolean promotionstate;   // is the label a promotion state name?

  private Integer labelId;          // the label ID

  private Integer promotionStateId; // the promotion state ID

  public StarTeamConnection(String hostname, int port, String user,
                            String passwd, String projectname, String viewname,
                            String foldername, String labelname, boolean promotionstate) {

    this(hostname, port, user, passwd, projectname, viewname, new HashMap<String, String>(), labelname, promotionstate);

    folderMap.put(foldername, ".");
  }

  public StarTeamConnection(String hostname, int port, String user,
                            String passwd, String projectname, String viewname,
                            Map<String, String> folderMap, String labelname, boolean promotionstate) {
    // Create the server information object
    serverinfo = new ServerInfo();
    serverinfo.setConnectionType(ServerConfiguration.PROTOCOL_TCP_IP_SOCKETS);

    // Do a trick in case there are several hosts with the same
    // description: if an exception is thrown, start adding a counter
    // to the end of the string.
    String desc_base = "StarTeam connection to " + hostname;
    String desc = desc_base;
    do {
      int ctr = 1;
      try {
        serverinfo.setDescription(desc);
        break;
      } catch (com.starbase.starteam.DuplicateServerListEntryException e) {
        desc = desc_base + " (" + Integer.toString(ctr++) + ")";
      }
    } while (true);

    serverinfo.setHost(hostname);
    serverinfo.setPort(port);

    this.username = user;
    this.password = passwd;
    this.projectname = projectname;
    this.viewname = viewname;
    this.folderMap = folderMap;
    this.labelname = trimToNull(labelname);
    this.promotionstate = promotionstate;
  }

  private static String trimToNull(String value) {
      String v = value != null ? value.trim() : null;
      return v;
  }

  /**
   * Initialize the connection. This means logging on to the server and
   * finding the project, view and folders we want.
   *
   * @throws StarTeamSCMException if logging on fails.
   */
  public void initialize() throws StarTeamSCMException {
    server = new Server(serverinfo);
    server.connect();
    try {
      server.logOn(username, password);
    } catch (LogonException e) {
      throw new StarTeamSCMException("Could not log on: "+ e.getErrorMessage());
    }

    project = findProjectOnServer(server, projectname);
    baseView = findViewInProject(project, viewname);

    if (labelname != null) {
      if (promotionstate) {
        // note: If the promotion state is assigned to <<current>> then the resulting ID will be NULL and
        // we will revert to a view based on the current timestamp.
        promotionStateId = findPromotionStateInView(baseView, labelname);
      } else {
        labelId = findLabelInView(baseView, labelname);
      }
    }

    effectiveView = createView(baseView, null, labelId, promotionStateId);

    for (Map.Entry<String, String> folder : folderMap.entrySet()) {
      Folder rootFolder = StarTeamFunctions.findFolderInView(effectiveView, folder.getKey());

      // Cache some folder data
      final PropertyNames pnames = rootFolder.getPropertyNames();
      final String[] propsToCache = new String[]{
          pnames.FILE_LOCAL_FILE_EXISTS, pnames.FILE_LOCAL_TIMESTAMP,
          pnames.FILE_NAME, pnames.FILE_FILE_TIME_AT_CHECKIN,
          pnames.MODIFIED_TIME, pnames.MODIFIED_USER_ID,
          pnames.FILE_STATUS};
      rootFolder.populateNow(server.getTypeNames().FILE, propsToCache, -1);
      rootFolder.setAlternatePathFragment(folder.getValue());
      rootFolderMap.put(folder.getKey(), rootFolder);
    }
  }

  public Map<String, Folder> getRootFolder() {
    return rootFolderMap;
  }

  static Project findProjectOnServer(final Server server, final String projectname) throws StarTeamSCMException {
    for (Project project : server.getProjects()) {
      if (project.getName().equals(projectname)) {
        return project;
      }
    }
    throw new StarTeamSCMException("Couldn't find project [" + projectname + "] on server [" + server.getAddress()+"]");
  }

  static View findViewInProject(final Project project, final String viewname) throws StarTeamSCMException {
    for (View view : project.getAccessibleViews()) {
      if (view.getName().equals(viewname)) {
        return view;
      }
    }
    throw new StarTeamSCMException("Couldn't find view [" + viewname + "] in project " + project.getName());
  }

  static int findLabelInView(final View view, final String labelname) throws StarTeamSCMException {
    for (Label label : view.getLabels()) {
      if (labelname.equals(label.getName())) {
        return label.getID();
      }
    }
    throw new StarTeamSCMException("Couldn't find label [" + labelname + "] in view " + view.getName());
  }

  static Integer findPromotionStateInView(final View view, final String promotionState) throws StarTeamSCMException {
    for (PromotionState ps : view.getPromotionModel().getPromotionStates()) {
      if (promotionState.equals(ps.getName())) {
        if (ps.getLabelID() == -1) {
          // PROMOTION STATE is set to <<current>>
          return null;
        }
        return ps.getObjectID();
      }
    }
    throw new StarTeamSCMException("Couldn't find promotion state " + promotionState + " in view " + view.getName());
  }

  static View createView(View view, Date effectiveDate, Integer labelId, Integer promotionStateId) throws StarTeamSCMException {
    final ViewConfiguration configuration;

    if (promotionStateId != null) {
      configuration = ViewConfiguration.createFromPromotionState(promotionStateId);
    } else if (labelId != null) {
      configuration = ViewConfiguration.createFromLabel(labelId);
    } else if (effectiveDate != null) {
      configuration = ViewConfiguration.createFromTime(new OLEDate(effectiveDate));
    } else if (view != null) {
        ViewConfiguration result = null;
        try {
            result = ViewConfiguration.createFromTime(view.getServer().getCurrentTime());
        } catch (UnsupportedOperationException unsupported) {
            // We cannot fetch the current server time from various versions of starteam so we "guess" by subtracting 10 seconds.
            result = ViewConfiguration.createFromTime(new OLEDate(System.currentTimeMillis()-5000));
        }
        configuration = result;
    } else {
      throw new StarTeamSCMException("Could not construct view - no configuration provided");
    }

    return new View(view, configuration);
  }

  /**
   * Close the connection.
   */
  public void close() {
    if (rootFolderMap != null) {
      for (Map.Entry<String, Folder> e : rootFolderMap.entrySet()) {
        e.getValue().discardItems(e.getValue().getTypeNames().FILE, -1);
      }
      rootFolderMap.clear();
    }
    if (effectiveView != null) {
      effectiveView.discard();
    }
    if (baseView != null) {
      baseView.discard();
    }
    if (project != null) {
      project.discard();
    }
    if (server != null) {
      server.disconnect();
    }
  }

  private final Object syncLock = new Object();

  public void checkOut(StarTeamChangeSet changeSet, PrintStream logger, java.io.File buildFolder) throws IOException {
    logger.println("*** Performing checkout on [" + changeSet.getFilesToCheckout().size() + "] files");
    boolean quietCheckout = changeSet.getFilesToCheckout().size() >= 2000;
    if (quietCheckout) {
      logger.println("*** More than 2000 files, quiet mode enabled");
    }
    if (!changeSet.isComparisonAvailable()) {
      changeSet.setDirty(new TreeSet<StarTeamFilePoint>());
    }
    int amount = 0;
    for (File f : changeSet.getFilesToCheckout()) {
      amount += 1;
      boolean actioned = checkOutFile(f, changeSet, logger, quietCheckout);
      f.discard();

      if (actioned && !quietCheckout) logger.println("ok");
      if (amount % 100 == 0) {
        logger.println("[checkout] [quite mode] " + amount + " files checked out.");
      }
    }
    logger.println("*** removing [" + changeSet.getFilesToRemove().size() + "] files");
    boolean quietDelete = changeSet.getFilesToRemove().size() > 100;
    if (quietDelete) {
      logger.println("*** More than 100 files, quiet mode enabled");
    }
    for (java.io.File f : changeSet.getFilesToRemove()) {
      if (f.exists()) {
        if (!quietDelete) logger.println("[remove] [" + f + "]");
        f.delete();
      } else {
        logger.println("[remove:warn] Planned to remove [" + f + "]");
      }
    }
    logger.println("*** storing change set");
    StarTeamFilePointFunctions.storeCollection(new java.io.File(buildFolder, FILE_POINT_FILENAME), changeSet.getFilePointsToRemember());
    logger.println("*** done");
  }

  private boolean checkOutFile(File f, StarTeamChangeSet changeSet, PrintStream logger, boolean quiet) throws IOException {

    // we may support parallel checkout and do not want to fail when creating folders
    java.io.File dir = new java.io.File(f.getFullName()).getParentFile();
    synchronized (syncLock) {
      if (!dir.exists()) {
        if (!quiet) logger.println("[mkdir] [" + dir.getPath() + "]");
        dir.mkdirs();
      }
    }

    boolean dirty = !changeSet.isComparisonAvailable();

    switch (f.getStatus()) {
      case Status.MERGE:
      case Status.MODIFIED:
      case Status.UNKNOWN:
        dirty = false;
        // clobber these
        new java.io.File(f.getFullName()).delete();
        break;
      case Status.MISSING:
        dirty = false;
      case Status.OUTOFDATE:
        // just go on an check out
        break;
      default:
        // By default do nothing, go to next iteration
        return false;
    }
    if (!quiet) logger.print("[checkout] [" + f.getFullName() + "] ");
    try {

      if (filterAllow(f)) {

          f.checkout(Item.LockType.UNCHANGED, // check out as unlocked
              false, // use timestamp from repo
              true, // convert EOL to native format
              true); // update status
      }
    } catch (IOException e) {
      logger.print("[checkout] [exception] [Problem checking out file: "+f.getFullName()+"] \n"+ExceptionUtils.getFullStackTrace(e)+"\n");
      throw e;
    } catch (RuntimeException e) {
      logger.print("[checkout] [exception] [Problem checking out file: "+f.getFullName()+"] \n"+ExceptionUtils.getFullStackTrace(e)+"\n");
      throw e;
    }

    if (dirty) {
      changeSet.getDirty().add(new StarTeamFilePoint(f));
    }

    return true;

  }

  private static Set<String> TWO = new HashSet<String>();
  private static Set<String> ONE = new HashSet<String>();
  static  {
    TWO.add("toplink");

    ONE.add("table");
    ONE.add("package");
    ONE.add("descriptor");
    ONE.add("database");
    ONE.add("classRepository");
    ONE.add("class");
  }

  public boolean filterAllow(File f) {
    String fn = f.getFullName().toLowerCase();

    if (fn.endsWith(".xml")) {
      java.io.File parent = new java.io.File(fn).getParentFile();
      if (parent != null) {
        if (ONE.contains(parent.getName())) {
          parent = parent.getParentFile();
          if (parent != null) {
            if (TWO.contains(parent.getName())) {
              return false;
            }
          }
        }
      }
    }

    return true;
  }

  public static void main(String[] args) throws Throwable {
      String hostname = "dhstarteam01";
      int port = 49201;
      String user = "MMSBUILD";
      String password = "******";
      String project = "Health Systems (MMD)";
      String view = "Health Systems (MMD)";
      String folder = "Health Systems (MMD)/HUM/AuthorisationObjectModel";
      String label = null;
      boolean promotionstate = false;

      StarTeamConnection stc = new StarTeamConnection(hostname,port,user,password,project,view,folder,label,promotionstate);
      stc.initialize();
      System.out.println("Yoohaaa");
      stc.close();
  }

  /**
 * @param rootFolderMap Map of all project directories
 * @param workspace a workspace directory
 * @param filePointFile a file containing previous File Point description
 * @param logger a logger for consuming log messages
 * @return set of changes  
 * @throws StarTeamSCMException
 * @throws IOException
 */
public StarTeamChangeSet computeChangeSet(Map<String,Folder> rootFolderMap, java.io.File workspace, java.io.File filePointFile, PrintStream logger) throws StarTeamSCMException, IOException {

    // --- compute changes as per starteam

    final Collection<com.starbase.starteam.File> starteamFiles = StarTeamFunctions.listAllFiles(rootFolderMap, workspace);
    final Map<java.io.File, com.starbase.starteam.File> starteamFileMap = StarTeamFilePointFunctions.convertToFileMap(starteamFiles);
    final Collection<java.io.File> starteamFileSet = starteamFileMap.keySet();
    final Collection<StarTeamFilePoint> starteamFilePoint = StarTeamFilePointFunctions.convertFilePointCollection(starteamFiles);

    final Collection<java.io.File> fileSystemFiles = StarTeamFilePointFunctions.listAllFiles(workspace);
    final Collection<java.io.File> fileSystemRemove = new TreeSet<java.io.File>(fileSystemFiles);
    fileSystemRemove.removeAll(starteamFileSet);

    final StarTeamChangeSet changeSet = new StarTeamChangeSet();
    changeSet.setFilesToCheckout(starteamFiles);
    changeSet.setFilesToRemove(fileSystemRemove);
    changeSet.setFilePointsToRemember(starteamFilePoint);

    // --- compute differences as per historic storage file

    if (filePointFile != null && filePointFile.exists() && filePointFile.isFile()) {

      try {

        //java.io.File starteamFilePoints = new java.io.File(fromDir, FILE_POINT_FILENAME);

        final Collection<StarTeamFilePoint> historicStarteamFilePoint = StarTeamFilePointFunctions.loadCollection(filePointFile);

        changeSet.setComparisonAvailable(true);

        StarTeamFilePointFunctions.computeDifference(starteamFilePoint, historicStarteamFilePoint, changeSet);

      } catch (Throwable t) {
        t.printStackTrace(logger);
      }
    }

    return changeSet;
  }

  public void populateDescription(ServerInfo serverInfoMock) {

  }

}
