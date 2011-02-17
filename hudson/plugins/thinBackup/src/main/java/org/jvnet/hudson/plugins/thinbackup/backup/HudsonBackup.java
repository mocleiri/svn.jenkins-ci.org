/**
 *  Copyright (C) 2011  Matthias Steinkogler, Thomas F�rer
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses.
 */
package org.jvnet.hudson.plugins.thinbackup.backup;

import hudson.PluginWrapper;
import hudson.model.Hudson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.utils.Utils;

public class HudsonBackup {
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.thinbackup");

  private static final String INSTALLED_PLUGINS_XML = "installedPlugins.xml";
  private static final String BUILDS_DIR_NAME = "builds";
  private static final String JOBS_DIR_NAME = "jobs";
  private static final String USERS_DIR_NAME = "users";

  private final Hudson hudson;
  private final File hudsonDirectory;
  private final File backupRoot;
  private final File backupDirectory;
  private final BackupType backupType;
  private final Date latestFullBackupDate;
  private final boolean cleanupDiff;
  private final int nrMaxStoredFull;

  public HudsonBackup(final File backupRoot, final File hudsonHome, final BackupType backupType,
      final int nrMaxStoredFull, final boolean cleanupDiff) {
    hudson = Hudson.getInstance();

    hudsonDirectory = hudsonHome;
    this.cleanupDiff = cleanupDiff;
    this.nrMaxStoredFull = nrMaxStoredFull;

    this.backupRoot = backupRoot;
    if (!backupRoot.exists()) {
      backupRoot.mkdir();
    }

    latestFullBackupDate = getLatestFullBackupDate();
    // if no full backup has been done yet, do a FULL backup
    if (latestFullBackupDate == null) {
      LOGGER.info("No previous full backup found, thus creating one.");
      this.backupType = BackupType.FULL;
    } else {
      this.backupType = backupType;
    }

    final Date date = new Date();
    backupDirectory = Utils.getFormattedDirectory(backupRoot, backupType, date);
  }

  public void backup() throws IOException {
    if (backupType == BackupType.NONE) {
      final String msg = "Backup type must be FULL or DIFF. Backup cannot be performed.";
      LOGGER.severe(msg);
      throw new IllegalStateException(msg);
    }

    LOGGER.fine(MessageFormat.format("Performing {0} backup.", backupType));

    if (!hudsonDirectory.exists() || !hudsonDirectory.isDirectory()) {
      final String msg = "No Hudson directory found. Backup cannot be performed.";
      LOGGER.severe(msg);
      throw new FileNotFoundException(msg);
    }
    if (!backupDirectory.exists() || !backupDirectory.isDirectory()) {
      final boolean res = backupDirectory.mkdirs();
      if (!res) {
        final String msg = "Could not create backup directory. Backup cannot be performed.";
        LOGGER.severe(msg);
        throw new IOException(msg);
      }
    }

    backupGlobalXmls();
    backupJobs();
    backupUsers();
    storePluginListIfChanged();

    new DirectoryCleaner().removeEmptyDirectories(backupDirectory);

    if (backupType == BackupType.FULL) {
      removeSuperfluousBackups();
      cleanupDiffs();
    }
  }

  private void backupGlobalXmls() throws IOException {
    LOGGER.fine("Backing up global configuration files...");

    IOFileFilter suffixFileFilter = FileFilterUtils.suffixFileFilter(".xml");
    suffixFileFilter = FileFilterUtils.andFileFilter(FileFileFilter.FILE, suffixFileFilter);
    suffixFileFilter = FileFilterUtils.andFileFilter(suffixFileFilter, getDiffFilter());
    FileUtils.copyDirectory(hudsonDirectory, backupDirectory, suffixFileFilter);

    LOGGER.fine("DONE backing up global configuration files.");
  }

