package hudson.plugins.perforce;

import com.perforce.p4java.*;
import com.perforce.p4java.client.*;
import com.perforce.p4java.core.*;
import com.perforce.p4java.core.file.*;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.impl.generic.client.ClientOptions;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.server.*;
import com.perforce.p4java.server.callback.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * P4Java utility methods. All significant P4Java library calls are wrappped here to improve
 * testability. See P4jUtilTest.
 *
 * @author Carl Quinn
 */
public final class P4jUtil {

    public static final boolean LOGGING_ENABLED = false;
    public static final boolean PROGRESS_ENABLED = false;

    {
        if (LOGGING_ENABLED) {
            // Hook P4Java logging callback for help in debugging
            Log.setLogCallback( new ILogCallback() {
                public void internalError(String arg0) {}
                public void internalException(Throwable arg0) {}
                public void internalWarn(String arg0) {}
                public void internalInfo(String arg0) {}
                public void internalStats(String arg0) {}
                public void internalTrace(LogTraceLevel arg0, String arg1) {}
                public LogTraceLevel getTraceLevel() {
                    return ILogCallback.LogTraceLevel.NONE;
                }
            } );
        }
    }

    /**
     * Returns a new connected server instance given the server+port, client program & version,
     * and optionally user login credentials. Server is logged into if user credentials are
     * supplied.
     */
    static IServer newServer(String p4port, String prog, String ver, String user, String pass)
            throws URISyntaxException, P4JavaException {
        Properties props = new Properties();
        props.put("programName", prog);
        props.put("programVersion", ver);
        props.put("autoConnect", false);
        props.put("autoLogin", false);
        // Note: props= PROG_NAME_KEY(programName), PROG_VERSION_KEY(programVersion)
        // Note: props= USER_NAME_KEY(userName), CLIENT_NAME_KEY(clientName), AUTO_CONNECT_KEY(autoConnect)

        // TODO: May need to set an ISystemFileCommandsHelper in order to perform advanced
        // filesystem operations that java doesn't support. (if using the pure java impl.)

        IServer server = ServerFactory.getServer("p4java://" + p4port, props);
        server.connect();
        if (user != null) {
            server.setUserName(user);
            if (pass != null) {
                server.login(pass);
            }
        }
        //TODO(CQ): we could also register a progress callback to allow reporting & cancelling
        if (PROGRESS_ENABLED) {
            server.registerProgressCallback(new IProgressCallback() {
                public void start(int arg0) {
                }
                public boolean tick(int arg0, String arg1) {
                    return true; // true to continue, false to stop
                }
                public void stop(int arg0) {
                }
            });
        }
        return server;
    }

    /**
     * Returns the User object from a server for a given string userid.
     */
    static IUser getUser(IServer server, String id) throws P4JavaException {
        return server.getUser(id);
    }

    /**
     * Returns the latest changelist id on the server. This is really fast.
     */
    static int latestChangeId(IServer server) throws P4JavaException {
         return Integer.parseInt(server.getCounter("change"));
    }

    static IChangelist getChange(IServer server, int id) throws P4JavaException {
        //TODO(CQ) IServer.getChangeList() fails when using a r/o role account
        return server.getChangelist(id);
    }

    /**
     * Returns all of the changelists within a given id range (inclusive) submitted by any user,
     * visible under a given client.
     */
    static List<IChangelist> changesInRange(IClient client, int from, int to)
            throws P4JavaException {
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(
                new String[] { "//" + client.getName() + "/...@" + from + ",@" + to });
        // Perforce decided (annoyingly) to remove the ability to fetch full
        // changelists. Instead, we have to pull them out one by one after getting
        // the list of client summaries.
        List<IChangelistSummary> summaries = client.getServer().getChangelists(-1, specs, /*client.getName()*/null, null,
                /*integ=*/true, /*subm=*/true, /*pend=*/false, /*long=*/true);
        List<IChangelist> changelists = new ArrayList<IChangelist>();
        for(IChangelistSummary summary : summaries){
            int changeId = summary.getId();
            IChangelist changelist = client.getServer().getChangelist(changeId);
            changelists.add(changelist);
        }
        return changelists;
    }

    /**
     * Returns a list of change ids given a list of changelist objects.
     */
    static List<Integer> changeIds(List<IChangelist> changes) {
        List<Integer> ids = new ArrayList<Integer>(changes.size());
        for (IChangelist cl : changes)
            ids.add(cl.getId());
        return ids;
    }

    static String jobStatus(IJob job) {
        return (String) job.getRawFields().get("Status");
    }

    /**
     * Returns true iff the named client exists on the server.
     */
    static boolean existsClient(IServer server, String name) throws P4JavaException {
        return server.getClient(name) != null;
    }

    // debug tracing helper
    static void trace(String prefix, IClient client) {
        System.out.println(prefix + 
//                " canC:" + client.canComplete() +
//                " canR:" + client.canRefresh() +
                " canU:" + client.canUpdate());
    }

    /**
     * Returns a new or updated client with the host optionally stripped and the root optionaly set.
     */
    static IClient retrieveClient(IServer server, String name, boolean clearHost, String root) throws P4JavaException{
        return P4jUtil.retrieveClient(server, name, clearHost, root, null);
    }

