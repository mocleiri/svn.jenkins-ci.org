package org.jvnet.hudson.plugins.thinbackup.backup;

import hudson.PluginWrapper;
import hudson.model.Hudson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;

public class HudsonBackup {

  private static final String BUILDS_DIR_NAME = "Builds";
  private static final String NEXT_BUILD_NUMBER_FILENAME = "nextBuildNumber";
  private static final String JOBS_DIR = "jobs";

  private final File hudsonDirectory;
  private final File backupDirectory;
  private final BackupType backupType;

  public HudsonBackup(final String backupRootPath, final File hudsonHome, final BackupType backupType) {
    hudsonDirectory = hudsonHome;

    final Date date = new Date();
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
    final String backupPath = String.format("%s/%s-%s", backupRootPath, backupType, format.format(date));
    backupDirectory = new File(backupPath);

    this.backupType = backupType;
  }

  public boolean run() throws IOException {
    if (!hudsonDirectory.exists() || !hudsonDirectory.isDirectory()) {
      throw new FileNotFoundException("No Hudson directory found, thus cannot trigger backup.");
    }
    if (!backupDirectory.exists() || !backupDirectory.isDirectory()) {
      final boolean res = backupDirectory.mkdirs();
      if (!res) {
        throw new IOException("Could not create target directory.");
      }
    }
    if (backupType == BackupType.NONE) {
      throw new IllegalStateException("Backup type must be FULL or DIFF.");
    }

    backupGlobalXmls();
    backupJobs();
    storePluginList();

    return true;
  }

  private void backupGlobalXmls() throws IOException {
    IOFileFilter suffixFileFilter = FileFilterUtils.suffixFileFilter(".xml");
    suffixFileFilter = FileFilterUtils.andFileFilter(FileFileFilter.FILE, suffixFileFilter);
    suffixFileFilter = FileFilterUtils.andFileFilter(suffixFileFilter, getDiffFilter());
    FileUtils.copyDirectory(hudsonDirectory, backupDirectory, suffixFileFilter);
  }

  private void backupJobs() throws IOException {
    final String jobsPath = String.format("%s/%s", hudsonDirectory.getAbsolutePath(), JOBS_DIR);
    final File jobsDirectory = new File(jobsPath);

    final String jobsBackupPath = String.format("%s/%s", backupDirectory.getAbsolutePath(), JOBS_DIR);
    final File jobsBackupDirectory = new File(jobsBackupPath);

    IOFileFilter filter = FileFilterUtils.suffixFileFilter(".xml");
    filter = FileFilterUtils.orFileFilter(filter, FileFilterUtils.nameFileFilter(NEXT_BUILD_NUMBER_FILENAME));

    final Hudson hudson = Hudson.getInstance();
    Collection<String> jobNames;
    if (hudson != null) {
      jobNames = hudson.getJobNames();
    } else {
      jobNames = Arrays.asList(jobsDirectory.list());
    }

    IOFileFilter jobFilter = null;
    for (final String jobName : jobNames) {
      final IOFileFilter nameFileFilter = FileFilterUtils.nameFileFilter(jobName);
      if (jobFilter == null) {
        jobFilter = nameFileFilter;
      } else {
        jobFilter = FileFilterUtils.orFileFilter(filter, nameFileFilter);
      }

      final File buildsDir = new File(new File(jobsDirectory, jobName), BUILDS_DIR_NAME);
      if (buildsDir.exists() && buildsDir.isDirectory()) {
        final Collection<String> builds = Arrays.asList(buildsDir.list());
        if (builds != null) {
          final String buildsBackupPath = String.format("%s/%s/%s", jobsBackupPath, jobName, BUILDS_DIR_NAME);
          for (final String build : builds) {
            final File srcDir = new File(buildsDir, build);
            final File destDir = new File(buildsBackupPath, build);
            final IOFileFilter buildFilter = FileFilterUtils.andFileFilter(FileFileFilter.FILE, getDiffFilter());
            FileUtils.copyDirectory(srcDir, destDir, buildFilter);
          }
        }
      }
    }
    if (jobFilter != null) {
      jobFilter = FileFilterUtils.andFileFilter(jobFilter, DirectoryFileFilter.DIRECTORY);
      filter = FileFilterUtils.orFileFilter(filter, jobFilter);
    }

    filter = FileFilterUtils.andFileFilter(filter, getDiffFilter());
    FileUtils.copyDirectory(jobsDirectory, jobsBackupDirectory, filter);
  }

  private void storePluginList() throws IOException {
    final Hudson hudson = Hudson.getInstance();
    final List<PluginWrapper> installedPlugins;

    if (hudson != null) {
      installedPlugins = hudson.getPluginManager().getPlugins();
    } else {
      installedPlugins = Collections.emptyList();
    }
    final File pluginVersionList = new File(backupDirectory, "installedPlugins.txt");
    pluginVersionList.createNewFile();
    final Writer w = new FileWriter(pluginVersionList);

    if (hudson != null) {
      w.write(String.format("Hudson [%s]\n", Hudson.getVersion()));
    }

    for (final PluginWrapper plugin : installedPlugins) {
      final String entry = String.format("Name: %s Version: %s\n", plugin.getShortName(), plugin.getVersion());
      w.write(entry);
    }
    w.close();
  }

  private IOFileFilter getDiffFilter() {
    IOFileFilter result = FileFilterUtils.trueFileFilter();

    if (backupType == BackupType.DIFF) {
      result = FileFilterUtils.ageFileFilter(getLatestFullBackupDate(), false);
    }

    return result;
  }

  private Date getLatestFullBackupDate() {
    final IOFileFilter prefixFilter = FileFilterUtils.prefixFileFilter(BackupType.FULL.toString());
    final Collection<File> backups = Arrays.asList(backupDirectory.getParentFile().listFiles(
        (FilenameFilter) prefixFilter));

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