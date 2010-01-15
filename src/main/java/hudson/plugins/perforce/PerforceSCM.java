package hudson.plugins.perforce;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import static hudson.Util.fixNull;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.FormFieldValidator;
import hudson.util.FormValidation;

import java.util.logging.Level;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.perforce.p4java.server.IServer;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.ILabel;
import hudson.PluginWrapper;

/**
 * Extends {@link SCM} to provide integration with Perforce SCM repositories.
 *
 * @author Mike Wille
 * @author Brian Westrich
 * @author Victor Szoltysek
 * @author Carl Quinn
 */
public class PerforceSCM extends SCM {

    String p4User;
    String p4Passwd;
    String p4Port;
    String p4Client;
    String projectPath;
    String p4Label;
    String projectOptions = "noallwrite clobber compress unlocked nomodtime rmdir";

    // Keep these fields around a while for compatibility with old serialized data.
    transient String p4Exe;
    transient String p4SysDrive;
    transient String p4SysRoot;
    transient IServer server;  // default connected server avail for the life of this SCM

    PerforceRepositoryBrowser browser;

    /**
     * This is being removed, including it as transient to fix exceptions on startup.
     */
    transient int lastChange;
    /**
     * force sync is a one time trigger from the config area to force a sync with the depot.
     * it is reset to false after the first checkout.
     */
    boolean forceSync = false;
    /**
     * If true, we will manage the workspace view within the plugin.  If false, we will leave the
     * view alone.
     */
    boolean updateView = true;
    /**
     * If false we add the slave hostname to the end of the client name when
     * running on a slave.  Defaulting to true so as not to change the behavior
     * for existing users.
     */
    boolean dontRenameClient = true;

    /**
     * If > 0, then will override the changelist we sync to for the first build.
     */
    int firstChange = -1;

    @DataBoundConstructor
    public PerforceSCM(String p4User, String p4Passwd, String p4Client, String p4Port, String projectPath, String projectOptions,
                       String p4Exe, String p4SysRoot, String p4SysDrive, String p4Label, boolean forceSync,
                       boolean updateView, boolean dontRenameClient, int firstChange, PerforceRepositoryBrowser browser) {

        this.p4User = p4User;
        this.setP4Passwd(p4Passwd);
        this.p4Client = p4Client;
        this.p4Port = p4Port;
        this.projectOptions = projectOptions;
        //make it backwards compatible with the old way of specifying a label
        Matcher m = Pattern.compile("(@\\S+)\\s*").matcher(projectPath);
        if (m.find()) {
            p4Label = m.group(1);
            projectPath = projectPath.substring(0,m.start(1))
                + projectPath.substring(m.end(1));
        }

        if (this.p4Label != null && p4Label != null) {
            Logger.getLogger(PerforceSCM.class.getName()).warning(
                    "Label found in views and in label field.  Using: "
                    + p4Label);
        }
        this.p4Label = Util.fixEmptyAndTrim(p4Label);

        this.projectPath = projectPath;

        if (p4Exe != null)
            this.p4Exe = p4Exe;

        if (p4SysRoot != null && p4SysRoot.length() != 0)
            this.p4SysRoot = p4SysRoot;

        if (p4SysDrive != null && p4SysDrive.length() != 0)
            this.p4SysDrive = p4SysDrive;

        this.forceSync = forceSync;
        this.browser = browser;
        this.updateView = updateView;
        this.dontRenameClient = dontRenameClient;
        this.firstChange = firstChange;
    }

    /**
     * Create, initialize, connect & login to the perforce server identified by a given port, user &
     * password.
     *
     * @return a ready-to-use IServer instance that should have disconnect()
     * called before dropping.
     */
    protected static IServer newServer(String port, String user, String pass)
            throws URISyntaxException, P4JavaException {
        PluginWrapper pw = Hudson.getInstance().getPluginManager().getPlugin("perforce");
        return P4jUtil.newServer(port, pw.getLongName(), pw.getVersion(), user, pass);
    }

    /**
     * Create, initialize, connect & login to the perforce server identified by our p4Xxx fields.
     *
     * @return a ready-to-use IServer instance that should have .logout() and .disconnect()
     * called before dropping.
     */
    protected IServer newServer() throws URISyntaxException, P4JavaException {
        PerforcePasswordEncryptor encryptor = new PerforcePasswordEncryptor();
        return newServer(p4Port, p4User, encryptor.decryptString(p4Passwd));
    }

