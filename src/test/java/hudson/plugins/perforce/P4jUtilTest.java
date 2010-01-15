package hudson.plugins.perforce;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.IUser;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerStatus;
import java.util.List;
import junit.framework.TestCase;

/**
 * @author cquinn
 */
public class P4jUtilTest extends TestCase {

    private static final String p4port = "perforce:1666";
    private static final String p4user = "cquinn"; //rolem cquinn
    private static final String p4pass = "";
    private static final String p4client = "hudson_plugin_debug_client";
    private static final String p4label = "hudson_plugin_debug_label";
    private static final String fakeRoot = "/tmp/p4j";
    private static final String testDir = "/Tomcat6.0.10/conf";
    private static final String testDepotPath = "//depot" + testDir + "/..." ;
    private static final String testClientPath = "//" + p4client + testDir + "/..." ;
    private static final int existingCl = 253215;  // any valid existing CL
    private static final int existingClWithJob = 225748;  // any valid existing CL with a Job attached
    private static final int rangeCl0 = 118758;  // Start CL for range test
    private static final int rangeCl1 = 121965;  // End CL for rage test--should yield 4 CLs

    private static IServer makeTestServer() throws Exception {
        return P4jUtil.newServer(p4port, "p4jUtilTest", "1", p4user, p4pass);
    }

    private static IClient makeSmallClient() throws Exception {
        IServer server = makeTestServer();
        P4jUtil.deleteClient(server, p4client);
        IClient client = P4jUtil.retrieveClient(server, p4client, false, fakeRoot);
        P4jUtil.clearView(client);
        P4jUtil.addViewMapping(client, testDepotPath, testClientPath);
        server.updateClient(client);
        server.setCurrentClient(client);
        return client;
    }

    public void testLogin() throws Exception {
        IServer server = makeTestServer();
        assertEquals(ServerStatus.READY, server.getStatus());
        assertEquals(p4user, server.getUserName());
        System.out.println("clientAddr:" + server.getServerInfo().getClientAddress());
        System.out.println("clientHost:" + server.getServerInfo().getClientHost());
        System.out.println("clientRoot:" + server.getServerInfo().getClientRoot());
        System.out.println("serverAddr:" + server.getServerInfo().getServerAddress());
        System.out.println("serverRoot:" + server.getServerInfo().getServerRoot());
        server.disconnect();
    }

    public void testGetUser() throws Exception {
        IServer server = makeTestServer();
        IUser user = P4jUtil.getUser(server, p4user);
        System.out.println("user: id:" + user.getLoginName() + " email:" + user.getEmail() + " full:" + user.getFullName());
        assertEquals(p4user, user.getLoginName());
        server.disconnect();
    }

    public void testGetLatestCL() throws Exception {
        IServer server = makeTestServer();
        int latestCl = P4jUtil.latestChangeId(server);
        System.out.println("latest CL on server:" + latestCl);
        assertTrue(latestCl > 0);
        server.disconnect();
    }

    public void testGetSpecificChangelist() throws Exception {
        IServer server = makeTestServer();
        IChangelist change = P4jUtil.getChange(server, existingCl);
        assertEquals(253215, change.getId());
    }

    public void testGetChangelistJobs() throws Exception {
        IClient client = makeSmallClient();
        int lastCl = P4jUtil.latestChangeId(client);
        IChangelist change = P4jUtil.getChange(client.getServer(), lastCl);
        for (IJob job : change.getJobs()) {
            System.out.print(job.getId());
            System.out.print(job.getDescription());
        }
        client.getServer().disconnect();
    }

    public void testJobStatus() throws Exception {
        IServer server = makeTestServer();
        IChangelist change = P4jUtil.getChange(server, existingClWithJob);
        assertEquals(1, change.getJobs().size());
        IJob job = change.getJobs().get(0);
        //System.out.println("Status for " + job.getId() + " is " + P4jUtil.jobStatus(job));
        assertEquals("open", P4jUtil.jobStatus(job));
        server.disconnect();
    }

    public void testRetrieveRawClient() throws Exception {
        IServer server = makeTestServer();
        IClient client = P4jUtil.retrieveClient(server, p4client, false, null);
        //P4jUtil.trace("R ", client);
        assertEquals(p4client, client.getName());
        server.disconnect();
    }

