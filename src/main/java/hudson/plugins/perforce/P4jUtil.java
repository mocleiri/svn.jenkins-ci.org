package hudson.plugins.perforce;

import com.perforce.p4java.client.P4JClient;
import com.perforce.p4java.client.P4JClientView;
import com.perforce.p4java.core.P4JChangeList;
import com.perforce.p4java.core.P4JJob;
import com.perforce.p4java.core.P4JLabel;
import com.perforce.p4java.core.file.P4JFileSpec;
import com.perforce.p4java.core.file.P4JFileSpecBuilder;
import com.perforce.p4java.core.file.P4JFileSpecOpStatus;
import com.perforce.p4java.exception.P4JAccessException;
import com.perforce.p4java.exception.P4JConnectionException;
import com.perforce.p4java.exception.P4JException;
import com.perforce.p4java.exception.P4JRequestException;
import com.perforce.p4java.impl.generic.client.P4JClientSpecImpl;
import com.perforce.p4java.impl.generic.client.P4JClientViewImpl;
import com.perforce.p4java.impl.generic.core.P4JLabelImpl;
import com.perforce.p4java.impl.mapbased.client.P4JClientImpl;
import com.perforce.p4java.server.P4JServer;
import com.perforce.p4java.server.P4JServerFactory;
import com.perforce.p4java.server.P4JUser;
import com.perforce.p4java.server.callback.P4JLogCallback;
import com.perforce.p4java.server.callback.P4JProgressCallback;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
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
            P4JServerFactory.logCallback = new P4JLogCallback() {
                public void internalError(String arg0) {}
                public void internalException(Throwable arg0) {}
                public void internalWarn(String arg0) {}
                public void internalInfo(String arg0) {}
                public void internalStats(String arg0) {}
                public void internalTrace(P4JLogTraceLevel arg0, String arg1) {}
                public P4JLogTraceLevel getTraceLevel() {
                    return P4JLogTraceLevel.NONE;
                }
            };
        }
    }

    /**
     * Returns a new connected server instance given the server+port, client program & version,
     * and optionally user login credentials. Server is logged into if user credentials are
     * supplied.
     */
    static P4JServer newServer(String p4port, String prog, String ver, String user, String pass)
            throws URISyntaxException, P4JException {
        Properties props = new Properties();
        props.put("programName", prog);
        props.put("programVersion", ver);
        // Note: props= PROG_NAME_KEY(programName), PROG_VERSION_KEY(programVersion)
        // Note: props= USER_NAME_KEY(userName), CLIENT_NAME_KEY(clientName), AUTO_CONNECT_KEY(autoConnect)
        P4JServer server = P4JServerFactory.getServer("p4java://" + p4port, props);
        server.connect();
        if (user != null) {
            server.setUserName(user);
            if (pass != null) {
                server.login(pass);
            }
        }
        //TODO(CQ): we could also register a progress callback to allow reporting & cancelling
        if (PROGRESS_ENABLED) {
            server.registerProgressCallback(new P4JProgressCallback() {
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
    static P4JUser getUser(P4JServer server, String id) throws P4JException {
        List<P4JUser> pus = server.getUserList(Arrays.asList(id), 0);
        if (pus.size() > 0) {
            return pus.get(0);
        }
        return null;
    }

    /**
     * Returns the latest changelist id on the server. This is really fast.
     */
    static int latestChangeId(P4JServer server) throws P4JException {
         return Integer.parseInt(server.getCounters().get("change"));
    }

    static P4JChangeList getChange(P4JServer server, int id) throws P4JException {
        //TODO(CQ) P4JServer.getChangeList() fails when using a r/o role account
        return server.getChangeList(id);
    }

    /**
     * Returns all of the changelists within a given id range (inclusive) submitted by any user,
     * visible under a given client.
     */
    static List<P4JChangeList> changesInRange(P4JClient client, int from, int to)
            throws P4JException {
        List<P4JFileSpec> specs = P4JFileSpecBuilder.makeFileSpecList(
                new String[] { "//" + client.getName() + "/...@" + from + ",@" + to });
        return client.getServer().getChangeLists(-1, specs, /*client.getName()*/null, null,
                /*integ=*/true, /*subm=*/true, /*pend=*/false, /*long=*/true);
    }

    /**
     * Returns a list of change ids given a list of changelist objects.
     */
    static List<Integer> changeIds(List<P4JChangeList> changes) {
        List<Integer> ids = new ArrayList<Integer>(changes.size());
        for (P4JChangeList cl : changes)
            ids.add(cl.getId());
        return ids;
    }

    static String jobStatus(P4JJob job) {
        return (String) job.getRawFields().get("Status");
    }

    /**
     * Returns true iff the named client exists on the server.
     */
    static boolean existsClient(P4JServer server, String name) throws P4JException {
        return server.getClient(name) != null;
    }

    // debug tracing helper
    static void trace(String prefix, P4JClient client) {
        System.out.println(prefix + " isComplete:" + client.isComplete() +
//                " canC:" + client.canComplete() +
//                " canR:" + client.canRefresh() +
                " canU:" + client.canUpdate());
    }

    /**
     * Returns a new or updated client with the host optionally stripped and the root optionaly set.
     */
    static P4JClient retrieveClient(P4JServer server, String name, boolean clearHost, String root)
            throws P4JException {
        // Get the existing client if available
        P4JClient client = server.getClient(name);

        // If there are no changes to make, return the client directly
        if (client != null &&
                (!clearHost || client.getHostName() == null) &&
                (root == null || root.equals(client.getRoot()))) {
            //trace("U0", client);
            return client;
        }

        // Create a spec from either the existing client, or a new template
        P4JClientSpecImpl spec;
        if (client != null) {
            //System.out.println("existing client:" + client);
            spec = specForClient(client);
        } else {
            P4JClient tpl = server.getClientTemplate(name);
            //System.out.println("template client:" + tpl);
            spec = specForClient(tpl);
        }
        if (clearHost) {
            spec.setHostName("");
        }
        if (root != null) {
            spec.setRoot(root);
        }

        // Push new client-side client object to server & get updated reference
        P4JClient nc = new P4JClientImpl(server, spec, false);
        if (client != null) {
            //trace("O0", nc);
            server.updateClient(nc);
            //trace("O1", nc);
            return server.getClient(nc.getName()); //nc;
        } else {
            //trace("N0", nc);
            server.newClient(nc);
            //trace("N1", nc);
        }
        client = server.getClient(nc.getName());
        server.setCurrentClient(client);
        return client;
    }

    private static P4JClientSpecImpl specForClient(P4JClient client) {
        P4JClientSpecImpl spec = new P4JClientSpecImpl();
        spec.setAccessed(client.getAccessed());
        spec.setAlternateRoots(client.getAlternateRoots());
        spec.setClientView(client.getClientView());
        spec.setDescription(client.getDescription());
        spec.setHostName(client.getHostName());
        spec.setLineEnd(client.getLineEnd());
        spec.setName(client.getName());
        spec.setOptions(client.getOptions());
        spec.setOwnerName(client.getOwnerName());
        spec.setRoot(client.getRoot());
        spec.setSubmitOptions(client.getSubmitOptions());
        spec.setUpdated(client.getUpdated());
        return spec;
    }

    /**
     * Deletes a given client by name.
     */
    static void deleteClient(P4JServer server, String clientname) throws P4JException {
        server.deleteClient(clientname, false);
    }

    /**
     * Returns the most recent changelist id made by any user, visible under a given client. This
     * is reasonably fast, but the changelist will have only a short description.
     */
    static int latestChangeId(P4JClient client) {
        List<P4JFileSpec> specs = P4JFileSpecBuilder.makeFileSpecList(
                new String[] { "//" + client.getName() + "/..." });
        try {
            List<P4JChangeList> changes =
                    client.getServer().getChangeLists(1, specs, null, null, true, true, false, false);
            return (changes != null && changes.size() >= 1) ? changes.get(0).getId() : -1;
        } catch (P4JException e) {
            // ignore
            //e.printStackTrace();
            return -1;
        }
    }

    /**
     * Clears the view spec for a given client, removing all the mappings.
     */
    static void clearView(P4JClient client) {
        client.getClientView().getMapping().clear();
    }

    /**
     * Adds a mapping pair to the view spec for a client.
     */
    static void addViewMapping(P4JClient client, String depotSpec, String clientSpec) {
        List<P4JClientView.P4JClientViewMapping> mapping = client.getClientView().getMapping();
        mapping.add(new P4JClientViewImpl.P4JClientViewMappingImpl(mapping.size(), depotSpec, clientSpec));
    }

    /**
     * Clears the view spec for a given label, removing all the mappings.
     */
    static void clearView(P4JLabel label) {
        label.getViewMapping().clear();
    }

    /**
     * Adds a mapping to the view spec for a label.
     */
    static void addViewMapping(P4JLabel label, String depotSpec) {
        List<P4JClientView.P4JClientViewMapping> mapping = label.getViewMapping();
        mapping.add(new P4JClientViewImpl.P4JClientViewMappingImpl(mapping.size(), depotSpec, ""));
    }

    /**
     * Returns true iff syncing the given client to head would make changes.
     */
    static boolean wouldSync(P4JClient client, String path)
            throws P4JConnectionException, P4JRequestException, P4JAccessException {
        List<P4JFileSpec> specs = P4JFileSpecBuilder.makeFileSpecList(new String[] { path });
        List<P4JFileSpec> what = client.sync(specs, false, true, false, false);
        //for (P4JFileSpec fs : what) {
        //    System.out.println(" " + fs.getOpStatus() + ": " + fs.getDisplayPath());
        //}
        // Nothing to sync is indicated by a single error entry
        return !what.isEmpty() && what.get(0).getOpStatus() != P4JFileSpecOpStatus.ERROR;
    }

    /**
     * Returns true iff syncing the given client to head would make changes.
     */
    static boolean wouldSyncAtHead(P4JClient client)
            throws P4JConnectionException, P4JRequestException, P4JAccessException {
        return wouldSync(client, "//" + client.getName() + "/...");
    }

    /**
     * Returns true iff syncing the given client to head would make changes.
     */
    static boolean wouldSyncAtChangeId(P4JClient client, int id)
            throws P4JConnectionException, P4JRequestException, P4JAccessException {
        return wouldSync(client, "//" + client.getName() + "/...@" + id);
    }

    /**
     * Synchronizes the given client workspace.
     */
    static List<P4JFileSpec> sync(P4JClient client, String path, boolean force)
            throws P4JConnectionException, P4JRequestException, P4JAccessException {
        List<P4JFileSpec> specs = P4JFileSpecBuilder.makeFileSpecList(new String[] { path });
        return client.sync(specs, force, false, false, false);
    }

    /**
     * Synchronizes the given client workspace to head.
     */
    static List<P4JFileSpec> syncToHead(P4JClient client, boolean force)
            throws P4JConnectionException, P4JRequestException, P4JAccessException {
        return sync(client, "//" + client.getName() + "/...", force);
    }

    /**
     * Synchronizes the given client workspace to a specified changelist id.
     */
    static List<P4JFileSpec> syncToChangeId(P4JClient client, int id, boolean force)
            throws P4JConnectionException, P4JRequestException, P4JAccessException {
        return sync(client, "//" + client.getName() + "/...@" + id, force);
    }

    /**
     * Synchronizes the given client workspace to a specified label.
     */
    static List<P4JFileSpec> syncToLabel(P4JClient client, String label, boolean force)
            throws P4JConnectionException, P4JRequestException, P4JAccessException {
        return sync(client, "//" + client.getName() + "/...@" + label, force);
    }

    static P4JLabel newLabel(P4JServer server, String tag, String desc, int id, String[] mappings) {
        P4JLabelImpl label = new P4JLabelImpl();
        label.setName(tag);
        label.setDescription(desc);
        label.setRevisionSpec(new Integer(id).toString());
        for (String mapping : mappings) {
            P4jUtil.addViewMapping(label, mapping);
        }
        label.setServer(server);
        return label;
    }

    static void deleteLabel(P4JServer server, String label)
            throws P4JConnectionException, P4JRequestException, P4JAccessException {
        //TODO(CQ) P4JServer.deleteLabel is missing from P4Java, reported to Perforce.
        // server.deleteLabel(label, false);
    }

    
    private P4jUtil() { }

}
