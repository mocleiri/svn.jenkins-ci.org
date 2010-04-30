package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Saves the job configuration if the job is created or renamed.
 *
 * @author Stefan Brausch
 */
@Extension
public final class JobConfigHistoryJobListener extends ItemListener {

    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(JobConfigHistoryJobListener.class.getName());

    /** {@inheritDoc} */
    @Override
    public void onCreated(Item item) {
        LOG.finest("In onCreated for " + item);
        if (item instanceof AbstractProject<?, ?>) {
            ConfigHistoryListenerHelper.CREATED.createNewHistoryEntry(((AbstractProject<?, ?>) item).getConfigFile());
        } else {
            LOG.finest("onCreated: not an AbstractProject, skipping history save");
        }
        LOG.finest("onCreated for " + item + " done.");
        //        new Exception("STACKTRACE for double invocation").printStackTrace();
    }

    /** {@inheritDoc}
     * 
     * <p>
     * Also checks if we have history stored under the old name.  If so, copies
     * all history to the folder for new name, and deletes the old history folder.
     */
    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        LOG.finest("In onRenamed for " + item + " oldName=" + oldName + ", newName=" + newName);
        if (item instanceof AbstractProject<?, ?>) {
            ConfigHistoryListenerHelper.RENAMED.createNewHistoryEntry(((AbstractProject<?, ?>) item).getConfigFile());
            final JobConfigHistory plugin = Hudson.getInstance().getPlugin(JobConfigHistory.class);

            // move history items from previous name, if the directory exists
            // only applies if using a custom root directory for saving history
            if (plugin.getConfiguredHistoryRootDir() != null) {
                final File currentHistoryDir = plugin.getHistoryDir(((AbstractProject<?, ?>) item).getConfigFile());
                final File historyParentDir = currentHistoryDir.getParentFile();
                final File oldHistoryDir = new File(historyParentDir, oldName);
                if (oldHistoryDir.exists()) {
                    final FilePath fp = new FilePath(oldHistoryDir);
                    try {
                        fp.copyRecursiveTo(new FilePath(currentHistoryDir));
                        fp.deleteRecursive();
                        LOG.finest("completed move of old history files for " + item.getName());
                    } catch (IOException e) {
                        LOG.warning("unable to move old history on rename for " + item.getName());
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        LOG.warning("interrupted while moving old history on rename for " + item.getName());
                        e.printStackTrace();
                    }
                }
            }
        }
        LOG.finest("onRename for " + item + " done.");
        //        new Exception("STACKTRACE for double invocation").printStackTrace();
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleted(Item item) {
        LOG.finest("In onDeleted for " + item);
        if (item instanceof AbstractProject<?, ?>) {
            final JobConfigHistory plugin = Hudson.getInstance().getPlugin(JobConfigHistory.class);

            // At this point, the /jobs/<job> directory has been deleted.  We do not want to take any
            // further action unless the history root dir is customized: otherwise we will re-create
            // the /jobs/<job>/config-history directory that we just deleted.
            //
            // Also rename history directory to <job>_deleted_<timestamp> - should be a safe 'unique' name
            if (plugin.getConfiguredHistoryRootDir() != null) {
                ConfigHistoryListenerHelper.DELETED.createNewHistoryEntry(((AbstractProject<?, ?>) item).getConfigFile());
                final File currentHistoryDir = plugin.getHistoryDir(((AbstractProject<?, ?>) item).getConfigFile());

                final SimpleDateFormat buildDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
                final String timestamp = buildDateFormat.format(new Date());
                final String deletedHistoryName = currentHistoryDir.getName() + "_deleted_" + timestamp;
                final File deletedHistoryDir = new File(currentHistoryDir.getParentFile(), deletedHistoryName);
                if (!currentHistoryDir.renameTo(deletedHistoryDir)) {
                    LOG.warning("unable to rename deleted history dir to: " + deletedHistoryDir);
                }
            }
        }
        LOG.finest("onDeleted for " + item + " done.");
    }
}
