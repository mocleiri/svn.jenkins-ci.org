package hudson.scm;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * {@link ChangeLogSet} for CVS.
 * @author Kohsuke Kawaguchi
 */
public final class CVSChangeLogSet extends ChangeLogSet {
    private List<CVSChangeLog> logs;

    public CVSChangeLogSet(List<CVSChangeLog> logs) {
        this.logs = Collections.unmodifiableList(logs);
    }

    /**
     * Returns the read-only list of changes.
     */
    public List<CVSChangeLog> getLogs() {
        return logs;
    }

    @Override
    public boolean isEmptySet() {
        return logs.isEmpty();
    }

    public static CVSChangeLogSet parse( java.io.File f ) throws IOException, SAXException {
        return new CVSChangeLogSet(CVSChangeLog.parse(f));
    }

    public static final CVSChangeLogSet EMPTY = new CVSChangeLogSet(Collections.EMPTY_LIST);
}
