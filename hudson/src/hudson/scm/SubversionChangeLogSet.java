package hudson.scm;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * {@link ChangeLogSet} for Subversion.
 *
 * @author Kohsuke Kawaguchi
 */
public final class SubversionChangeLogSet extends ChangeLogSet {
    private final List<LogEntry> logs;

    public SubversionChangeLogSet(List<LogEntry> logs) {
        this.logs = Collections.unmodifiableList(logs);
    }

    public boolean isEmptySet() {
        return logs.isEmpty();
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    /**
     * One commit.
     */
    public static class LogEntry extends Entry {
        private int revision;
        private String author;
        private String date;
        private String msg;
        private List<Path> paths = new ArrayList<Path>();

        public int getRevision() {
            return revision;
        }

        public void setRevision(int revision) {
            this.revision = revision;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public void addPath( Path p ) {
            paths.add(p);
        }

        public List<Path> getPaths() {
            return paths;
        }
    }

    public static class Path {
        private char action;
        private String value;

        public void setAction(String action) {
            this.action = action.charAt(0);
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public EditType getEditType() {
            if( action=='A' )
                return EditType.ADD;
            if( action=='D' )
                return EditType.DELETE;
            return EditType.EDIT;
        }
    }
}