  private void backupJobs() throws IOException {
    LOGGER.fine("Backing up job specific configuration files...");
    final File jobsDirectory = new File(hudsonDirectory.getAbsolutePath(), JOBS_DIR_NAME);
    final File jobsBackupDirectory = new File(backupDirectory.getAbsolutePath(), JOBS_DIR_NAME);

    Collection<String> jobNames;
    if (hudson != null) {
      jobNames = hudson.getJobNames();
    } else {
      jobNames = Arrays.asList(jobsDirectory.list());
    }

    LOGGER.info(String.format("Found %d jobs to back up.", jobNames.size()));
    LOGGER.fine(String.format("\t%s", jobNames));
    for (final String jobName : jobNames) {
      backupJobConfigFor(jobName, jobsDirectory, jobsBackupDirectory);
      backupBuildsFor(jobName, jobsDirectory, jobsBackupDirectory);
    }
    LOGGER.fine("DONE backing up job specific configuration files.");
  }

  private void backupJobConfigFor(String jobName, File jobsDirectory, File jobsBackupDirectory) throws IOException {
    IOFileFilter filter = FileFilterUtils.suffixFileFilter(".xml");
    filter = FileFilterUtils.andFileFilter(filter, getDiffFilter());
    File srcDir = new File(jobsDirectory, jobName);
    if (srcDir.exists()) { // sub jobs e.g. maven modules need not be copied
      FileUtils.copyDirectory(srcDir, new File(jobsBackupDirectory, jobName), filter);
    }
  }

