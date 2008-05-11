package hudson.plugins.clearcase.ucm;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Changelog entry for UCM ClearCase
 *
 * @author Henrik L. Hansen
 */
public class UcmActivity extends ChangeLogSet.Entry {
    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private String name;
    private String headline;
    private String stream;
    private String user;
    private String userName;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    /**
     * Overrides the setParent() method so the ClearCaseChangeLogSet can access it.
     */
    @Override
    public void setParent(ChangeLogSet parent) {
        super.setParent(parent);
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

        private Date date;
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

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getDateStr() {
            if (date == null) {
                return "";
            } else {
                return DATE_FORMATTER.format(date);
    }
}

        public void setDateStr(String date) {
            try {
                this.date = DATE_FORMATTER.parse(date);
            } catch (ParseException e) {
                //TODO: error handling
            }
        }
    }
}
