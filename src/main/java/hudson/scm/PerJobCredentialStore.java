package hudson.scm;

import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Saveable;
import hudson.remoting.Channel;
import hudson.scm.BlameSubversionSCM.DescriptorImpl.Credential;
import hudson.scm.BlameSubversionSCM.DescriptorImpl.RemotableSVNAuthenticationProvider;
import org.tmatesoft.svn.core.SVNURL;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;

/**
 * Persists the credential per job. This object is remotable.
 * 
 * Modify: changed by tang
 * 
 * @author tang,Kohsuke Kawaguchi
 */
final class PerJobCredentialStore implements Saveable, RemotableSVNAuthenticationProvider {
    private final transient AbstractProject<?,?> project;

    /**
     * SVN authentication realm to its associated credentials, scoped to this project.
     */
    private final Map<String,Credential> credentials = new Hashtable<String,Credential>();
    
    PerJobCredentialStore(AbstractProject<?,?> project) {
        this.project = project;
        // read existing credential
        XmlFile xml = getXmlFile();
        try {
            if (xml.exists())
                xml.unmarshal(this);
        } catch (IOException e) {
            // ignore the failure to unmarshal, or else we'll never get through beyond this point.
            LOGGER.log(INFO,"Failed to retrieve Subversion credentials from "+xml,e);
        }
    }

    public synchronized Credential get(String realm) {
        return credentials.get(realm);
    }

    public Credential getCredential(SVNURL url, String realm) {
        return get(realm);
    }

    public void acknowledgeAuthentication(String realm, Credential cred) {
        try {
            acknowledge(realm, cred);
        } catch (IOException e) {
            LOGGER.log(INFO,"Failed to persist the credentials",e);
        }
    }

    public synchronized void acknowledge(String realm, Credential cred) throws IOException {
        Credential old = cred==null ? credentials.remove(realm) : credentials.put(realm, cred);
        // save only if there was a change
        if (old==null && cred==null)    return;
        if (old==null || cred==null || !old.equals(cred))
            save();
    }

    public synchronized void save() throws IOException {
        getXmlFile().write(this);
    }

    private XmlFile getXmlFile() {
        return new XmlFile(new File(project.getRootDir(),"subversion.credentials"));
    }

    /**
     * When sent to the remote node, send a proxy.
     */
    private Object writeReplace() {
        Channel c = Channel.current();
        return c==null ? this : c.export(RemotableSVNAuthenticationProvider.class, this);
    }

    private static final Logger LOGGER = Logger.getLogger(PerJobCredentialStore.class.getName());
}
