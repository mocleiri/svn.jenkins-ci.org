package hudson.scm;

import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Subversion.
 *
 * @author Kohsuke Kawaguchi
 */
public class SubversionSCM extends AbstractCVSFamilySCM {
    private final String modules;
    private boolean useUpdate;

    SubversionSCM( String modules, boolean useUpdate ) {
        this.modules = modules;
        this.useUpdate = useUpdate;
    }

    public String getModules() {
        return modules;
    }

    public boolean isUseUpdate() {
        return useUpdate;
    }

    public boolean calcChangeLog(Build build, File changelogFile, BuildListener listener) throws IOException {
        // BASE:HEAD isn't very accurate
        // also the output format needs to be converted
        // return run(DESCRIPTOR.getSvnExe()+" log --xml --non-interactive -r BASE:HEAD",listener,build.getProject().getWorkspace());
        listener.getLogger().println("changelog is TBD");        
        return true;
    }

    public boolean checkout(File dir, BuildListener listener) throws IOException {
        if(useUpdate && isUpdatable(dir))
            return update(dir,listener);

        Util.deleteContentsRecursive(dir);

        return run(DESCRIPTOR.getSvnExe()+" co -q  "+modules,listener,dir);
    }

    public boolean update(File dir, BuildListener listener) throws IOException {
        String cmd = DESCRIPTOR.getSvnExe()+" update -q --non-interactive";
        StringTokenizer tokens = new StringTokenizer(modules);
        while(tokens.hasMoreTokens()) {
            if(!run(cmd,listener,new File(dir,getLastPathComponent(tokens.nextToken()))))
                return false;
        }
        return true;
    }

    /**
     * Returns true if we can use "cvs update" instead of "cvs checkout"
     */
    private boolean isUpdatable(File dir) {
        StringTokenizer tokens = new StringTokenizer(modules);
        while(tokens.hasMoreTokens()) {
            String url = tokens.nextToken();
            File module = new File(dir,getLastPathComponent(url));
            File svn = new File(module,".svn/entries");
            if(!svn.exists())
                return false;

            // check wc-entries/entry/@url
            synchronized(spf) {
                try {
                    SAXParser parser = spf.newSAXParser();
                    Checker checker = new Checker(url);
                    parser.parse(svn,checker);
                    if(!checker.found())
                        return false;
                } catch (ParserConfigurationException e) {
                    // impossible
                    throw new Error(e);
                } catch (SAXException e) {
                    // corrupt file? don't use update to be safe
                    return false;
                } catch (IOException e) {
                    // corrupt file? don't use update to be safe
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Looks for /wc-entries/entry/@url and see if it matches the expected URL.
     */
    private static final class Checker extends DefaultHandler {
        private final String url;

        private boolean matched = false;

        public Checker(String url) {
            this.url = url;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if(!qName.equals("entry"))
                return;

            String n = attributes.getValue("name");
            if(n==null || n.length()>0)     return;

            String url = attributes.getValue("url");
            if(url!=null && url.equals(this.url))
                matched = true;
        }

        public boolean found() {
            return matched;
        }
    }




    public SCMDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public void buildEnvVars(Map env) {
        // no environment variable
    }

    public String getModule() {
        String s;

        // if multiple URLs are specified, pick the first one
        int idx = modules.indexOf(' ');
        if(idx>=0)  s = modules.substring(0,idx);
        else        s = modules;

        return getLastPathComponent(s);
    }

    private String getLastPathComponent(String s) {
        String[] tokens = s.split("/");
        return tokens[tokens.length-1]; // return the last token
    }

    /**
     * Shared instance. Note that it is not namespace aware.
     */
    static final SAXParserFactory spf = SAXParserFactory.newInstance();

    static final Descriptor DESCRIPTOR = new Descriptor();

    public static final class Descriptor extends SCMDescriptor {
        Descriptor() {
            super(SubversionSCM.class);
        }

        public String getDisplayName() {
            return "Subversion";
        }

        public SCM newInstance(HttpServletRequest req) {
            return new SubversionSCM(
                req.getParameter("svn_modules"),
                req.getParameter("svn_use_update")!=null
            );
        }

        public String getSvnExe() {
            String value = (String)getProperties().get("svn_exe");
            if(value==null)
                value = "svn";
            return value;
        }

        public void setSvnExe(String value) {
            getProperties().put("svn_exe",value);
            save();
        }

        public boolean configure( HttpServletRequest req ) {
            setSvnExe(req.getParameter("svn_exe"));
            return true;
        }
    };
}