    /**
     * Returns the re-usable Perforce server object managed by this SCM instance.
     */
    protected synchronized IServer getServer()  throws URISyntaxException, P4JavaException {
        if (server == null || !server.isConnected()) {
            server = newServer();
        }
        return server;
    }

    protected synchronized void disconnectServer() throws P4JavaException {
        disconnectServer(server);
        server = null;
    }

    protected synchronized void disconnectServer(IServer server) throws P4JavaException {
        if (server != null && server.isConnected()) {
            server.logout();
            server.disconnect();
        }
    }

    protected synchronized void disconnectServer(IServer server, PrintStream log) {
        try {
            disconnectServer(server);
            if(server != null && server.isConnected()){
                log.println("Perforce failed to disconnect!");
            }
        } catch (P4JavaException ex) {
            logServerException(ex, log);
        }
    }
    
    protected synchronized void disconnectServer(PrintStream log){
        try {
            disconnectServer(server);
            if(server != null && server.isConnected()){
                log.println("Perforce failed to disconnect!");
            }
        } catch (P4JavaException ex) {
            logServerException(ex, log);
        } finally {
            server = null;
        }
    }

    // TODO(CQ) shouldn't need this since getServer() now lazily inits
    /**
     * Depot is transient, so we need to create a new one on start up
     * specifically for the getDepot() method.
     *
    private void readObject(ObjectInputStream is) {
        try {
            is.defaultReadObject();
            server = createServer();
        } catch (IOException exception) {
            // DO nothing
        } catch (ClassNotFoundException exception) {
            // DO nothing
        }
    }*/

    /**
     * Returns a new tek42 Depot object to use when building helper objects that
     * will be serialized, such as PerforceTagAction. This Depot is not functional,
     * but merely serves to hold onto server login information
     */
    private PerforceDepot newDepot() {
        PerforceDepot depot = new PerforceDepot();
        depot.setPort(p4Port);
        depot.setUser(p4User);
        depot.setPassword(new PerforcePasswordEncryptor().decryptString(p4Passwd));
        depot.setClient(p4Client);
        return depot;
    }

    /**
     * Override of SCM.buildEnvVars() in order to setup some useful Perforce environment variables
     * to be available to the build subprocesses.
     * <ul>
     *  <li> the last change we have sync'd to: P4_CHANGELIST
     *  <li> or, the label we have sync'd to: P4_LABEL
     *  <li> the user: P4USER
     *  <li> the client: P4CLIENT
     *  <li> the server & port: P4PORT
     * </ul>
     */
    @Override
    public void buildEnvVars(AbstractBuild build, Map<String, String> env) {
        super.buildEnvVars(build, env);
        env.put("P4PORT", p4Port);
        env.put("P4USER", p4User);
        env.put("PCLIENT", p4Client);
        PerforceTagAction pta = getMostRecentTagAction(build);
        if (pta != null) {
            if (pta.getChangeNumber() > 0) {
                int lastChange = pta.getChangeNumber();
                env.put("P4_CHANGELIST", Integer.toString(lastChange));
            }
            else if (pta.getTag() != null) {
                String label = pta.getTag();
                env.put("P4_LABEL", label);
            }
        }
    }

    private static void logServerException(Exception e, PrintStream logger) {
        logger.println("Caught Exception communicating with Perforce: " + e.getMessage());
        e.printStackTrace(logger);
        logger.flush();
    }

