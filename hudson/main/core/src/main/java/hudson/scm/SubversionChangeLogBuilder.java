package hudson.scm;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import hudson.FilePath.FileCallable;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.xml.SVNXMLLogHandler;
import org.xml.sax.helpers.LocatorImpl;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import java.io.IOException;
import java.io.PrintStream;
import java.io.File;
import java.util.Map;
import java.util.Collection;

/**
 * Builds <tt>changelog.xml</tt> for {@link SubversionSCM}.
 *
 * @author Kohsuke Kawaguchi
 */
final class SubversionChangeLogBuilder {
    /**
     * Revisions of the workspace before the update/checkout.
     */
    private final Map<String,Long> previousRevisions;
    /**
     * Revisions of the workspace after the update/checkout.
     */
    private final Map<String,Long> thisRevisions;

    private final BuildListener listener;
    private final SubversionSCM scm;
    private final AbstractBuild<?,?> build;

    public SubversionChangeLogBuilder(AbstractBuild<?,?> build, BuildListener listener, SubversionSCM scm) throws IOException {
        previousRevisions = SubversionSCM.parseRevisionFile(build.getPreviousBuild());
        thisRevisions     = SubversionSCM.parseRevisionFile(build);
        this.listener = listener;
        this.scm = scm;
        this.build = build;

    }

    public boolean run(Collection<String> externals, Result changeLog) throws IOException, InterruptedException {
        boolean changelogFileCreated = false;

        SVNLogClient svnlc = SubversionSCM.createSvnClientManager(createAuthenticationProvider()).getLogClient();
        TransformerHandler th = createTransformerHandler();
        th.setResult(changeLog);
        SVNXMLLogHandler logHandler = new SVNXMLLogHandler(th);
        // work around for http://svnkit.com/tracker/view.php?id=175
        th.setDocumentLocator(DUMMY_LOCATOR);
        logHandler.startDocument();

        for (ModuleLocation l : scm.getLocations()) {
            changelogFileCreated |= buildModule(l.remote, svnlc, logHandler);
        }
        for(String path : externals) {
            changelogFileCreated |= buildModule(
                getUrlForPath(build.getProject().getWorkspace().child(path)), svnlc, logHandler);
        }

        if(changelogFileCreated) {
            logHandler.endDocument();
        }

        return changelogFileCreated;
    }

    private String getUrlForPath(FilePath path) throws IOException, InterruptedException {
        return path.act(new GetUrlForPath(createAuthenticationProvider()));
    }

    private ISVNAuthenticationProvider createAuthenticationProvider() {
        return SubversionSCM.DescriptorImpl.DESCRIPTOR.createAuthenticationProvider();
    }

    private boolean buildModule(String url, SVNLogClient svnlc, SVNXMLLogHandler logHandler) {
        PrintStream logger = listener.getLogger();
        Long prevRev = previousRevisions.get(url);
        if(prevRev==null) {
            logger.println("no revision recorded for "+url+" in the previous build");
            return false;
        }
        Long thisRev = thisRevisions.get(url);
        if (thisRev == null) {
        	listener.error("No revision found for URL: " + url + " in " + SubversionSCM.getRevisionFile(build) + ". Revision file contains: " + thisRevisions.keySet());
        	return true;
        }
        if(thisRev.equals(prevRev)) {
            logger.println("no change for "+url+" since the previous build");
            return false;
        }

        try {
            svnlc.doLog(SVNURL.parseURIEncoded(url),null,
                SVNRevision.UNDEFINED, SVNRevision.create(prevRev+1),
                SVNRevision.create(thisRev),
                false, true, Long.MAX_VALUE, logHandler);
        } catch (SVNException e) {
            e.printStackTrace(listener.error("revision check failed on "+url));
        }
        return true;
    }

    /**
     * Creates an identity transformer.
     */
    private static TransformerHandler createTransformerHandler() {
        try {
            return ((SAXTransformerFactory) SAXTransformerFactory.newInstance()).newTransformerHandler();
        } catch (TransformerConfigurationException e) {
            throw new Error(e); // impossible
        }
    }

    private static final LocatorImpl DUMMY_LOCATOR = new LocatorImpl();

    static {
        DUMMY_LOCATOR.setLineNumber(-1);
        DUMMY_LOCATOR.setColumnNumber(-1);
    }

    private static class GetUrlForPath implements FileCallable<String> {
        private final ISVNAuthenticationProvider authProvider;

        public GetUrlForPath(ISVNAuthenticationProvider authProvider) {
            this.authProvider = authProvider;
        }

        public String invoke(File p, VirtualChannel channel) throws IOException {
            SVNWCClient svnwc = SubversionSCM.createSvnClientManager(authProvider).getWCClient();

            SVNInfo info;
            try {
                info = svnwc.doInfo(p, SVNRevision.WORKING);
                return info.getURL().toDecodedString();
            } catch (SVNException e) {
                e.printStackTrace();
                return null;
            }
        }

        private static final long serialVersionUID = 1L;
    }
}
