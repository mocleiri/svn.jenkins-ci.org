package hudson.scm;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Util;
import static hudson.Util.fixEmpty;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.remoting.VirtualChannel;
import hudson.util.FormFieldValidator;
import hudson.util.Scrambler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc.xml.SVNXMLLogHandler;

import javax.servlet.ServletException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

/**
 * Subversion.
 *
 * Check http://svn.collab.net/repos/svn/trunk/subversion/svn/schema/ for
 * various output formats.
 *
 * @author Kohsuke Kawaguchi
 */
public class SubversionSCM extends AbstractCVSFamilySCM implements Serializable {
    private final String modules;
    private boolean useUpdate;
    private String username;

    /**
     * @deprecated
     *      No longer in use but left for serialization compatibility.
     */
    private transient String otherOptions;

    SubversionSCM(String modules, boolean useUpdate, String username) {
        StringBuilder normalizedModules = new StringBuilder();
        StringTokenizer tokens = new StringTokenizer(modules);
        while(tokens.hasMoreTokens()) {
            if(normalizedModules.length()>0)    normalizedModules.append(' ');
            String m = tokens.nextToken();
            if(m.endsWith("/"))
                // the normalized name is always without the trailing '/'
                m = m.substring(0,m.length()-1);
            normalizedModules.append(m);
       }

        this.modules = normalizedModules.toString();
        this.useUpdate = useUpdate;
        this.username = nullify(username);
    }

    /**
     * Whitespace-separated list of SVN URLs that represent
     * modules to be checked out.
     */
    public String getModules() {
        return modules;
    }

    public boolean isUseUpdate() {
        return useUpdate;
    }

    public String getUsername() {
        return username;
    }

    private Collection<String> getModuleDirNames() {
        List<String> dirs = new ArrayList<String>();
        StringTokenizer tokens = new StringTokenizer(modules);
        while(tokens.hasMoreTokens()) {
            dirs.add(getLastPathComponent(tokens.nextToken()));
        }
        return dirs;
    }

    private boolean calcChangeLog(AbstractBuild<?, ?> build, File changelogFile, BuildListener listener) throws IOException {
        if(build.getPreviousBuild()==null) {
            // nothing to compare against
            return createEmptyChangeLog(changelogFile, listener, "log");
        }

        PrintStream logger = listener.getLogger();

        Map<String,Long> previousRevisions = parseRevisionFile(build.getPreviousBuild());
        Map<String,Long> thisRevisions     = parseRevisionFile(build);

        boolean changelogFileCreated = false;

        SVNLogClient svnlc = createSvnClientManager(getDescriptor().createAuthenticationProvider()).getLogClient();

        TransformerHandler th = createTransformerHandler();
        th.setResult(new StreamResult(changelogFile));
        SVNXMLLogHandler logHandler = new SVNXMLLogHandler(th);
        logHandler.startDocument();


        StringTokenizer tokens = new StringTokenizer(modules);
        while(tokens.hasMoreTokens()) {
            String url = tokens.nextToken();
            Long prevRev = previousRevisions.get(url);
            if(prevRev==null) {
                logger.println("no revision recorded for "+url+" in the previous build");
                continue;
            }
            Long thisRev = thisRevisions.get(url);
            if(thisRev.equals(prevRev)) {
                logger.println("no change for "+url+" since the previous build");
                continue;
            }

            try {
                svnlc.doLog(SVNURL.parseURIEncoded(url),null,
                SVNRevision.create(prevRev), SVNRevision.create(prevRev+1),
                    SVNRevision.create(thisRev),
                    false, true, Long.MAX_VALUE, logHandler);
            } catch (SVNException e) {
                e.printStackTrace(listener.error("revision check failed on "+url));
            }
            changelogFileCreated = true;
        }

        if(changelogFileCreated) {
            logHandler.endDocument();
        }

        if(!changelogFileCreated)
            createEmptyChangeLog(changelogFile, listener, "log");

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

    /*package*/ static Map<String,Long> parseRevisionFile(AbstractBuild build) throws IOException {
        Map<String,Long> revisions = new HashMap<String,Long>(); // module -> revision
        {// read the revision file of the last build
            File file = getRevisionFile(build);
            if(!file.exists())
                // nothing to compare against
                return revisions;

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line=br.readLine())!=null) {
                int index = line.lastIndexOf('/');
                if(index<0) {
                    continue;   // invalid line?
                }
                try {
                    revisions.put(line.substring(0,index), Long.parseLong(line.substring(index+1)));
                } catch (NumberFormatException e) {
                    // perhaps a corrupted line. ignore
                }
            }
        }

