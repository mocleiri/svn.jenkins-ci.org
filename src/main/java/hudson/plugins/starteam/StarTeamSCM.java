package hudson.plugins.starteam;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * StarTeam SCM plugin for Hudson.
 *
 * @author Ilkka Laukkanen <ilkka.s.laukkanen@gmail.com>
 */
public class StarTeamSCM extends SCM {

	/**
	 * Singleton descriptor.
	 */
	public static final StarTeamSCMDescriptorImpl DESCRIPTOR = new StarTeamSCMDescriptorImpl();

	private final String user;
	private final String passwd;
	private final String projectname;
	private final String viewname;
	private String foldername;
	private final String hostname;
  private String labelname;
  private boolean promotionstate;
  private final int port;

  // do not persist the following, they are transient
  /**
   * All folders (single or multiple) end up in the following folder map
   * as key value pairs where the key is the starteam folder name and the value is the relative path.
   * . is used if none is provided.
   */
  private transient Map<String,String> folderMap;
  private transient String name;
  private transient boolean namePromotionState;


  /**
	 * The constructor.
	 *
	 * @stapler-constructor
	 */
	public StarTeamSCM(String hostname, int port,
                     String projectname, String viewname, String foldername,
                     String username, String password,
                     String labelname, String promotionstate) {
		this.hostname = hostname;
		this.port = port;
		this.projectname = projectname;
		this.viewname = viewname;
		this.foldername = StringUtils.trimToNull(foldername);
		this.user = username;
		this.passwd = password;

    this.labelname = StringUtils.trimToNull(labelname);
    this.promotionstate = StringUtils.trimToNull(promotionstate) != null ? true : false;
  }

  protected void initialise() {
    this.foldername = StringUtils.trimToNull(this.foldername);
    this.labelname = StringUtils.trimToNull(this.labelname);

    this.folderMap = new HashMap<String,String>();

    this.name = labelname;
    this.namePromotionState = promotionstate;

    if (foldername != null) {
      folderMap.putAll(StarTeamFunctions.splitCsvString(foldername));
    }
  }

  /*
	 * @see hudson.scm.SCM#checkout(hudson.model.AbstractBuild, hudson.Launcher,
	 *      hudson.FilePath, hudson.model.BuildListener, java.io.File)
	 */
  @Override
  public boolean checkout(AbstractBuild build, Launcher launcher,
		  FilePath workspace, BuildListener listener, File changelogFile)
  throws IOException, InterruptedException {

	  initialise();

	  listener.getLogger().println("Checking Out (host=["+hostname+"],port=["+port+"],projectname=["+projectname+"],viewname=["+viewname+"],user=["+user+"],password=<******>,labelname=["+labelname+"],promotionstate=["+promotionstate+"])");

	  boolean status;

	  // Create an actor to do the checkout, possibly on a remote machine
	  StarTeamCheckoutActor co_actor = new StarTeamCheckoutActor(
			  hostname, port, user, passwd, projectname, viewname,
			  folderMap, name, namePromotionState,
			  build.getRootDir(),
			  listener, changelogFile, build);

	  if (workspace.act(co_actor)) {
		  status = true;
	  } else {
		  listener.getLogger().println("StarTeam checkout failed");
		  status = false;
	  }
	  return status;
  }

  /*
	 * (non-Javadoc)
	 *
	 * @see hudson.scm.SCM#createChangeLogParser()
	 */
	@Override
	public ChangeLogParser createChangeLogParser() {
		return new StarTeamChangeLogParser();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see hudson.scm.SCM#getDescriptor()
	 */
	@Override
	public StarTeamSCMDescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see hudson.scm.SCM#pollChanges(hudson.model.AbstractProject,
	 *      hudson.Launcher, hudson.FilePath, hudson.model.TaskListener)
	 */
	@Override
	public boolean pollChanges(final AbstractProject proj,
			final Launcher launcher, final FilePath workspace,
			final TaskListener listener) throws IOException,
			InterruptedException {

		initialise();

		boolean status = false;

		// Create an actor to do the polling, possibly on a remote machine
		StarTeamPollingActor p_actor = new StarTeamPollingActor(hostname, port,
				user, passwd, projectname, viewname, folderMap, name, namePromotionState,
				listener, (AbstractBuild)proj.getLastBuild());

		if (workspace.act(p_actor)) {
			status = true;
		} else {
			listener.getLogger().println("StarTeam polling failed");
		}
		return status;
	}

	/**
	 * Descriptor class for the SCM class.
	 *
	 * @author Ilkka Laukkanen <ilkka.s.laukkanen@gmail.com>
	 *
	 */
	public static final class StarTeamSCMDescriptorImpl extends
			SCMDescriptor<StarTeamSCM> {

		private final Collection<StarTeamSCM> scms = new ArrayList<StarTeamSCM>();

		protected StarTeamSCMDescriptorImpl() {
			super(StarTeamSCM.class, null);
		}

		@Override
		public String getDisplayName() {
			return "StarTeam";
		}

		@Override
		public SCM newInstance(StaplerRequest req, JSONObject formData)
				throws hudson.model.Descriptor.FormException {
			// Go ahead and create the scm.. the bindParameters() method
			// takes the request and nabs all "starteam." -prefixed
			// parameters from it, then sets the scm instance's fields
			// according to those parameters.
			StarTeamSCM scm = null;
			try {
				scm = req.bindParameters(StarTeamSCM.class, "starteam.");
				scms.add(scm);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			// We don't have working repo browsers yet...
			// scm.repositoryBrowser = RepositoryBrowsers.createInstance(
			// StarTeamRepositoryBrowser.class, req, "starteam.browser");
			return scm;
		}

	}

	/**
	 * Get the hostname this SCM is using.
	 *
	 * @return The hostname.
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * Get the port number this SCM is using.
	 *
	 * @return The port.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Get the project name this SCM is connected to.
	 *
	 * @return The project's name.
	 */
	public String getProjectname() {
		return projectname;
	}

	/**
	 * Get the view name in the project this SCM uses.
	 *
	 * @return The name of the view.
	 */
	public String getViewname() {
		return viewname;
	}

	/**
	 * Get the root folder name of our monitored workspace.
	 *
	 * @return The name of the folder.
	 */
	public String getFoldername() {
		return foldername;
	}

	/**
	 * Get the username used to connect to starteam.
	 *
	 * @return The username.
	 */
	public String getUsername() {
		return user;
	}

	/**
	 * Get the password used to connect to starteam.
	 *
	 * @return The password.
	 */
	public String getPassword() {
		return passwd;
	}

  /**
   * Get the label used to check out from starteam.
   *
   * @return The label.
   */
  public String getLabelname() {
    return labelname;
  }

  /**
   * Is the label a promotion state name?
   *
   * @return True if the label name is actually a promotion state.
   */
  public boolean isPromotionstate() {
    return promotionstate;
  }
}
