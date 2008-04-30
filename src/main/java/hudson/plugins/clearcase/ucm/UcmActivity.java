package hudson.plugins.clearcase.ucm;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Changelog entry for UCM ClearCase
 *
 * @author Henrik L. Hansen
 */
public class UcmActivity extends ChangeLogSet.Entry {

    private String name;
    private String headline;
    private String stream;
    private String user;
    private List<File> files = new ArrayList<File>();
    private List<UcmActivity> subActivities = new ArrayList<UcmActivity>();

    public UcmActivity() {
        // empty by design
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isIntegrationActivity() {
        if (name.startsWith("deliver.") || name.startsWith("rebase.")) {
            return true;
        } else {
            return false;
        }        
    }

    public void addFile(File file) {
        files.add(file);
    }

    public void addFiles(Collection<File> files) {
        this.files.addAll(files);
    }

    public List<File> getFiles() {
        return files;
    }

    public void addSubActivity(UcmActivity activity) {
        subActivities.add(activity);
    }

    public void addSubActivities(Collection<UcmActivity> activities) {
        this.subActivities.addAll(activities);
    }

    public List<UcmActivity> getSubActivities() {
        return subActivities;
    }

    @Override
    public String getMsg() {
        return headline;
    }

    @Override
    public User getAuthor() {
        return User.get(user);
    }

    @Override
    public Collection<String> getAffectedPaths() {
        Collection<String> paths = new ArrayList<String>(files.size());
        for (File file : files) {
            paths.add(file.getName());
        }
        return paths;
    }
        
    public static class File {

        private long date;
        private String name;
        private String version;
        private String operation;
        private String event; // can maybe be dumbed       

        private String comment;

        public String getEvent() {
            return event;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public long getDate() {
            return date;
        }

        public void setDate(long date) {
            this.date = date;
        }
    }
}