        return revisions;
    }

    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, final BuildListener listener, File changelogFile) throws IOException, InterruptedException {
        boolean result;

        if(useUpdate && isUpdatable(workspace, listener)) {
            result = update(launcher,workspace,listener);
            if(!result)
                return false;
        } else {
            final ISVNAuthenticationProvider authProvider = getDescriptor().createAuthenticationProvider();
            result = workspace.act(new FileCallable<Boolean>() {
                public Boolean invoke(File ws, VirtualChannel channel) throws IOException {
                    Util.deleteContentsRecursive(ws);
                    SVNUpdateClient svnuc = createSvnClientManager(authProvider).getUpdateClient();
                    svnuc.setEventHandler(new SubversionUpdateEventHandler(listener));

                    StringTokenizer tokens = new StringTokenizer(modules);
                    while(tokens.hasMoreTokens()) {
                        try {
                            SVNURL url = SVNURL.parseURIEncoded(tokens.nextToken());
                            listener.getLogger().println("Checking out "+url);

                            svnuc.doCheckout(url, new File(ws, getLastPathComponent(url.getPath())), SVNRevision.HEAD, SVNRevision.HEAD, true );
                        } catch (SVNException e) {
                            e.printStackTrace(listener.error("Error in subversion"));
                            return false;
                        }
                    }

                    return true;
                }
            });
            if(!result)
                return false;
        }

        // write out the revision file
        PrintWriter w = new PrintWriter(new FileOutputStream(getRevisionFile(build)));
        try {
            Map<String,SvnInfo> revMap = buildRevisionMap(workspace, listener);
            for (Entry<String,SvnInfo> e : revMap.entrySet()) {
                w.println( e.getKey() +'/'+ e.getValue().revision );
            }
        } finally {
            w.close();
        }

        return calcChangeLog(build, changelogFile, listener);
    }

    /**
     * Creates {@link SVNClientManager}.
     *
     * <p>
     * This method must be executed on the slave where svn operations are performed.
     *
     * @param authProvider
     *      The value obtained from {@link DescriptorImpl#createAuthenticationProvider()}.
     *      If the operation runs on slaves,
     *      (and properly remoted, if the svn operations run on slaves.)
     */
    private SVNClientManager createSvnClientManager(ISVNAuthenticationProvider authProvider) {
        ISVNAuthenticationManager sam = SVNWCUtil.createDefaultAuthenticationManager();
        sam.setAuthenticationProvider(authProvider);
        return SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true),sam);
    }

    public static final class SvnInfo implements Serializable {
        /**
         * Decoded repository URL.
         */
        final String url;
        final long revision;

        public SvnInfo(String url, long revision) {
            this.url = url;
            this.revision = revision;
        }

        public SvnInfo(SVNInfo info) {
            this( info.getURL().toDecodedString(), info.getRevision().getNumber() );
        }

        public SVNURL getSVNURL() throws SVNException {
            return SVNURL.parseURIDecoded(url);
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Gets the SVN metadata for the given local workspace.
     *
     * @param workspace
     *      The target to run "svn info".
     */
    private SVNInfo parseSvnInfo(File workspace, ISVNAuthenticationProvider authProvider) throws SVNException {
        SVNWCClient svnWc = createSvnClientManager(authProvider).getWCClient();
        return svnWc.doInfo(workspace,SVNRevision.WORKING);
    }

    /**
     * Gets the SVN metadata for the remote repository.
     *
     * @param remoteUrl
     *      The target to run "svn info".
     */
    private SVNInfo parseSvnInfo(SVNURL remoteUrl, ISVNAuthenticationProvider authProvider) throws SVNException {
        SVNWCClient svnWc = createSvnClientManager(authProvider).getWCClient();
        return svnWc.doInfo(remoteUrl, SVNRevision.HEAD, SVNRevision.HEAD);
    }

    /**
     * Checks .svn files in the workspace and finds out revisions of the modules
     * that the workspace has.
     *
     * @return
     *      null if the parsing somehow fails. Otherwise a map from the repository URL to revisions.
     */
    private Map<String,SvnInfo> buildRevisionMap(FilePath workspace, final TaskListener listener) throws IOException, InterruptedException {
        final ISVNAuthenticationProvider authProvider = getDescriptor().createAuthenticationProvider();
        return workspace.act(new FileCallable<Map<String,SvnInfo>>() {
            public Map<String,SvnInfo> invoke(File ws, VirtualChannel channel) throws IOException {
                Map<String/*module name*/,SvnInfo> revisions = new HashMap<String,SvnInfo>();

                SVNWCClient svnWc = createSvnClientManager(authProvider).getWCClient();
                // invoke the "svn info"
                for( String module : getModuleDirNames() ) {
                    try {
                        SvnInfo info = new SvnInfo(svnWc.doInfo(new File(ws,module),SVNRevision.WORKING));
                        revisions.put(info.url,info);
                    } catch (SVNException e) {
                        e.printStackTrace(listener.error("Failed to parse svn info for "+module));
                    }
                }

                return revisions;
            }
        });
    }

    /**
     * Gets the file that stores the revision.
     */
    private static File getRevisionFile(AbstractBuild build) {
        return new File(build.getRootDir(),"revision.txt");
    }

    public boolean update(Launcher launcher, FilePath workspace, final BuildListener listener) throws IOException, InterruptedException {
        final ISVNAuthenticationProvider authProvider = getDescriptor().createAuthenticationProvider();
        return workspace.act(new FileCallable<Boolean>() {
            public Boolean invoke(File ws, VirtualChannel channel) throws IOException {
                SVNUpdateClient svnuc = createSvnClientManager(authProvider).getUpdateClient();
                svnuc.setEventHandler(new SubversionUpdateEventHandler(listener));

                StringTokenizer tokens = new StringTokenizer(modules);
                while(tokens.hasMoreTokens()) {
                    try {
                        String url = tokens.nextToken();
                        listener.getLogger().println("Updating "+url);
                        svnuc.doUpdate(new File(ws, getLastPathComponent(url)), SVNRevision.HEAD, true );
                    } catch (SVNException e) {
                        e.printStackTrace(listener.error("Error in subversion"));
                        return false;
                    }
                }
                return true;
            }
        });
    }

    /**
     * Returns true if we can use "svn update" instead of "svn checkout"
     */
    private boolean isUpdatable(FilePath workspace, final BuildListener listener) throws IOException, InterruptedException {
        final ISVNAuthenticationProvider authProvider = getDescriptor().createAuthenticationProvider();

        return workspace.act(new FileCallable<Boolean>() {
            public Boolean invoke(File ws, VirtualChannel channel) throws IOException {
                StringTokenizer tokens = new StringTokenizer(modules);
                while(tokens.hasMoreTokens()) {
                    String url = tokens.nextToken();
                    String moduleName = getLastPathComponent(url);
                    File module = new File(ws,moduleName);

                    if(!module.exists()) {
                        listener.getLogger().println("Checking out a fresh workspace because "+module+" doesn't exist");
                        return false;
                    }

                    try {
                        SvnInfo svnInfo = new SvnInfo(parseSvnInfo(module,authProvider));
                        if(!svnInfo.url.equals(url)) {
                            listener.getLogger().println("Checking out a fresh workspace because the workspace is not "+url);
                            return false;
                        }
                    } catch (SVNException e) {
                        listener.getLogger().println("Checking out a fresh workspace because Hudson failed to detect the current workspace "+module);
                        e.printStackTrace(listener.error(e.getMessage()));
                        return false;
                    }
                }
                return true;
            }
        });
    }

    public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener) throws IOException, InterruptedException {
        // current workspace revision
        Map<String,SvnInfo> wsRev = buildRevisionMap(workspace, listener);

        ISVNAuthenticationProvider authProvider = getDescriptor().createAuthenticationProvider();

        // check the corresponding remote revision
        for (SvnInfo localInfo : wsRev.values()) {
            try {
                SvnInfo remoteInfo = new SvnInfo(parseSvnInfo(localInfo.getSVNURL(),authProvider));
                listener.getLogger().println("Revision:"+remoteInfo.revision);
                if(remoteInfo.revision > localInfo.revision)
                    return true;    // change found
            } catch (SVNException e) {
                e.printStackTrace(listener.error("Failed to check repository revision for "+localInfo.url));
            }
        }

        return false; // no change
    }

    public ChangeLogParser createChangeLogParser() {
        return new SubversionChangeLogParser();
    }


    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }

    public void buildEnvVars(Map<String,String> env) {
        // no environment variable
    }

    public FilePath getModuleRoot(FilePath workspace) {
        String s;

        // if multiple URLs are specified, pick the first one
        int idx = modules.indexOf(' ');
        if(idx>=0)  s = modules.substring(0,idx);
        else        s = modules;

        return workspace.child(getLastPathComponent(s));
    }

    private static String getLastPathComponent(String s) {
        String[] tokens = s.split("/");
        return tokens[tokens.length-1]; // return the last token
    }

    public static final class DescriptorImpl extends Descriptor<SCM> {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

        /**
         * Path to <tt>svn.exe</tt>. Null to default.
         *
         * @deprecated
         *      No longer in use.
         */
        private volatile String svnExe;

        /**
         * SVN authentication realm to its associated credentials.
         */
        private final Map<String,Credential> credentials = new Hashtable<String,Credential>();

        /**
         * Stores {@link SVNAuthentication} for a single realm.
         */
        private static abstract class Credential {
            abstract SVNAuthentication createSVNAuthentication();
        }

        private static final class PasswordCredential extends Credential {
            private final String userName;
            private final String password; // scrambled by base64

            public PasswordCredential(String userName, String password) {
                this.userName = userName;
                this.password = Scrambler.scramble(password);
            }

            @Override
            SVNPasswordAuthentication createSVNAuthentication() {
                return new SVNPasswordAuthentication(userName,Scrambler.descramble(password),false);
            }
        }

        /**
         * See {@link DescriptorImpl#createAuthenticationProvider()}.
         */
        private class SVNAuthenticationProviderImpl implements ISVNAuthenticationProvider, Serializable {
            public SVNAuthentication requestClientAuthentication(String kind, SVNURL url, String realm, SVNErrorMessage errorMessage, SVNAuthentication previousAuth, boolean authMayBeStored) {
                Credential cred = credentials.get(realm);
                if(cred==null)  return null;
                return cred.createSVNAuthentication();
            }

            public int acceptServerAuthentication(SVNURL url, String realm, Object certificate, boolean resultMayBeStored) {
                return ACCEPTED_TEMPORARY;
            }

            /**
             * When sent to the remote node, send a proxy.
             */
            private Object writeReplace() {
                return Channel.current().export(ISVNAuthenticationProvider.class, this);
            }
        }

        private DescriptorImpl() {
            super(SubversionSCM.class);
            load();
        }

        public String getDisplayName() {
            return "Subversion";
        }

        public SCM newInstance(StaplerRequest req) {
            return new SubversionSCM(
                req.getParameter("svn_modules"),
                req.getParameter("svn_use_update")!=null,
                req.getParameter("svn_username")
            );
        }

        /**
         * Creates {@link ISVNAuthenticationProvider} backed by {@link #credentials}.
         * This method must be invoked on the master, but the returned object is remotable.
         */
        public ISVNAuthenticationProvider createAuthenticationProvider() {
            return new SVNAuthenticationProviderImpl();
        }

        /**
         * Used in the job configuration page to check if authentication for the SVN URLs
         * are available.
         */
        public void doAuthenticationCheck(final StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator(req,rsp,true) {
                protected void check() throws IOException, ServletException {
                    StringTokenizer tokens = new StringTokenizer(fixEmpty(request.getParameter("value")));
                    String message="";

                    while(tokens.hasMoreTokens()) {
                        String url = tokens.nextToken();

                        try {
                            SVNRepository repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(url));
                            repository.testConnection();
                        } catch (SVNException e) {
                            message += "Unable to access "+url+" : "+e.getErrorMessage();
                            if(e.getErrorMessage().getErrorCode().equals(SVNErrorCode.RA_NOT_AUTHORIZED))
                                message += "(<a href='"+req.getContextPath()+"/scm/SubversionSCM/enterCredential?"+url+"'>enter credential</a>)";
                            message += "<br>";
                        }
                    }

                    if(message.length()==0)
                        ok();
                    else
                        error(message);
                }
            }.process();
        }

        /**
         * Submits the authentication info.
         */
        public void doPostCredential(final StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            final String url = req.getParameter("url");
            final String username = req.getParameter("username");
            final String password = req.getParameter("password");

            try {
                // the way it works with SVNKit is that
                // 1) svnkit calls AuthenticationManager asking for a credential.
                //    this is when we can see the 'realm', which identifies the user domain.
                // 2) DefaultSVNAuthenticationManager returns the username and password we set below
                // 3) if the authentication is successful, svnkit calls back acknowledgeAuthentication
                //    (so we store the password info here)
                SVNRepository repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(url));
                repository.setAuthenticationManager(new DefaultSVNAuthenticationManager(SVNWCUtil.getDefaultConfigurationDirectory(),true,username,password) {
                    public void acknowledgeAuthentication(boolean accepted, String kind, String realm, SVNErrorMessage errorMessage, SVNAuthentication authentication) throws SVNException {
                        if(accepted) {
                            credentials.put(realm,new PasswordCredential(username,password));
                            save();
                        }
                        super.acknowledgeAuthentication(accepted, kind, realm, errorMessage, authentication);
                    }
                });
                repository.testConnection();
                rsp.sendRedirect("credentialOK");
            } catch (SVNException e) {
                req.setAttribute("message",e.getErrorMessage());
                rsp.forward(Hudson.getInstance(),"error",req);
            }
        }
    }

    private static final long serialVersionUID = 1L;

    static {
        DAVRepositoryFactory.setup();   // http, https
        SVNRepositoryFactoryImpl.setup();   // svn, svn+xxx
        FSRepositoryFactory.setup();    // file
    }
}
