package hudson.scm.browsers;

import hudson.model.Descriptor;
import hudson.scm.CVSChangeLogSet.CVSChangeLog;
import hudson.scm.CVSChangeLogSet.File;
import hudson.scm.CVSChangeLogSet.Revision;
import hudson.scm.CVSRepositoryBrowser;
import hudson.scm.RepositoryBrowser;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * {@link RepositoryBrowser} for CVS.
 * @author Kohsuke Kawaguchi
 */
public final class ViewCVS extends CVSRepositoryBrowser {
    /**
     * The URL of the top of the site.
     *
     * Normalized to ends with '/', like <tt>http://isscvs.cern.ch/cgi-bin/viewcvs-all.cgi/</tt>
     * It may contain a query parameter like <tt>?cvsroot=foobar</tt>, so relative URL
     * construction needs to be done with care.
     */
    public final URL url;

    /**
     * @stapler-constructor
     */
    public ViewCVS(URL url) throws MalformedURLException {
        this.url = normalizeToEndWithSlash(url);
    }

    public URL getFileLink(File file) throws IOException {
        return new URL(url,trimHeadSlash(file.getFullName())+param());
    }

    public URL getDiffLink(File file) throws IOException {
        Revision r = new Revision(file.getRevision());
        Revision p = r.getPrevious();
        if(p==null) return null;

        return new URL(getFileLink(file), file.getSimpleName()+".diff"+param().add("r1="+p).add("r2="+r));
    }

    /**
     * No changeset support in ViewCVS.
     */
    public URL getChangeSetLink(CVSChangeLog changeSet) throws IOException {
        return null;
    }

    private QueryBuilder param() {
        return new QueryBuilder(url.getQuery());
    }

    public Descriptor<RepositoryBrowser<?>> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor<RepositoryBrowser<?>> DESCRIPTOR = new Descriptor<RepositoryBrowser<?>>(ViewCVS.class) {
        public String getDisplayName() {
            return "ViewCVS";
        }

        public ViewCVS newInstance(StaplerRequest req) throws FormException {
            return req.bindParameters(ViewCVS.class,"viewcvs.");
        }
    };

    private static final long serialVersionUID = 1L;
}
