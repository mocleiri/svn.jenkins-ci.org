/**
 *
 */
package hudson.plugins.starteam;

import com.starbase.starteam.Folder;
import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author ip90568
 *
 */
public class StarTeamPollingActor implements FileCallable<Boolean> {

	private String hostname;

	private int port;

	private String user;

	private String passwd;

	private String projectname;

	private String viewname;

	private Map<String,String> folderMap;

  private String labelname;

  private boolean promotionstate;

  private final TaskListener listener;

  private AbstractBuild lastBuild;

  public StarTeamPollingActor(String hostname, int port, String user,
			String passwd, String projectname, String viewname,
			Map<String,String> folderMap, String labelname, boolean promotionstate,
      TaskListener listener, AbstractBuild lastBuild ) {
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
    this.lastBuild = lastBuild;
  }

	/*
	 * (non-Javadoc)
	 *
	 * @see hudson.FilePath.FileCallable#invoke(java.io.File,
	 *      hudson.remoting.VirtualChannel)
	 */
	public Boolean invoke(File workspace, VirtualChannel channel) throws IOException {

    StarTeamConnection connection = new StarTeamConnection(
				hostname, port, user, passwd,
				projectname, viewname, folderMap, labelname, promotionstate);

    try {

      try {
        connection.initialize();
      } catch (StarTeamSCMException e) {
        listener.getLogger().println(e.getLocalizedMessage());
        return false;
      }

      Map<String, Folder> rootFolderMap = connection.getRootFolder();

      java.io.File filePointFile = null;
      if (lastBuild != null && new File(lastBuild.getRootDir(), StarTeamConnection.FILE_POINT_FILENAME).exists() ) {
        filePointFile = new File(lastBuild.getRootDir(),StarTeamConnection.FILE_POINT_FILENAME);
      }

      StarTeamChangeSet changeSet = connection.computeChangeSet(rootFolderMap, workspace,filePointFile,listener.getLogger());

      return changeSet.hasChanges();

    } catch (StarTeamSCMException e) {
      listener.getLogger().println(e.getLocalizedMessage());
      return false;
    } finally {
      connection.close();
    }

  }

}
