package hudson.scm;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.model.LargeText;
import hudson.scm.SubversionSCM.SvnInfo;
import hudson.util.CopyOnWriteMap;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map.Entry;
import java.lang.ref.WeakReference;

/**
 * {@link Action} that lets people create tag for the given build.
 * 
 * @author Kohsuke Kawaguchi
 */
public class SubversionTagAction extends AbstractScmTagAction {

    /**
     * Map is from the repository URL to the URLs of tags.
     * If a module is not tagged, the value will be empty set.
     */
    private final Map<SvnInfo,List<String>> tags = new CopyOnWriteMap.Tree<SvnInfo, List<String>>();

    /*package*/ SubversionTagAction(AbstractBuild build,Collection<SvnInfo> svnInfos) {
        super(build);
        Map<SvnInfo,List<String>> m = new HashMap<SvnInfo,List<String>>();
        for (SvnInfo si : svnInfos)
            m.put(si,new ArrayList<String>());
        tags.putAll(m);
    }

    public String getIconFileName() {
        if(tags==null && !Hudson.isAdmin())
            return null;
        return "save.gif";
    }

    public String getDisplayName() {
        int nonNullTag = 0;
        for (List<String> v : tags.values()) {
            if(!v.isEmpty()) {
                nonNullTag++;
                if(nonNullTag>1)
                    break;
            }
        }
        if(nonNullTag==0)
            return "Tag this build";
        if(nonNullTag==1)
            return "Subversion tag";
        else
            return "Subversion tags";
    }

    /**
     * @see #tags
     */
    public Map<SvnInfo,List<String>> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    /**
     * Returns true if this build has already been tagged at least once.
     */
    public boolean isTagged() {
        for (List<String> t : tags.values()) {
            if(!t.isEmpty())    return true;
        }
        return false;
    }

    private static final Pattern TRUNK_BRANCH_MARKER = Pattern.compile("/(trunk|branches)(/|$)");

    /**
     * Creates a URL, to be used as the default value of the module tag URL.
     *
     * @return
     *      null if failed to guess.
     */
    public String makeTagURL(SvnInfo si) {
        // assume the standard trunk/branches/tags repository layout
        Matcher m = TRUNK_BRANCH_MARKER.matcher(si.url);
        if(!m.find())
            return null;    // doesn't have 'trunk' nor 'branches'

        return si.url.substring(0,m.start())+"/tags/"+build.getProject().getName()+"-"+build.getNumber();
    }

    /**
     * Invoked to actually tag the workspace.
     */
    public synchronized void doSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        if(!Hudson.adminCheck(req,rsp))
            return;

        Map<SvnInfo,String> newTags = new HashMap<SvnInfo,String>();

        int i=-1;
        for (SvnInfo e : tags.keySet()) {
            i++;
            if(tags.size()>1 && req.getParameter("tag"+i)==null)
                continue; // when tags.size()==1, UI won't show the checkbox.
            newTags.put(e,req.getParameter("name" + i));
        }

        new TagWorkerThread(newTags).start();

        rsp.sendRedirect(".");
    }

    /**
     * The thread that performs tagging operation asynchronously.
     */
    public final class TagWorkerThread extends AbstractTagWorkerThread {
        private final Map<SvnInfo,String> tagSet;

        public TagWorkerThread(Map<SvnInfo, String> tagSet) {
            this.tagSet = tagSet;
        }

        public synchronized void start() {
            SubversionTagAction.this.workerThread = this;
            SubversionTagAction.this.log = new WeakReference<LargeText>(text);
            super.start();
        }

        @Override
        protected void perform(TaskListener listener) {
            try {
                final SVNClientManager cm = SubversionSCM.createSvnClientManager(SubversionSCM.DescriptorImpl.DESCRIPTOR.createAuthenticationProvider());
                try {
                    for (Entry<SvnInfo, String> e : tagSet.entrySet()) {
                        PrintStream logger = listener.getLogger();
                        logger.println("Tagging "+e.getKey()+" to "+e.getValue());

                        try {
                            SVNURL src = SVNURL.parseURIDecoded(e.getKey().url);
                            SVNURL dst = SVNURL.parseURIDecoded(e.getValue());

                            SVNCopyClient svncc = cm.getCopyClient();
                            svncc.doCopy(src, SVNRevision.create(e.getKey().revision), dst, false, true, "Tagged from "+build );
                        } catch (SVNException x) {
                            x.printStackTrace(listener.error("Failed to tag"));
                            return;
                        }
                    }

                    // completed successfully
                    for (Entry<SvnInfo,String> e : tagSet.entrySet())
                        SubversionTagAction.this.tags.get(e.getKey()).add(e.getValue());
                    build.save();
                    workerThread = null;
                } finally {
                    cm.dispose();
                }
           } catch (Throwable e) {
               e.printStackTrace(listener.fatalError(e.getMessage()));
           }
        }
    }
}
