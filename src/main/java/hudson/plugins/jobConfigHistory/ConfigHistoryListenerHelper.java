/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import hudson.Util;
import hudson.XmlFile;
import hudson.model.Hudson;
import hudson.model.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Defines some helper functions needed by {@link JobConfigHistoryJobListener} and
 * {@link JobConfigHistorySaveableListener}.
 *
 * @author mfriedenhagen
 */
public enum ConfigHistoryListenerHelper {

    /**
     * Helper for job creation.
     */
    CREATED("Created"),

    /**
     * Helper for job rename.
     */
    RENAMED("Renamed"),

    /**
     * Helper for job change.
     */
    CHANGED("Changed"),

    /**
     * Helper for job deleted.
     */
    DELETED("Deleted");

    /**
     * Name of the operation.
     */
    private final String operation;

    /**
     *
     * @param operation
     *            the operation we handle.
     */
    ConfigHistoryListenerHelper(final String operation) {
        this.operation = operation;
    }

    /**
     * Returns the configuration history directory for the given {@link Item}.
     *
     * @param item
     *            for which we want to save the configuration.
     * @return base directory where to store the history.
     */
    //private File getConfigsDir(Item item) {
    //    return new File(item.getRootDir(), "config-history");
    //}

    /**
     * Creates a timestamped directory to save the configuration beneath.
     * Purges old data if configured
     *
     * @param xmlFile
     *            the current xmlFile configuration file to save
     * @param timestamp
     *            time of operation.
     * @return timestamped directory where to store one history entry.
     */
    private File getRootDir(final XmlFile xmlFile, final Calendar timestamp) {
        final JobConfigHistory plugin = Hudson.getInstance().getPlugin(JobConfigHistory.class);
        final File itemHistoryDir = plugin.getHistoryDir(xmlFile);
        // perform check for purge here, when we are actually going to create
        //  a new directory, rather than just when we scan it in above method.
        plugin.checkForPurgeByQuantity(itemHistoryDir);
        final File f = new File(itemHistoryDir, getIdFormatter().format(timestamp.getTime()));
        // mkdirs sometimes fails although the directory exists afterwards,
        // so check for existence as well and just be happy if it does.
        if (!(f.mkdirs() || f.exists())) {
            throw new RuntimeException("Could not create rootDir " + f);
        }
        return f;
    }

    /**
     * Creates a new backup of the job configuration.
     *
     * @param xmlFile
     *            configuration file for the item we want to backup
     */
    public final void createNewHistoryEntry(final XmlFile xmlFile) {
        try {
            final Calendar timestamp = new GregorianCalendar();
            final File timestampedDir = getRootDir(xmlFile, timestamp);
            if (this != DELETED) {
                copyConfigFile(xmlFile.getFile(), timestampedDir);
            }
            createHistoryXmlFile(timestamp, timestampedDir);
        } catch (IOException e) {
            throw new RuntimeException("Operation " + operation + " on " + xmlFile + " did not succeed", e);
        }
    }

    /**
     * Creates the historical description for this action.
     *
     * @param timestamp
     *            when the action did happen.
     * @param timestampedDir
     *            the directory where to save the history.
     * @throws IOException
     *             if writing the history fails.
     */
    private void createHistoryXmlFile(final Calendar timestamp, final File timestampedDir) throws IOException {
        final User currentUser = getCurrentUser();
        final String user;
        final String userId;
        if (currentUser != null) {
            user = currentUser.getFullName();
            userId = currentUser.getId();
        } else {
            user = "Anonym";
            userId = "anonymous";
        }

        final XmlFile historyDescription = new XmlFile(new File(timestampedDir, JobConfigHistoryConsts.HISTORY_FILE));
        final HistoryDescr myDescr = new HistoryDescr(user, userId, operation, getIdFormatter().format(
                timestamp.getTime()));
        historyDescription.write(myDescr);
    }

    /**
     * Returns the user who invoked the action.
     *
     * @return current user.
     */
    User getCurrentUser() {
        return User.current();
    }

    /**
     * Saves a copy of this project's {@code config.xml} into {@code timestampedDir}.
     *
     * @param currentConfig
     *            which we want to copy.
     * @param timestampedDir
     *            the directory where to save the copy.
     * @throws FileNotFoundException
     *             if initiating the file holding the copy fails.
     * @throws IOException
     *             if writing the file holding the copy fails.
     */
    private void copyConfigFile(final File currentConfig, final File timestampedDir) throws FileNotFoundException, IOException {

        final FileOutputStream configCopy = new FileOutputStream(new File(timestampedDir, currentConfig.getName()));
        try {
            final FileInputStream configOriginal = new FileInputStream(currentConfig);
            try {
                Util.copyStream(configOriginal, configCopy);
            } finally {
                configOriginal.close();
            }
        } finally {
            configCopy.close();
        }
    }

    /**
     * Returns a simple formatter used for creating timestamped directories. We create this every time as
     * {@link SimpleDateFormat} is <b>not</b> threadsafe.
     *
     * @return the idFormatter
     */
    SimpleDateFormat getIdFormatter() {
        return new SimpleDateFormat(JobConfigHistoryConsts.ID_FORMATTER);
    }

}
