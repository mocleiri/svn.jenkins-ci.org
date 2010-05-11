package hudson.plugins.collabnet.documentuploader;


import com.collabnet.ce.soap50.webservices.docman.DocumentSoapDO;
import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.DocumentApp;
import com.collabnet.ce.webservices.FileStorageApp;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.collabnet.AbstractTeamForgeNotifier;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.ComboBoxUpdater;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepMonitor;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Hudson plugin to upload the Hudson build log 
 * to the CollabNet Documents.
 */
public class CNDocumentUploader extends AbstractTeamForgeNotifier {
    private static Logger logger = Logger.getLogger("CNDocumentUploader");
    private static final String IMAGE_URL = "/plugin/collabnet/images/48x48/";
	
    // listener is used for logging and will only be
    // set at the beginning of perform.
    private transient BuildListener listener = null;

    // collabNet object
    private transient CollabNetApp cna = null;

    // Variables from the form
    private String uploadPath;
    private String description;
    private FilePattern[] file_patterns;
    private boolean includeBuildLog;    

    /**
     * Creates a new CNDocumentUploader object.
     *
     * @param project where the build log will be uploaded.
     * @param uploadPath on the CollabNet server, where the build log should
     *                   be uploaded.
     * @param description
     * @param filePatterns
     * @param includeBuildLog
     */
    @DataBoundConstructor
    public CNDocumentUploader(ConnectionFactory connectionFactory,
                              String project, String uploadPath, 
                              String description, FilePattern[] filePatterns,
                              boolean includeBuildLog) {
        super(connectionFactory,project);
        this.uploadPath = uploadPath;
        this.description = description;
        this.file_patterns = filePatterns;
        this.includeBuildLog = includeBuildLog;
    }

    /**
     * Setting the listener allows logging to work.
     *
     * @param listener passed into the perform method.
     */
    private void setupLogging(BuildListener listener) {
        this.listener = listener;
    }

    /**
     * Log a message to the console.  Logging will only work once the 
     * listener is set. Otherwise, it will fail (silently).
     *
     * @param message A string to print to the console.
     */
    private void logConsole(String message) {
        if (this.listener != null) {
            message = "CollabNet Document Uploader: " + message;
            this.listener.getLogger().println(message);
        }
    }


    /**
     * Log a message to the console, including stack trace.  Logging will only work once the 
     * listener is set. Otherwise, it will fail (silently).
     *
     * @param message A string to print to the console.
     * @param exception the exception containing the stack trace to log
     */
    private void logConsole(String message, Exception exception) {
        if (this.listener != null) {
            message = "CollabNet Document Uploader: " + message;
            this.listener.getLogger().println(message);

            // now print the stack trace
            exception.printStackTrace(this.listener.error("error"));
        }
    }

    
    /**
     * Convenience method to log RemoteExceptions.  
     *
     * @param methodName in progress on when this exception occurred.
     * @param re The RemoteException that was thrown.
     */
    private void log(String methodName, RemoteException re) {
        CommonUtil.logRE(logger, methodName, re);
    }

    /**
     * @return the path where the build log is uploaded.
     */
    public String getUploadPath() {
        return this.uploadPath;
    }

    /**
     * @return the description of the uploaded files.
     */
    public String getDescription() {
        return this.description;
    }
    
    /**
     * @return the ant-style file patterns.
     */
    public FilePattern[] getFilePatterns() {
        if (this.file_patterns != null) {
            return this.file_patterns;
        } else {
            return new FilePattern[0];
        }
    }