    public void testRetrieveUpdatedClient() throws Exception {
        IServer server = makeTestServer();
        P4jUtil.retrieveClient(server, p4client, true, "/xxx");  // force a root change
        IClient client = P4jUtil.retrieveClient(server, p4client, true, fakeRoot);
        //P4jUtil.trace("O ", client);
        assertEquals(p4client, client.getName());
        assertEquals(fakeRoot, client.getRoot());
        //System.out.println("clientView:" + client.getClientView());
        server.disconnect();
    }

    public void testRetrieveNewClient() throws Exception {
        IServer server = makeTestServer();
        P4jUtil.deleteClient(server, p4client); // make sure the old one is gone
        IClient client = P4jUtil.retrieveClient(server, p4client, true, fakeRoot);
        //P4jUtil.trace("N ", client);
        assertEquals(p4client, client.getName());
        server.disconnect();
    }

    public void testRetrieveSameClient() throws Exception {
        IServer server = makeTestServer();
        P4jUtil.retrieveClient(server, p4client, true, fakeRoot);
        IClient client = P4jUtil.retrieveClient(server, p4client, true, fakeRoot);
        //P4jUtil.trace("S ", client);
        assertEquals(p4client, client.getName());
        server.disconnect();
    }

    public void testUpdateClientViewSpec() throws Exception {
        IServer server = makeTestServer();
        IClient client = P4jUtil.retrieveClient(server, p4client, true, fakeRoot);
        P4jUtil.clearView(client);
        P4jUtil.addViewMapping(client, "//depot/A/...", "//" + p4client + "/a/...");
        P4jUtil.addViewMapping(client, "//depot/B/...", "//" + p4client + "/b/...");
        P4jUtil.addViewMapping(client, "//depot/C/...", "//" + p4client + "/c/...");
        server.updateClient(client);
        // TODO: retrieve view and check that mappings match
        server.disconnect();
    }

    public void testGetLatestClientCL() throws Exception {
        IServer server = makeTestServer();
        IClient client = P4jUtil.retrieveClient(server, p4client, true, fakeRoot);
        P4jUtil.clearView(client);
        P4jUtil.addViewMapping(client, testDepotPath, testClientPath);
        server.updateClient(client);
        server.setCurrentClient(client);
        int lastCl = P4jUtil.latestChangeId(client);
        System.out.println("latest CL in client:" + lastCl);
        assertTrue(lastCl > 0);
        server.disconnect();
    }

     //TODO(CQ) generalize this test
    public void testGetLatestClientChangesInRange() throws Exception {
        IServer server = makeTestServer();
        IClient client = P4jUtil.retrieveClient(server, p4client, true, fakeRoot);
        P4jUtil.clearView(client);
        P4jUtil.addViewMapping(client, testDepotPath, testClientPath);
        server.updateClient(client);
        server.setCurrentClient(client);
        List<IChangelist> changes = P4jUtil.changesInRange(client, rangeCl0, rangeCl1);
        assertNotNull(changes);
        System.out.println("changeCount:" + changes.size());
        for (IChangelist cl : changes) {
            System.out.println(" change:" + cl.getId() + ": " + cl.getDescription());
        }
        assertTrue(changes.size() == 4);
        server.disconnect();
    }

    public void testSyncAndWouldSync() throws Exception {
        IClient client = makeSmallClient();
        int lastCl = P4jUtil.latestChangeId(client.getServer());
        System.out.println("Should sync:");
        assertTrue(P4jUtil.wouldSyncAtChangeId(client, lastCl));
        P4jUtil.syncToChangeId(client, lastCl, true);
        System.out.println("Shouldn't sync:");
        assertFalse(P4jUtil.wouldSyncAtChangeId(client, lastCl));
        client.getServer().disconnect();
    }

    // TODO reenable this test once Iava spports label deletion
    public void BROKEN_testDeleteLabel() throws Exception {
        IServer server = makeTestServer();
        P4jUtil.deleteLabel(server, p4label);
        ILabel label = server.getLabel(p4label);
        assertNull(label);
    }

    public void testMakeLabel() throws Exception {
        IServer server = makeTestServer();
        P4jUtil.deleteLabel(server, p4label);
        ILabel label = P4jUtil.newLabel(server, p4label, "Just a test label", existingCl,
                new String[] { testDepotPath });
        server.updateLabel(label);
        server.disconnect();
    }

}
