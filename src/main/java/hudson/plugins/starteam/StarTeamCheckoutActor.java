package hudson.plugins.starteam;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.remoting.VirtualChannel;

import java.io.*;
import java.util.*;

import com.starbase.starteam.Folder;

/**
 * A helper class for transparent checkout operations over the network. Can be
 * used with FilePath.act() to do a checkout on a remote node.
 * 
 * @author Ilkka Laukkanen <ilkka.s.laukkanen@gmail.com>
 */
class StarTeamCheckoutActor implements FileCallable<Boolean> {

	private final BuildListener listener;

	private String hostname;

	private int port;

	private String user;

	private String passwd;

	private String projectname;

	private String viewname;

	private Map folderMap;

  private String labelname;

  private boolean promotionstate;

  private File buildFolder;

  private File changelogFile;

  private AbstractBuild build;

  public StarTeamCheckoutActor(String hostname, int port,
                               String user, String passwd,
                               String projectname, String viewname, Map folderMap,
                               String labelname, boolean promotionstate,
                               File buildFolder, BuildListener listener, File changelogFile,
                               AbstractBuild build) {
		this.hostname = hostname;
		this.port = port;
		this.user = user;
		this.passwd = passwd;
		this.projectname = projectname;
		this.viewname = viewname;
		this.folderMap = folderMap;
		this.listener = listener;
    this.labelname = labelname;
    this.promotionstate = promotionstate;
    this.buildFolder = buildFolder;
    this.changelogFile = changelogFile;
    this.build = build;
  }

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.FilePath.FileCallable#invoke(java.io.File,
	 *      hudson.remoting.VirtualChannel)
	 */
	public Boolean invoke(File workspace, VirtualChannel channel)
			throws IOException {

    listener.getLogger().print("Connecting to starteam ");

    StarTeamConnection connection = new StarTeamConnection(
				hostname, port,
        user, passwd,
				projectname, viewname, folderMap,
        labelname, promotionstate);

    try {

      try {
        connection.initialize();
      } catch (StarTeamSCMException e) {
        listener.getLogger().println(e.getLocalizedMessage());
        return false;
      }

      listener.getLogger().println("[done]");

      Map<String, Folder> rootFolderMap = connection.getRootFolder();

      listener.getLogger().print("Computing change set ");

      java.io.File filePointFile = null;
      if (build.getPreviousBuild()!=null && new File(build.getPreviousBuild().getRootDir(),StarTeamConnection.FILE_POINT_FILENAME).exists()) {
        filePointFile = new File(build.getPreviousBuild().getRootDir(),StarTeamConnection.FILE_POINT_FILENAME);
      }
      //return createEmptyChangeLog(changelogFile, listener, "log");

      // compute changes
      StarTeamChangeSet changeSet = connection.computeChangeSet(rootFolderMap, workspace, filePointFile, listener.getLogger());

      listener.getLogger().println("[done]");

      // perform checkout
      connection.checkOut(changeSet, listener.getLogger(),buildFolder);

      listener.getLogger().print("Writing change log ");

      // write out the change log
			createChangeLog(changelogFile, changeSet);

      listener.getLogger().println("[done]");

      return true;
    } catch (StarTeamSCMException e) {
      listener.getLogger().println("[problem] "+e.getLocalizedMessage());
      return false;
    } finally {
      connection.close();
    }

  }

	protected boolean createChangeLog(File changelogFile, StarTeamChangeSet changeSet) throws IOException {
	  OutputStream os = new BufferedOutputStream(new FileOutputStream(changelogFile));
    try {
      return StarTeamChangeLogBuilder.writeChangeLog(os, changeSet);
    } finally {
      os.close();
    }
  }

}
