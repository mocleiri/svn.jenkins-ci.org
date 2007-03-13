package hudson.plugins.jira;

import com.thoughtworks.xstream.XStream;
import hudson.XmlFile;
import hudson.model.Hudson;
import hudson.plugins.jira.soap.RemoteIssue;
import hudson.util.XStream2;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One JIRA issue.
 *
 * <p>
 * This class is used to persist crucial issue information
 * so that Hudson can display it without talking to JIRA.
 *
 * @author Kohsuke Kawaguchi
 * @see JiraSite#getUrl(JiraIssue) 
 */
public final class JiraIssue implements Comparable<JiraIssue> {
    /**
     * JIRA ID, like "MNG-1235".
     */
    public final String id;

    /**
     * Title of the issue.
     * For example, in case of MNG-1235, this is "NPE In DiagnosisUtils while using tomcat plugin"
     */
    public final String title;

    /**
     * Instanciation is controlled by {@link JiraIssueMap}.
     */
    /*package*/ JiraIssue(RemoteIssue issue) throws IOException {
        this.id = issue.getKey();
        this.title = issue.getSummary();
        save();
    }

    public int compareTo(JiraIssue that) {
        return this.id.compareTo(that.id);
    }

    /**
     * Save the settings to a file.
     */
    public synchronized void save() throws IOException {
        XmlFile f = getFile(id);
        f.mkdirs();
        f.write(this);
    }

    /**
     * Loads the settings from a file if it exists.
     */
    /*package*/ static JiraIssue load(String id) throws IOException {
        XmlFile f = getFile(id);
        if(!f.exists())
            return null;
        try {
            return (JiraIssue)f.read();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load "+f,e);
            throw e;
        }
    }


    /**
     * Determines the file storage location from JIRA ID.
     */
    /*package*/ static XmlFile getFile(String id) {
        return new XmlFile( XSTREAM,
            new File( Hudson.getInstance().getRootDir(),"jira/"+id.replace('-','/')+".xml"));
    }

    private static final XStream XSTREAM = new XStream2();
    static {
        XSTREAM.alias("issue",JiraIssue.class);
    }

    private static final Logger LOGGER = Logger.getLogger(JiraIssue.class.getName());
}
