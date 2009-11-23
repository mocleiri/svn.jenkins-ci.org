package hudson.plugins.perforce;

import java.util.*;

import hudson.scm.*;
import hudson.model.User;

/**
 * Perforce Implementation of {@link ChangeLogSet.Entry}.  This is a 1 to 1 mapping of
 * Perforce changelists.
 * <p>
 * Note: Internally, within the plugin we use an actual Perforce Change object in place of this.
 * 
 * @author Mike Wille
 */
public class PerforceChangeLogEntry extends ChangeLogSet.Entry {

    private Integer changeNumber;
    private java.util.Date date;
    private String description;
    private String user;
    private String workspace;
    private List<FileEntry> files;
    private List<JobEntry> jobs;

    public Integer getChangeNumber() {
        return changeNumber;
    }

    public void setChangeNumber(Integer changeNumber) {
        this.changeNumber = changeNumber;
    }

    public java.util.Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public List<FileEntry> getFiles() {
        return files;
    }

    public void setFiles(List<FileEntry> files) {
        this.files = files;
    }

    public List<JobEntry> getJobs() {
        return jobs;
    }

    public void setJobs(List<JobEntry> jobs) {
        this.jobs = jobs;
    }

    public PerforceChangeLogEntry(PerforceChangeLogSet parent) {
        super();
        setParent(parent);
    }
    
    @Override
    public User getAuthor() {
        return User.get(getUser());
    }

    @Override
    public Collection<String> getAffectedPaths() {
        List<String> paths = new ArrayList<String>(getFiles().size());
        for (FileEntry entry : getFiles()) {
            paths.add(entry.getFilename());
        }
        return paths;
    }

    @Override
    public String getMsg() {
        return getDescription();
    }

    public static class FileEntry {
        private String filename;
        private String revision;
        private String action;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getRevision() {
            return revision;
        }

        public void setRevision(String revision) {
            this.revision = revision;
        }
        public static class Action {
            public static final String EDIT = "EDIT";
            public static final String INTEGRATE = "INTEGRATE";
        }
    }

    public static class JobEntry {
        private String job;
        private String description;
        private String status;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getJob() {
            return job;
        }

        public void setJob(String job) {
            this.job = job;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
        
    }
        
}
