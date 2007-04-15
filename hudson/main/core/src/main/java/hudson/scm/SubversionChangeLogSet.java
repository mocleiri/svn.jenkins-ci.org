package hudson.scm;

import hudson.model.AbstractBuild;
import hudson.model.User;
import hudson.scm.SubversionChangeLogSet.LogEntry;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.AbstractList;

/**
 * {@link ChangeLogSet} for Subversion.
 *
 * @author Kohsuke Kawaguchi
 */
public final class SubversionChangeLogSet extends ChangeLogSet<LogEntry> {
    private final List<LogEntry> logs;

    /**
     * @GuardedBy this
     */
    private Map<String,Long> revisionMap;

    /*package*/ SubversionChangeLogSet(AbstractBuild build, List<LogEntry> logs) {
        super(build);
        this.logs = Collections.unmodifiableList(logs);
        for (LogEntry log : logs)
            log.setParent(this);
    }

    public boolean isEmptySet() {
        return logs.isEmpty();
    }

    public List<LogEntry> getLogs() {
        return logs;
    }


    public Iterator<LogEntry> iterator() {
        return logs.iterator();
    }

    public synchronized Map<String,Long> getRevisionMap() throws IOException {
        if(revisionMap==null)
            revisionMap = SubversionSCM.parseRevisionFile(build);
        return revisionMap;
    }

    /**
     * One commit.
     * <p>
     * Setter methods are public only so that the objects can be constructed from Digester.
     * So please consider this object read-only.
     */
    public static class LogEntry extends ChangeLogSet.Entry {
        private int revision;
        private User author;
        private String date;
        private String msg;
        private List<Path> paths = new ArrayList<Path>();

        /**
         * Gets the {@link SubversionChangeLogSet} to which this change set belongs.
         */
        public SubversionChangeLogSet getParent() {
            return (SubversionChangeLogSet)super.getParent();
        }

        /**
         * Gets the revision of the commit.
         *
         * <p>
         * If the commit made the repository revision 1532, this
         * method returns 1532.
         */
        @Exported
        public int getRevision() {
            return revision;
        }

        public void setRevision(int revision) {
            this.revision = revision;
        }

        @Override
        public User getAuthor() {
            return author;
        }

        @Override
        public Collection<String> getAffectedPaths() {
            return new AbstractList<String>() {
                public String get(int index) {
                    return paths.get(index).value;
                }
                public int size() {
                    return paths.size();
                }
            };
        }

        public void setUser(String author) {
            this.author = User.get(author);
        }

        @Exported
        public String getUser() {// digester wants read/write property, even though it never reads. Duh.
            return author.getDisplayName();
        }

        @Exported
        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        @Override @Exported
        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public void addPath( Path p ) {
            p.entry = this;
            paths.add(p);
        }

        /**
         * Gets the files that are changed in this commit.
         * @return
         *      can be empty but never null.
         */
        @Exported
        public List<Path> getPaths() {
            return paths;
        }
    }

    /**
     * A file in a commit.
     * <p>
     * Setter methods are public only so that the objects can be constructed from Digester.
     * So please consider this object read-only.
     */
    @ExportedBean(defaultVisibility=999)
    public static class Path {
        private LogEntry entry;
        private char action;
        private String value;

        /**
         * Gets the {@link LogEntry} of which this path is a member.
         */
        public LogEntry getLogEntry() {
            return entry;
        }

        public void setAction(String action) {
            this.action = action.charAt(0);
        }

        /**
         * Path in the repository. Such as <tt>/test/trunk/foo.c</tt>
         */
        @Exported(name="file")
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Exported
        public EditType getEditType() {
            if( action=='A' )
                return EditType.ADD;
            if( action=='D' )
                return EditType.DELETE;
            return EditType.EDIT;
        }
    }
}