    /*
     * TODO(CQ) consider: change checkout() so that it finds the list of new changes only if the
     * current and previous builds did NOT use a p4 label.
     */
    @Override
    public boolean checkout(AbstractBuild build, Launcher launcher,
            FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {

        PrintStream log = listener.getLogger();

        // keep projectPath local so any modifications for slaves don't get saved
        String projectPath = this.projectPath;

        try {
            IServer server = getServer();
            
            String clientName = getEffectiveClientName(build.getBuiltOn(), workspace, listener);
            IClient client = getPreparedClient(server, clientName, launcher, workspace, listener);

            // Get the list of changes since the last time we looked...
            final int lastChange = getLastChange((Run) build.getPreviousBuild());
            log.println("Last sync'd change: " + lastChange);

            List<IChangelist> changes;
            int newestChange = lastChange;
            if (p4Label != null) {
                changes = new ArrayList<IChangelist>(0);
            } else {
                newestChange = P4jUtil.latestChangeId(server);
                if (lastChange <= 0 || lastChange >= newestChange) {
                    changes = new ArrayList<IChangelist>(0);
                } else {
                    changes = P4jUtil.changesInRange(client, lastChange+1, newestChange);
                }
            }

            if (changes.size() > 0) {
                // Save the changes we discovered.
                PerforceChangeLogSet.saveToChangeLog(new FileOutputStream(changelogFile), changes);
                newestChange = changes.get(0).getId();
            }
            else {
                // No new changes discovered (though the definition of the workspace or label may have changed).
                createEmptyChangeLog(changelogFile, listener, "changelog");
            }

            // Now we can actually do the sync process...
            StringBuilder sbMessage = new StringBuilder("Sync'ing workspace to ");
            StringBuilder sbSyncPath = new StringBuilder("//" + client.getName() + "/...");
            sbSyncPath.append("@");

            if (p4Label != null) {
                sbMessage.append("label ");
                sbMessage.append(p4Label);
                sbSyncPath.append(p4Label);
            }
            else {
                sbMessage.append("changelist ");
                sbMessage.append(newestChange);
                sbSyncPath.append(newestChange);
            }

            if (forceSync)
                sbMessage.append(" (forcing sync of unchanged files).");
            else
                sbMessage.append(".");

            log.println(sbMessage.toString());
            String syncPath = sbSyncPath.toString();

            long startTime = System.currentTimeMillis();

            PerforcePasswordEncryptor encryptor = new PerforcePasswordEncryptor();
            workspace.act(new SyncTask(clientName, syncPath, forceSync, listener,
                    p4Port, p4User, encryptor.decryptString(p4Passwd)));

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.println("Sync complete, took " + duration + " ms");

            // reset one time use variables...
            forceSync = false;
            firstChange = -1;

            if (p4Label != null) {
                // Add tagging action that indicates that the build is already
                // tagged (you can't label a label).
                build.addAction(new PerforceTagAction(build, newDepot(), p4Label, projectPath));
            }
            else {
                // Add tagging action that enables the user to create a label
                // for this build.
                build.addAction(new PerforceTagAction(build, newDepot(), newestChange, projectPath));
            }

            // Save the one time use variables.
            build.getParent().save();
            return true;

        } catch (URISyntaxException e) {
            logServerException(e, log);
            //TODO(CQ) throw an IOException here too like in pollChanges()?
        } catch (P4JavaException e) {
            logServerException(e, log);
        } catch (InterruptedException e) {
            throw new IOException("Unable to get hostname from slave. " + e.getMessage());
        } finally {
            disconnectServer(log);
        }

        return false;
    }

    private static class SyncTask implements FileCallable<Boolean> {

        private final String clientName;
        private final String syncPath;
        private final boolean forceSync;
        private final BuildListener listener;
        private final String p4port;
        private final String p4user;
        private final String p4passwd;

        public SyncTask(String clientName, String syncPath, boolean forceSync, 
                BuildListener listener, String p4port,
                String p4user, String p4passwd) {
            this.clientName = clientName;
            this.syncPath = syncPath;
            this.forceSync = forceSync;
            this.listener = listener;
            this.p4port = p4port;
            this.p4passwd = p4passwd;
            this.p4user = p4user;
        }

        public Boolean invoke(File workspace, VirtualChannel virtualChannel) throws IOException {
            final PrintStream log = listener.getLogger();

            IServer server = null;
            try {
                // TODO: pass version information into the filecaller
                server = P4jUtil.newServer(p4port, "hudson", "sync", p4user, p4passwd);
                IClient client = server.getClient(clientName);
                server.setCurrentClient(client);
                P4jUtil.sync(client, syncPath, forceSync);
                // Logout and disconnect since this is a new connection
                server.logout();
                server.disconnect();
                return true;
            } catch (URISyntaxException ex) {
                logServerException(ex, log);
            } catch (P4JavaException ex) {
                logServerException(ex, log);
            } finally {
                try {
                    if (server != null && server.isConnected()){
                        server.logout();
                        server.disconnect();
                    }
                } catch (P4JavaException ex){
                    logServerException(ex, log);
                }
            }
            return false;
        }
    }
    
    @Override
    public PerforceRepositoryBrowser getBrowser() {
       return browser;
    }

    /*
     * @see hudson.scm.SCM#createChangeLogParser()
     */
    @Override
    public ChangeLogParser createChangeLogParser() {
        return new PerforceChangeLogParser();
    }

    /*
     * @see hudson.scm.SCM#pollChanges(hudson.model.AbstractProject, hudson.Launcher, hudson.FilePath, hudson.model.TaskListener)
     *
     * When *should* this method return true?
     *
     * 1) When there is no previous build (might be the first, or all previous
     *    builds might have been deleted).
     *
     * 2) When the previous build did not use Perforce, in which case we can't
     *    be "sure" of the state of the files.
     *
     * 3) If the clientspec's views have changed since the last build; we don't currently
     *    save that info, but we should!  I (James Synge) am not sure how to save it;
     *    should it be:
     *         a) in the build.xml file, and if so, how do we save it there?
     *         b) in the change log file (which actually makes a fair amount of sense)?
     *         c) in a separate file in the build directory (not workspace),
     *            along side the change log file?
     *
     * 4) p4Label has changed since the last build (either from unset to set, or from
     *    one label to another).
     *
     * 5) p4Label is set AND unchanged AND the set of file-revisions selected
     *    by the label in the p4 workspace has changed.  Unfortunately, I don't
     *    know of a cheap way to do this.
     *
     * There may or may not have been a previous build.  That build may or may not
     * have been done using Perforce, and if with Perforce, may have been done
     * using a label or latest, and may or may not be for the same view as currently
     * defined.  If any change has occurred, we'll treat that as a reason to build.
     *
     * Note that the launcher and workspace may operate remotely (as of 2009-06-21,
     * they correspond to the node where the last build occurred, if any; if none,
     * then the master is used).
     *
     * Note also that this method won't be called while the workspace (not job)
     * is in use for building or some other polling thread.
     */
    @Override
    public boolean pollChanges(AbstractProject project, Launcher launcher,
            FilePath workspace, TaskListener listener) throws IOException, InterruptedException {

        PrintStream logger = listener.getLogger();
        logger.println("Looking for changes...");

        try {
            IServer server = getServer();
            String clientName = getEffectiveClientName(project.getLastBuiltOn(), workspace, listener);
            if (!P4jUtil.existsClient(server, clientName)) {
                logger.println("New client: " + clientName);
                return true;
                // TODO: possible improvement: if the client spec has been deleted but the
                // workspace is uptodate, this return will be a false positive.
            }
            IClient client = getPreparedClient(server, clientName, launcher, workspace, listener);

            Boolean needToBuild = needToBuild(client, project, logger);
            if (needToBuild == null) {
                needToBuild = wouldSyncChangeWorkspace(client, logger);
            }
            logger.flush();
            return needToBuild;

        } catch (Exception e) {
            logServerException(e, logger);
            throw new IOException("Unable to communicate with Perforce.  Check log file for: " + e.getMessage());
        } finally {
            disconnectServer(logger);
        }
    }

    /**
     * Figures out if there is a need to build by looking at build numbers and labels.
     *
     * @return True if there is definitely a need to build, False if definitely not, and null
     * if it cannot be determined.
     */
    private Boolean needToBuild(IClient client, AbstractProject project, PrintStream logger)
            throws IOException, InterruptedException, P4JavaException {

        /*
         * Don't bother polling if we're already building, or soon will.
         * Ideally this would be a policy exposed to the user, perhaps for all
         * jobs with all types of scm, not just those using Perforce.
         */
//        if (project.isBuilding() || project.isInQueue()) {
//            logger.println("Job is already building or in the queue; skipping polling.");
//            return Boolean.FALSE;
//        }

        Run lastBuild = project.getLastBuild();
        if (lastBuild == null) {
            logger.println("No previous build exists.");
            return null;    // Unable to determine if there are changes.
        }

        PerforceTagAction action = lastBuild.getAction(PerforceTagAction.class);
        if (action == null) {
            logger.println("Previous build doesn't have Perforce info.");
            return null;
        }

        int lastChangeNumber = action.getChangeNumber();
        String lastLabelName = action.getTag();

        if (lastChangeNumber <= 0 && lastLabelName != null) {
            logger.println("Previous build was based on label: " + lastLabelName);
            // Last build was based on a label, so we want to know if:
            //      the definition of the label was changed;
            //      or the view has been changed;
            //      or p4Label has been changed.
            if (p4Label == null) {
                logger.println("Job configuration changed to build from head, not a label.");
                return Boolean.TRUE;
            }

            if (!lastLabelName.equals(p4Label)) {
                logger.println("Job configuration changed to build from label " + p4Label + ", not from head");
                return Boolean.TRUE;
            }

            // No change in job definition (w.r.t. p4Label).  Don't currently
            // save enough info about the label to determine if it changed.
            logger.println("Assuming that the workspace and label definitions have not changed.");
            return Boolean.FALSE;
        }

        if (lastChangeNumber > 0) {
            logger.println("Last sync'd change was " + lastChangeNumber);
            if (p4Label != null) {
                logger.println("Job configuration changed to build from label " + p4Label + ", not from head.");
                return Boolean.TRUE;
            }

            // Has any new change been submitted since then (that is selected
            // by this workspace).
            int highestSelectedChangeNumber = P4jUtil.latestChangeId(client);
            if (highestSelectedChangeNumber == -1) {
                // Wierd, this shouldn't be!  I suppose it could happen if the
                // view selects no files (e.g. //depot/non-existent-branch/...).
                // Just in case, let's try to build.
                logger.println("Unexpected empty view has no changes visible.");
                return Boolean.TRUE;
            }

            logger.println("Latest submitted change selected by workspace is " + highestSelectedChangeNumber);
            if (lastChangeNumber >= highestSelectedChangeNumber) {
                // Note, can't determine with currently saved info
                // whether the workspace definition has changed.
                logger.println("No new changes present, assuming that the workspace definition has not changed.");
                return Boolean.FALSE;
            }
            else {
                logger.println("New changes present.");
                return Boolean.TRUE;
            }
        }

        return null;
    }

    // TODO Handle the case where p4Label is set.
    private boolean wouldSyncChangeWorkspace(IClient client, PrintStream logger) throws P4JavaException {
        if (P4jUtil.wouldSyncAtHead(client)) {
            logger.println("Workspace not up-to-date.");
            return true;
        }
        else {
            logger.println("Workspace up-to-date.");
            return false;
        }
    }

    private int getLastChange(Run build) {
        // If we are starting a new hudson project on existing work and want to skip the prior history...
        if (firstChange > 0)
            return firstChange;

        // If we can't find a PerforceTagAction, we will default to 0.

        PerforceTagAction action = getMostRecentTagAction(build);
        if (action == null)
            return 0;

        //log.println("Found last change: " + action.getChangeNumber());
        return action.getChangeNumber();
    }

    private PerforceTagAction getMostRecentTagAction(Run build) {
        if (build == null)
            return null;

        PerforceTagAction action = build.getAction(PerforceTagAction.class);
        if (action != null)
            return action;

        // if build had no actions, keep going back until we find one that does.
        return getMostRecentTagAction(build.getPreviousBuild());
    }

    /**
     * Returns the effective name to use for the client on this build node.
     */
    private String getEffectiveClientName(Node buildNode, FilePath workspace, TaskListener listener)
            throws IOException, InterruptedException, P4JavaException {
        PrintStream log = listener.getLogger();

        // If we are building on a slave node, and each node is supposed to have
        // its own unique client, then adjust the client name accordingly.
        // make sure each slave has a unique client name by adding it's
        // hostname to the end of the client spec

        String nodeSuffix = "";
        String p4Client = this.p4Client;
        if (!nodeIsRemote(buildNode)) {
            log.println("Using master perforce client: " + p4Client);
        }
        else if (dontRenameClient) {
            log.println("Using shared perforce client: " + p4Client);
        }
        else {
            // Use the first part of the hostname as the node suffix.
            String host = workspace.act(new GetHostname());
            if (host.contains(".")) {
                nodeSuffix = "-" + host.subSequence(0, host.indexOf('.'));
            } else {
                nodeSuffix = "-" + host;
            }
            p4Client += nodeSuffix;

            log.println("Using remote perforce client: " + p4Client);
        }
        return p4Client;
    }

    /**
     * Prepare the Client instance for Perforce operations.
     */
    private IClient getPreparedClient(
            IServer server, String p4Client,
            Launcher launcher, FilePath workspace, TaskListener listener)
            throws IOException, InterruptedException, P4JavaException {
        PrintStream log = listener.getLogger();

        boolean creatingNewWorkspace = !P4jUtil.existsClient(server, p4Client);

        // If we use the same client on multiple hosts (e.g. master and slave),
        // erase the host field so the client isn't tied to a single host.

        // Ensure that the root is appropriate (it might be wrong if the user
        // created it, or if we previously built on another node).
        String localPath = PerforceSCMHelper.getLocalPathName(workspace, launcher.isUnix());

        IClient client = P4jUtil.retrieveClient(server, p4Client, dontRenameClient, localPath, projectOptions);

        // If necessary, rewrite the views field in the clientspec
        if (updateView || creatingNewWorkspace) {
            List<String> mappings = parseProjectPath(projectPath, p4Client);

            if (true) { // TODO(CQ) see if string views is equal to mappings asString !views.equals())
                log.println("Changing Client View to:");
                P4jUtil.clearView(client);
                for (int i = 0; i < mappings.size(); ) {
                    String depotPath = mappings.get(i++);
                    String clientPath = mappings.get(i++);
                    P4jUtil.addViewMapping(client, depotPath, clientPath);
                    log.println("  " + depotPath + " " + clientPath);
                }
                server.updateClient(client);
                server.setCurrentClient(client); // TODO(CQ) why is this needed after an update?
            }
        }

        return client;
    }

    private boolean nodeIsRemote(Node buildNode) {
        return buildNode != null && buildNode.getNodeName().length() != 0;
    }


    @Extension
    public static final class PerforceSCMDescriptor extends SCMDescriptor<PerforceSCM> {
        public PerforceSCMDescriptor() {
            super(PerforceSCM.class, PerforceRepositoryBrowser.class);
            load();
        }

        public String getDisplayName() {
            return "Perforce";
        }

        public String isValidProjectPath(String path) {
            if (!path.startsWith("//")) {
                return "Path must start with '//' (Example: //depot/ProjectName/...)";
            }
            if (!path.endsWith("/...")) {
                if (!path.contains("@")) {
                    return "Path must end with Perforce wildcard: '/...'  (Example: //depot/ProjectName/...)";
                }
            }
            return null;
        }

        /**
         * Returns a fresh server object given a request, or null if none available with
         * the request params. The returned server must be disconnected when no longer needed.
         */
        protected IServer getServerFromRequest(StaplerRequest request) {
            String port = fixNull(request.getParameter("port")).trim();
            String user = fixNull(request.getParameter("user")).trim();
            String pass = fixNull(request.getParameter("pass")).trim();

            if (port.length() == 0) { // Not enough entered yet
                return null;
            }
            try {
                IServer server = newServer(port, user, pass);
                try {
                    server.setUserName(user);
                    PerforcePasswordEncryptor encryptor = new PerforcePasswordEncryptor();
                    if (encryptor.appearsToBeAnEncryptedPassword(pass)) {
                        server.login(encryptor.decryptString(pass));
                    }
                    else {
                        server.login(pass);
                    }
                } catch (P4JavaException e) {
                    // user or pass are not correct yet, keep going anyway with no user
                    server.setUserName(null);
                }

                String counter = server.getCounters().get("change");
                if (counter != null)
                    return server;
            }
            catch (URISyntaxException e) {
                // port is not valid yet
            } catch (P4JavaException e) {
                // connect failed for some other reason
            }
            return null;  // no luck talking to server
        }

        /**
         * Checks if the perforce login credentials are good.
         */
        public void doValidatePerforceLogin(StaplerRequest request, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator(request, rsp, false) {
                protected void check() throws IOException, ServletException {
                    IServer server = getServerFromRequest(request);
                    if (server != null && server.getUserName() != null) {
                        try { server.disconnect(); } catch (P4JavaException e) {}
                        ok();
                    } else {
                        error("Could not login to server");
                    }
                }
            }.check();
        }

        /**
         * Checks to see if the specified workspace is valid.
         */
        public FormValidation doValidateP4Client(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {

            String workspace = Util.fixEmptyAndTrim(req.getParameter("client"));
            if (workspace == null) {
                return FormValidation.error("You must enter a workspaces name");
            }
            IServer server = getServerFromRequest(req);
            if (server == null) {
                return FormValidation.error("Unable to check workspace against depot");
            }
            try {
                IClient client = server.getClient(workspace);
                if (client == null || !client.canRefresh())
                    return FormValidation.warning("Workspace does not exist. " +
                            "If \"Let Hudson Manage Workspace View\" is check" +
                            " the workspace will be automatically created.");
            } catch (P4JavaException e) {
                return FormValidation.error("Error accessing perforce while checking workspace");
            } finally {
                try { server.disconnect(); } catch (P4JavaException e) {}
            }

            return FormValidation.ok();
        }

        /**
         * Performs syntactical check on the P4Label
          */
        public FormValidation doValidateP4Label(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {

            String label = Util.fixEmptyAndTrim(req.getParameter("label"));
            if (label == null)
                return FormValidation.ok();

            IServer server = getServerFromRequest(req);
            if (server != null) {
                try {
                    ILabel p4Label = server.getLabel(label);
                    if (p4Label == null || !p4Label.canRefresh())
                        return FormValidation.error("Label does not exist");
                } catch (P4JavaException e) {
                    return FormValidation.error("Error accessing perforce while checking label");
                } finally {
                    try { server.disconnect(); } catch (P4JavaException e) {}
                }

            }
            return FormValidation.ok();
        }

        /**
         * Checks if the value is a valid Perforce project path.
         */
        public FormValidation doCheckProjectPath(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            String view = Util.fixEmptyAndTrim(req.getParameter("value"));
            for (String mapping : view.split("\n")) {
                if (!DEPOT_ONLY.matcher(mapping).matches() && !DEPOT_AND_WORKSPACE.matcher(mapping).matches())
                    return FormValidation.error("Invalid mapping: " + mapping);
            }
            return FormValidation.ok();
        }

        /**
         * Checks if the change list entered exists
         */
        public void doCheckChangeList(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator(req, rsp, false) {
                protected void check() throws IOException, ServletException {
                    String change = fixNull(request.getParameter("change")).trim();
                    if (change.length() == 0) { // nothing entered yet
                        ok();
                        return;
                    }
                    IServer server = getServerFromRequest(request);
                    if (server != null) {
                        try {
                            P4jUtil.getChange(server, Integer.parseInt(change));
                        } catch (P4JavaException e) {
                            error("Error accessing perforce while checking change: " + change);
                        } finally {
                            try { server.disconnect(); } catch (P4JavaException e) {}
                        }
                    }
                    ok();
                    return;
                }
            }.check();
        }
    }

    private static final Pattern DEPOT_ONLY = Pattern.compile("^\\s*//\\S+?(/\\S+)\\s*$");
    private static final Pattern DEPOT_AND_WORKSPACE =
            Pattern.compile("^\\s*(//\\S+?/\\S+)\\s*//\\S+?(/\\S+)\\s*$");

    /**
     * Parses the projectPath into a list of pairs of strings representing the depot and client
     * paths. Even items are depot and odd items are client.
     */
    static List<String> parseProjectPath(String projectPath, String p4Client) {
        List<String> parsed = new ArrayList<String>();
        for (String line : projectPath.split("\n")) {
            Matcher depotOnly = DEPOT_ONLY.matcher(line);
            if (depotOnly.find()) {
                // add the trimmed depot path, plus a manufactured client path
                parsed.add(line.trim());
                parsed.add("//" + p4Client + depotOnly.group(1));
            } else {
                Matcher depotAndWorkspace = DEPOT_AND_WORKSPACE.matcher(line);
                if (depotAndWorkspace.find()) {
                    // add the found depot path and the clientname-tweaked client path
                    parsed.add(depotAndWorkspace.group(1));
                    parsed.add("//" + p4Client + depotAndWorkspace.group(2));
                }
            }
        }
        return parsed;
    }

    /**
     * @return the projectPath
     */
    public String getProjectPath() {
        return projectPath;
    }

    /**
     * @param projectPath the projectPath to set
     */
    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    /**
     * @return the p4User
     */
    public String getP4User() {
        return p4User;
    }

    /**
     * @param user the p4User to set
     */
    public void setP4User(String user) {
        p4User = user;
    }

    /**
     * @return the p4Passwd
     */
    public String getP4Passwd() {
        return p4Passwd;
    }

    /**
     * @param passwd the p4Passwd to set
     */
    public void setP4Passwd(String passwd) {
        PerforcePasswordEncryptor encryptor = new PerforcePasswordEncryptor();
        if (encryptor.appearsToBeAnEncryptedPassword(passwd))
            p4Passwd = passwd;
        else
            p4Passwd = encryptor.encryptString(passwd);
    }

    /**
     * @return the p4Port
     */
    public String getP4Port() {
        return p4Port;
    }

    /**
     * @param port the p4Port to set
     */
    public void setP4Port(String port) {
        p4Port = port;
    }

    /**
     * @return the p4Client
     */
    public String getP4Client() {
        return p4Client;
    }

    /**
     * @param client the p4Client to set
     */
    public void setP4Client(String client) {
        p4Client = client;
    }

    /**
     * @return the p4SysDrive
     */
    public String getP4SysDrive() {
        return p4SysDrive;
    }

    /**
     * @param sysDrive the p4SysDrive to set
     */
    public void setP4SysDrive(String sysDrive) {
        p4SysDrive = sysDrive;
    }

    /**
     * @return the p4SysRoot
     */
    public String getP4SysRoot() {
        return p4SysRoot;
    }

    /**
     * @param sysRoot the p4SysRoot to set
     */
    public void setP4SysRoot(String sysRoot) {
        p4SysRoot = sysRoot;
    }

    /**
     * @return the p4Exe
     */
    public String getP4Exe() {
        return p4Exe;
    }

    /**
     * @param exe the p4Exe to set
     */
    public void setP4Exe(String exe) {
        p4Exe = exe;
    }

    /**
     * @return the p4Label
     */
    public String getP4Label() {
        return p4Label;
    }

    /**
     * @param exe the p4Label to set
     */
    public void setP4Label(String label) {
        p4Label = label;
    }

    /**
     * @param update    True to let the plugin manage the view, false to let the user manage it
     */
    public void setUpdateView(boolean update) {
        this.updateView = update;
    }

    /**
     * @return  True if the plugin manages the view, false if the user does.
     */
    public boolean isUpdateView() {
        return updateView;
    }

    /**
     * @return  True if we are performing a one-time force sync
     */
    public boolean isForceSync() {
        return forceSync;
    }

    /**
     * @param force True to perform a one time force sync, false to perform normal sync
     */
    public void setForceSync(boolean force) {
        this.forceSync = force;
    }

    /**
     * @return  True if we are using a label
     */
    public boolean isUseLabel() {
        return p4Label != null;
    }

    /**
     * @param dontRenameClient  False if the client will rename the client spec for each
     * slave
     */
    public void setDontRenameClient(boolean dontRenameClient) {
        this.dontRenameClient = dontRenameClient;
    }

    /**
     * @return  True if the client will rename the client spec for each slave
     */
    public boolean isDontRenameClient() {
        return dontRenameClient;
    }

    public String getProjectOptions() {
        return projectOptions;
    }

    public void setProjectOptions(String p4ClientOptions) {
        this.projectOptions = p4ClientOptions;
    }

    /**
     * This is only for the config screen.  Also, it returns a string and not an int.
     * This is because we want to show an empty value in the config option if it is not being
     * used.  The default value of -1 is not exactly empty.  So if we are set to default of
     * -1, we return an empty string.  Anything else and we return the actual change number.
     *
     * @return  The one time use variable, firstChange.
     */
    public String getFirstChange() {
        if (firstChange <= 0)
            return "";
        return Integer.valueOf(firstChange).toString();
    }

    /**
     * Get the hostname of the client to use as the node suffix
     */
    private static final class GetHostname implements FileCallable<String> {
        public String invoke(File f, VirtualChannel channel) throws IOException {
            return InetAddress.getLocalHost().getHostName();
        }
        private static final long serialVersionUID = 1L;
    }


    /**
     * With Perforce the server keeps track of files in the workspace.  We never
     * want files deleted without the knowledge of the server so we disable the
     * cleanup process.
     *
     * @param project
     *      The project that owns this {@link SCM}. This is always the same
     *      object for a particular instanceof {@link SCM}. Just passed in here
     *      so that {@link SCM} itself doesn't have to remember the value.
     * @param workspace
     *      The workspace which is about to be deleted. Never null. This can be
     *      a remote file path.
     * @param node
     *      The node that hosts the workspace. SCM can use this information to
     *      determine the course of action.
     *
     * @return
     *      true if {@link SCM} is OK to let Hudson proceed with deleting the
     *      workspace.
     *      False to veto the workspace deletion.
     */
    @Override
    public boolean processWorkspaceBeforeDeletion(AbstractProject<?,?> project, FilePath workspace, Node node) {
        Logger.getLogger(PerforceSCM.class.getName()).info("Veto workspace cleanup");
        return false;
        // TODO(CQ): figure out how to tell the server that we've delete the workspace instead?
        // Or, use the -p (serverBypass) on sync so the server doesn't track our files at all?
    }
}
