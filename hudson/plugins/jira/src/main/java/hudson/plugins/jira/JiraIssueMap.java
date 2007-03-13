package hudson.plugins.jira;

import hudson.util.KeyedDataStorage;

import java.io.IOException;

/**
 * Keeps track of {@link JiraIssue}s in Hudson.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JiraIssueMap extends KeyedDataStorage<JiraIssue,JiraSession> {

    private JiraIssueMap() {
    }

    protected JiraIssue load(String id) throws IOException {
        return JiraIssue.load(id);
    }

    protected JiraIssue create(String id, JiraSession session) throws IOException {
        return new JiraIssue(session.getIssue(id));
    }

    public static final JiraIssueMap theInstance = new JiraIssueMap();
}
