/**
 * 
 */
package hudson.plugins.starteam;

import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.starbase.starteam.Folder;

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
	
	public StarTeamPollingActor(String hostname, int port, String user,
			String passwd, String projectname, String viewname,
			Map<String,String> folderMap, String labelname, boolean promotionstate,
      TaskListener listener) {
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

      StarTeamChangeSet changeSet = connection.computeChangeSet(rootFolderMap, workspace,null,listener.getLogger());

      return changeSet.hasChanges();

    } catch (StarTeamSCMException e) {
      listener.getLogger().println(e.getLocalizedMessage());
      return false;
    } finally {
      connection.close();
    }

  }

}