    /**
     * @return true if the build log should be uploaded.
     */
    public boolean getIncludeBuildLog() {
        return this.includeBuildLog;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * The function does the work of uploading the build log.
     *
     * @param build current Hudson build.
     * @param launcher unused.
     * @param listener receives events that happen during a build.  We use it 
     *                 for logging.
     * @return true if successful, false if a critical error occurred.
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, 
                           BuildListener listener) throws IOException, InterruptedException {
        this.setupLogging(listener);
        this.cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(), 
                                                this.getUsername(), 
                                                this.getPassword());
        if (this.cna == null) {
            this.logConsole("Critical Error: login to " + this.getCollabNetUrl() +
                     " failed.  Setting build status to UNSTABLE (or worse).");
            Result previousBuildStatus = build.getResult();
            build.setResult(previousBuildStatus.combine(Result.UNSTABLE));
            build.addAction(this.createAction(0, null));
            return false;
        }
        String projectId = this.getProjectId();
        if (projectId == null) {
            this.logConsole("Critical Error: Unable to find project '" +
                     this.getProject() + "'.  " + 
                     "Setting build status to UNSTABLE (or worse).");
            Result previousBuildStatus = build.getResult();
            build.setResult(previousBuildStatus.combine(Result.UNSTABLE));
            build.addAction(this.createAction(0, null));
            this.logoff();
            return false;
        }
        DocumentApp da = new DocumentApp(this.cna);
        String folderId = null;
        String path = this.getInterpreted(build, this.getUploadPath());
        try {
            folderId = da.findOrCreatePath(projectId, path);
        } catch (RemoteException re) {
            this.log("findOrCreatePath", re);
            // if this fails, cannot continue
            this.logConsole("Critical Error: Unable to create a path for '" +
                     path + "'.  Setting build status to " +
                     "UNSTABLE (or worse).");
            Result previousBuildStatus = build.getResult();
            build.setResult(previousBuildStatus.combine(Result.UNSTABLE));
            build.addAction(this.createAction(0, folderId));
            this.logoff();
            return false;
        }
        int numUploaded = this.uploadFiles(folderId, build, listener);
        build.addAction(this.createAction(numUploaded, folderId));
        try {
            this.cna.logoff();
        } catch (RemoteException re) {
            this.log("logoff", re);
        }
        return true;
    }

    private Action createAction(int numUploaded, String folderId) {
        String displaymsg = "Download from CollabNet Documents";
        return new CnduResultAction(displaymsg, 
                                    IMAGE_URL + "cn-icon.gif", 
                                    "console", 
                                    this.getFolderUrl(folderId), 
                                    numUploaded);
    }

    /**
     * @param folderId
     * @return the url pointing to this folder.
     */
    private String getFolderUrl(String folderId) {
        String path = null;
        if (folderId != null) {
            try {
                DocumentApp da = new DocumentApp(this.cna);
                path = da.getFolderPath(folderId);
            } catch (RemoteException re) {
                this.log("getFolderUrl", re);
            }
        }
        if (path == null) {
            return this.getCollabNetUrl();
        } else {
            return this.getCollabNetUrl() + "/sf/docman/do/listDocuments/" 
                + path;
        }
    }

    /**
     * Upload files matching the file patterns to the Document Service.
     *
     * @param folderId folder where the files should be uploaded.
     * @param build the current Hudson build.
     * @return the number of files successfully uploaded.
     */
    public int uploadFiles(String folderId, AbstractBuild<?, ?> build, BuildListener listener)
            throws IOException, InterruptedException {
        int numUploaded = 0;
        String path = this.getInterpreted(build, this.getUploadPath());
        this.logConsole("Uploading files to project '" + this.getProject() +
                 "', folder '" + path + "' on host '" + 
                 this.getCollabNetUrl() + "' as user '" + this.getUsername() 
                 + "'.");
        for (FilePattern uninterp_fp : this.getFilePatterns()) {
            String file_pattern;
            try {
                file_pattern = uninterp_fp.interpret(build,listener);
            } catch (IllegalArgumentException e) {
                this.logConsole("File pattern " + uninterp_fp + " contained a bad env var.  Skipping.");
                continue;
            }
            if (file_pattern.equals("")) {
                // skip empty fields
                continue;
            }

            FilePath[] filePaths = this.getFilePaths(build, file_pattern);
            for (FilePath uploadFilePath : filePaths) {
                String fileId = this.uploadFile(uploadFilePath);
                if (fileId == null) {
                    this.logConsole("Failed to upload " + uploadFilePath.getName() + ".");
                    continue;
                }
                try {
                    String docId = this.updateOrCreateDoc(folderId, fileId,
                                                   uploadFilePath.getName(),
                                                   CNDocumentUploader.
                                                   getMimeType(uploadFilePath),
                                                   build);
                    this.logConsole("Uploaded " + uploadFilePath.getName() + " -> "
                             + this.getDocUrl(docId));
                    numUploaded++;
                } catch (RemoteException re) {
                    logConsole("Upload file failed: " + re.getMessage());
                    this.log("updateOrCreateDoc", re);
                }
            }
        }
        if (this.getIncludeBuildLog()) {
            String fileId = this.uploadBuildLog(build);
            if (fileId == null) {
                this.logConsole("Failed to upload " + build.getLogFile().getName() + ".");
            } else {
                try {
                    String docId = this.updateOrCreateDoc(folderId, fileId,
                                                   build.getLogFile().getName(),
                                                   CNDocumentUploader.
                                                   getMimeType(build.
                                                               getLogFile()),
                                                   build);
                    this.logConsole("Uploaded " + build.getLogFile().getName() + " -> " + this.getDocUrl(docId));
                    numUploaded++;
                } catch (RemoteException re) {
                    logConsole("Upload log failed: " + re.getMessage(), re);
                    this.log("updateOrCreateDoc", re);
                }
            }
        }
        return numUploaded;
    }

    /**
     * Get the document's URL.
     *
     * @param docId document's id.
     * @return an absolute URL to the document.
     */
    private String getDocUrl(String docId) {
        return this.getCollabNetUrl() + "/sf/go/" + docId;
    }

    /**
     * Return the filepaths in the workspace which match the pattern.
     *
     * @param build The hudson build.
     * @param pattern An ant-style pattern.
     * @return an array of FilePaths which match this pattern in the 
     *         hudson workspace.
     */
    private FilePath[] getFilePaths(AbstractBuild<?, ?> build, 
                                    String pattern) {
        FilePath workspace;
        if (FreeStyleProject.class.isInstance(build.getProject())) { // generic instanceof causes compilation error
            // our standard project
            workspace = build.getWorkspace();
        } else {
            // promoted build - use the project's workspace, since the build doesn't always account for custom workspace
            // may be a bug with promoted build?
            workspace = build.getProject().getRootProject().getWorkspace();
        }

        String logEntry = "Searching ant pattern '" + pattern + "'";
        FilePath[] uploadFilePaths = new FilePath[0];
        try {
            uploadFilePaths = workspace.list(pattern);
            logEntry += " in " + workspace.absolutize().getRemote();
        } catch (IOException ioe) {
            this.logConsole("Could not list workspace due to IOException: "
                     + ioe.getMessage());
        } catch (InterruptedException ie) {
            this.logConsole("Could not list workspace due to " +
                     "InterruptedException: " + ie.getMessage());
        }
        logEntry += " : found " + uploadFilePaths.length + " entry(ies)";
        logConsole(logEntry);
        return uploadFilePaths;
    }

    /**
     * If a document exists under this folder with the fileName, 
     * increment it's version and update with the new fileId.
     * Otherwise, create a new document.
     *
     * @param folderId of the folder where the document will be 
     *                 created/updated. 
     * @param fileId of the already upload build log.
     * @param fileName name of the uploaded file.
     * @param mimeType of the uploaded file.
     * @param build the current Hudson build.
     * @return the docId associated with the new/updated document.
     * @throws RemoteException
     */
    private String updateOrCreateDoc(String folderId, String fileId, 
                                     String fileName, String mimeType, 
                                     AbstractBuild<?, ?> build) 
        throws RemoteException, IOException, InterruptedException {
        DocumentApp da = new DocumentApp(this.cna);
        String docId = da.findDocumentId(folderId, fileName);
        if (docId != null) {
            da.updateDoc(docId, fileId);
        } else {
            DocumentSoapDO document = da.
                createDocument(folderId, fileName, 
                               this.getInterpreted(build, 
                                                   this.getDescription()), 
                               null, "final", false, fileName, 
                               mimeType, fileId, null, null);
            docId = document.getId();
        }
        return docId;
    }

    /**
     * Get the mimetype for the file.
     *
     * @param filePath The filePath to return the mimetype for.
     * @return the string representing the mimetype of the file.
     */
    public static String getMimeType(FilePath filePath) {
        String mimeType = "text/plain";
        try {
            mimeType = filePath.act(new FileCallable<String>() {
                @Override
                public String invoke(File f, VirtualChannel channel) 
                throws IOException, RemoteException {
                    if (f.getName().endsWith("log")) {
                        return "text/plain";
                    }
                    return new MimetypesFileTypeMap().getContentType(f);
                }});
        } catch (IOException ioe) { // ignore exceptions
        } catch (InterruptedException ie) {}
        return mimeType;
    }

    /**
     * Get the mimetype for the file.
     *
     * @param f The file to return the mimetype for.
     * @return the string representing the mimetype of the file.
     */
    public static String getMimeType(File f) {
        if (f.getName().endsWith("log")) {
            return "text/plain";
        }
        return new MimetypesFileTypeMap().getContentType(f);
    }

    /**
     * Upload the build log to the collabnet server.
     *
     * @param build the current Hudson build.
     * @return the id associated with the file upload.
     */
    private String uploadBuildLog(AbstractBuild <?, ?> build) {
        if (this.cna == null) {
            this.logConsole("Cannot call updateSucceedingBuild, not logged in!");
            return null;
        }
        String id = null;
        FileStorageApp sfsa = new FileStorageApp(this.cna);
        try {
            id = sfsa.uploadFile(build.getLogFile());
        } catch (RemoteException re) {
            this.log("uploadBuildLog", re);
        }
        return id;
    }

    /**
     * Upload the build log to the collabnet server.
     *
     * @param filePath the path of file to upload
     * @return the id associated with the file upload.
     */
    private String uploadFile(FilePath filePath) {
        if (this.cna == null) {
            this.logConsole("Cannot call uploadFile, not logged in!");
            return null;
        }
        String id = null;

        try {
            // must upload to same session so temp file will be available later for creation of document
            id = filePath.act(new RemoteFileUploader(getCollabNetUrl(), getUsername(), cna.getSessionId()));
        } catch (RemoteException re) {
            this.logConsole("upload file failed", re);
        } catch (IOException ioe) {
            this.logConsole("Could not upload file due to IOException: " + ioe.getMessage(), ioe);
        } catch (InterruptedException ie) {
            this.logConsole("Could not upload file due to InterruptedException: " + ie.getMessage());
        }
        return id;
    }

    /**
     * Private class that can perform upload function.
     */
    private static class RemoteFileUploader implements FileCallable<String> {

        private String mUrl;
        private String mUsername;
        private String mSessionId;

        /**
         * Constructor. Needs to have old sessionId, since the uploaded file is only available to the same session.
         * @param url collabnet url
         * @param username collabnet username
         * @param sessionId collabnet sessionId
         */
        public RemoteFileUploader(String url, String username, String sessionId) {
            mUrl = url;
            mUsername = username;
            mSessionId = sessionId;
        }

        /**
         * @see FileCallable#invoke(File, VirtualChannel)
         */
        public String invoke(File f, VirtualChannel channel) throws IOException {
            CollabNetApp cnApp = CNHudsonUtil.recreateCollabNetApp(mUrl, mUsername, mSessionId);
            FileStorageApp sfsa = new FileStorageApp(cnApp);
            return sfsa.uploadFile(f);
        }
    }

    /**
     * Log out of the collabnet server.   Invalidates the cna object.
     */
    public void logoff() {
        CNHudsonUtil.logoff(this.cna);
        this.cna = null;        
    }

    /**
     * Get the project id for the project name.
     * 
     * @return the matching project id, or null if none is found.
     */
    public String getProjectId() {
        if (this.cna == null) {
            this.logConsole("Cannot getProjectId, not logged in!");
            return null;
        }
        return CNHudsonUtil.getProjectId(this.cna, this.getProject());
    }

    /**
     * Translates a string that may contain  build vars like ${BUILD_VAR} to
     * a string with those vars interpreted.
     * 
     * @param build the Hudson build.
     * @param str the string to be interpreted.
     * @return the interpreted string.
     * @throws IllegalArgumentException if the env var is not found.
     */
    private String getInterpreted(AbstractBuild<?, ?> build, String str)
            throws IOException, InterruptedException {
        try {
            return CommonUtil.getInterpreted(build.getEnvironment(TaskListener.NULL), str);
        } catch (IllegalArgumentException iae) {
            this.logConsole(iae.getMessage());
            throw iae;
        }
    }

    /**
     * The CNDocumentUploader Descriptor class.
     */
    @Extension
    public static final class DescriptorImpl extends AbstractTeamForgeNotifier.DescriptorImpl {
        /**
         * @return string to display for configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "CollabNet Document Uploader";
        }

        /**
         * Form validation for upload path.
         */
        public FormValidation doCheckUploadPath(CollabNetApp app, @QueryParameter String project, @QueryParameter String value) throws IOException {
            return CNFormFieldValidator.documentPathCheck(app,project,value);
        }

        /**
         * Form validation for the file patterns.
         *
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckFilePatterns(@QueryParameter String value) throws FormValidation {
            return CNFormFieldValidator.unrequiredInterpretedCheck(value, "file patterns");
        }
    }
}