  private void backupBuildsFor(final String jobName, final File jobsDirectory, final File jobsBackupDirectory)
      throws IOException {
    final File buildsDir = new File(new File(jobsDirectory, jobName), BUILDS_DIR_NAME);
    if (buildsDir.exists() && buildsDir.isDirectory()) {
      final Collection<String> builds = Arrays.asList(buildsDir.list());
      if (builds != null) {
        for (final String build : builds) {
          final File srcDir = new File(buildsDir, build);
          if (!isSymLinkFile(srcDir)) {
            final File destDir = new File(new File(new File(jobsBackupDirectory, jobName), BUILDS_DIR_NAME), build);
            IOFileFilter buildFilter = FileFilterUtils.andFileFilter(FileFileFilter.FILE, getDiffFilter());
            buildFilter = FileFilterUtils.andFileFilter(buildFilter,
                FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter(".zip")));
            FileUtils.copyDirectory(srcDir, destDir, buildFilter);
          }
        }
      }
    }
  }

  private void backupUsers() throws IOException {
    final File usersDirectory = new File(hudsonDirectory.getAbsolutePath(), USERS_DIR_NAME);
    if (usersDirectory.exists() && usersDirectory.isDirectory()) {
      LOGGER.fine("Backing up users specific configuration files...");
      final File usersBackupDirectory = new File(backupDirectory.getAbsolutePath(), USERS_DIR_NAME);
      IOFileFilter filter = FileFilterUtils.suffixFileFilter(".xml");
      filter = FileFilterUtils.andFileFilter(filter, getDiffFilter());
      filter = FileFilterUtils.orFileFilter(filter, DirectoryFileFilter.DIRECTORY);
      FileUtils.copyDirectory(usersDirectory, usersBackupDirectory, filter);
      LOGGER.fine("DONE backing up users specific configuration files.");
    }
  }

  private boolean isSymLinkFile(File file) throws IOException {
    String canonicalPath = file.getCanonicalPath();
    String absolutePath = file.getAbsolutePath();
    return !canonicalPath.substring(canonicalPath.lastIndexOf(File.separatorChar)).equals(
        absolutePath.substring(absolutePath.lastIndexOf(File.separatorChar)));
  }

  private void storePluginListIfChanged() throws IOException {
    final PluginList pluginList = getInstalledPlugins();
    PluginList latestFullPlugins = null;
    if (backupType == BackupType.DIFF) {
      latestFullPlugins = getPluginListFromLatestFull();
    }

    if (pluginList.compareTo(latestFullPlugins) != 0) {
      LOGGER.fine("Storing list of installed plugins...");
      pluginList.save();
    } else {
      LOGGER.fine("No changes in plugin list since last full backup.");
    }

    LOGGER.fine("DONE storing list of installed plugins.");
  }

  private PluginList getInstalledPlugins() {
    final File pluginVersionList = new File(backupDirectory, INSTALLED_PLUGINS_XML);
    final PluginList newPluginList = new PluginList(pluginVersionList);
    if (hudson != null) {
      newPluginList.add("Hudson core", Hudson.getVersion().toString());
    }

    final List<PluginWrapper> installedPlugins;
    if (hudson != null) {
      installedPlugins = hudson.getPluginManager().getPlugins();
    } else {
      installedPlugins = Collections.emptyList();
    }
    for (final PluginWrapper plugin : installedPlugins) {
      newPluginList.add(plugin.getShortName(), plugin.getVersion());
    }

    return newPluginList;
  }

  private PluginList getPluginListFromLatestFull() throws IOException {
    final File latestFullBackupDir = Utils.getFormattedDirectory(backupRoot, BackupType.FULL, latestFullBackupDate);
    final File pluginsOfLatestFull = new File(latestFullBackupDir, INSTALLED_PLUGINS_XML);
    final PluginList latestFullPlugins = new PluginList(pluginsOfLatestFull);
    latestFullPlugins.load();
    return latestFullPlugins;
  }

  private void removeSuperfluousBackups() throws IOException {
    if (nrMaxStoredFull > 0) {
      LOGGER.fine("Removing superfluous backups...");
      final List<BackupSet> availableBackupSets = Utils.getAvailableValidBackupSets();
      int nrOfRemovedBackups = 0;
      while (availableBackupSets.size() > nrMaxStoredFull) {
        final BackupSet set = availableBackupSets.get(0);
        set.delete();
        availableBackupSets.remove(set);
        ++nrOfRemovedBackups;
      }
      LOGGER.fine(String.format("DONE. Removed %d superfluous backup(s).", nrOfRemovedBackups));
    }
  }

  private void cleanupDiffs() throws IOException {
    if (cleanupDiff) {
      LOGGER.fine("Cleaning up diffs...");
      IOFileFilter filter = FileFilterUtils.prefixFileFilter(BackupType.DIFF.toString());
      filter = FileFilterUtils.andFileFilter(filter, DirectoryFileFilter.DIRECTORY);
      final File[] diffDirs = backupDirectory.getParentFile().listFiles((FilenameFilter) filter);
      for (final File diffDirToDelete : diffDirs) {
        FileUtils.deleteDirectory(diffDirToDelete);
      }
      LOGGER.fine(String.format("DONE. Removed %s unnecessary diff directories.", diffDirs.length));
    }
  }

  private IOFileFilter getDiffFilter() {
    IOFileFilter result = FileFilterUtils.trueFileFilter();

    if (backupType == BackupType.DIFF) {
      result = FileFilterUtils.ageFileFilter(latestFullBackupDate, false);
    }

    return result;
  }

  private Date getLatestFullBackupDate() {
    IOFileFilter prefixFilter = FileFilterUtils.prefixFileFilter(BackupType.FULL.toString());
    prefixFilter = FileFilterUtils.andFileFilter(prefixFilter, DirectoryFileFilter.DIRECTORY);
    final File[] backups = backupRoot.listFiles((FilenameFilter) prefixFilter);

    if ((backups == null) || (backups.length == 0)) {
      return null;
    }

    Date latestBackupDate = null;
    for (final File fullBackupDir : backups) {
      final Date curModifiedDate = new Date(fullBackupDir.lastModified());
      if ((latestBackupDate == null) || curModifiedDate.after(latestBackupDate)) {
        latestBackupDate = curModifiedDate;
      }
    }

    return latestBackupDate;
  }

}