    static IClient retrieveClient(IServer server, String name, boolean clearHost, String root, String options)
            throws P4JavaException {
        // Get the existing client if available
        IClient client = server.getClient(name);

        // If there are no changes to make, return the client directly
        if (client != null &&
                (!clearHost || client.getHostName() == null) &&
                (root == null || root.equals(client.getRoot())) &&
                (options == null || client.getOptions().toString().equals(options))) {
            //trace("U0", client);
            return client;
        }

        // Create a spec from either the existing client, or a new template
        IClient spec;
        if (client != null) {
            //System.out.println("existing client:" + client);
            spec = client;
        } else {
            spec = new Client(server);
            spec.setName(name);
        }
        if (clearHost) {
            spec.setHostName("");
        }
        if (root != null) {
            spec.setRoot(root);
        }
        if (options != null){
            spec.setOptions(new ClientOptions(options));
        }

        // Push new client-side client object to server & get updated reference
        if (client != null) {
            server.updateClient(spec);
        } else {
            server.createClient(spec);
        }
        client = server.getClient(spec.getName());
        // hopefully not necessary anymore
        //server.setCurrentClient(client);
        return client;
    }

    /**
     * Deletes a given client by name.
     */
    static void deleteClient(IServer server, String clientname) throws P4JavaException {
        server.deleteClient(clientname, false);
    }

    /**
     * Returns the most recent changelist id made by any user, visible under a given client. This
     * is reasonably fast, but the changelist will have only a short description.
     */
    static int latestChangeId(IClient client) {
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(
                new String[] { "//" + client.getName() + "/..." });
        try {
            List<IChangelistSummary> changes =
                    client.getServer().getChangelists(1, specs, client.getName(), null, true, IChangelist.Type.SUBMITTED, false);
            return (changes != null && changes.size() >= 1) ? changes.get(0).getId() : -1;
        } catch (P4JavaException e) {
            // ignore
            //e.printStackTrace();
            return -1;
        }
    }

    /**
     * Clears the view spec for a given client, removing all the mappings.
     */
    static void clearView(IClient client) {
        client.getClientView().getEntryList().clear();
    }

    /**
     * Adds a mapping pair to the view spec for a client.
     */
    static void addViewMapping(IClient client, String depotSpec, String clientSpec) {
        ClientView mapping = client.getClientView();
        mapping.addEntry(new ClientView.ClientViewMapping(mapping.getSize(), depotSpec, clientSpec));
    }

    /**
     * Clears the view spec for a given label, removing all the mappings.
     */
    static void clearView(ILabel label) {
        label.getViewMapping().getEntryList().clear();
    }

    /**
     * Adds a mapping to the view spec for a label.
     */
    static void addViewMapping(ILabel label, String depotSpec) {
        ViewMap<ILabelMapping> mapping = label.getViewMapping();
        mapping.addEntry(new Label.LabelMapping(mapping.getSize(), depotSpec));
    }

    /**
     * Returns true iff syncing the given client to head would make changes.
     */
    static boolean wouldSync(IClient client, String path)
            throws ConnectionException, RequestException, AccessException {
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(new String[] { path });
        List<IFileSpec> what = client.sync(specs, false, true, false, false);
        //for (IFileSpec fs : what) {
        //    System.out.println(" " + fs.getOpStatus() + ": " + fs.getDisplayPath());
        //}
        // Nothing to sync is indicated by a single error entry
        return !what.isEmpty() && what.get(0).getOpStatus() != FileSpecOpStatus.ERROR;
    }

    /**
     * Returns true iff syncing the given client to head would make changes.
     */
    static boolean wouldSyncAtHead(IClient client)
            throws ConnectionException, RequestException, AccessException {
        return wouldSync(client, "//" + client.getName() + "/...");
    }

    /**
     * Returns true iff syncing the given client to head would make changes.
     */
    static boolean wouldSyncAtChangeId(IClient client, int id)
            throws ConnectionException, RequestException, AccessException {
        return wouldSync(client, "//" + client.getName() + "/...@" + id);
    }

    /**
     * Synchronizes the given client workspace.
     */
    static List<IFileSpec> sync(IClient client, String path, boolean force)
            throws ConnectionException, RequestException, AccessException {
        List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(new String[] { path });
        return client.sync(specs, force, false, false, false);
    }

    /**
     * Synchronizes the given client workspace to head.
     */
    static List<IFileSpec> syncToHead(IClient client, boolean force)
            throws ConnectionException, RequestException, AccessException {
        return sync(client, "//" + client.getName() + "/...", force);
    }

    /**
     * Synchronizes the given client workspace to a specified changelist id.
     */
    static List<IFileSpec> syncToChangeId(IClient client, int id, boolean force)
            throws ConnectionException, RequestException, AccessException {
        return sync(client, "//" + client.getName() + "/...@" + id, force);
    }

    /**
     * Synchronizes the given client workspace to a specified label.
     */
    static List<IFileSpec> syncToLabel(IClient client, String label, boolean force)
            throws ConnectionException, RequestException, AccessException {
        return sync(client, "//" + client.getName() + "/...@" + label, force);
    }

    static ILabel newLabel(IServer server, String tag, String desc, int id, String[] mappings) {
        Label label = new Label();
        label.setName(tag);
        label.setDescription(desc);
        label.setRevisionSpec(new Integer(id).toString());
        for (String mapping : mappings) {
            P4jUtil.addViewMapping(label, mapping);
        }
        label.setServer(server);
        return label;
    }

    static void deleteLabel(IServer server, String label)
            throws ConnectionException, RequestException, AccessException {
        //TODO(CQ) IServer.deleteLabel is missing from P4Java, reported to Perforce.
        // server.deleteLabel(label, false);
    }

    
    private P4jUtil() { }

}
