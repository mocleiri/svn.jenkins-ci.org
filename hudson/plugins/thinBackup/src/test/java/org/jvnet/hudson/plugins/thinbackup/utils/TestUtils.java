/**
 *  Copyright (C) 2011  Matthias Steinkogler, Thomas Fürer
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
package org.jvnet.hudson.plugins.thinbackup.utils;

import java.io.File;
import java.text.ParseException;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPeriodicWork.BackupType;
import org.jvnet.hudson.plugins.thinbackup.backup.BackupDirStructureSetup;
import org.jvnet.hudson.plugins.thinbackup.backup.BackupSet;

public class TestUtils extends BackupDirStructureSetup {

  @Test
  public void testConvertToDirectoryNameDateFormat() throws ParseException {
    final String displayDate = "2011-02-13 10:48";
    final String fileDate = Utils.convertToDirectoryNameDateFormat(displayDate);
    Assert.assertEquals("2011-02-13_10-48", fileDate);
  }

  @Test(expected = ParseException.class)
  public void testBadFormatConvertToDirectoryNameDateFormat() throws ParseException {
    final String displayDate = "2011-02-13-10:48";
    Utils.convertToDirectoryNameDateFormat(displayDate);
  }

  @Test(expected = ParseException.class)
  public void testWrongFormatConvertToDirectoryNameDateFormat() throws ParseException {
    final String displayDate = "FULL-2011-02-13_10-48";
    Utils.convertToDirectoryNameDateFormat(displayDate);
  }

  @Test(expected = ParseException.class)
  public void testEmptyDateConvertToDirectoryNameDateFormat() throws ParseException {
    Utils.convertToDirectoryNameDateFormat("");
  }

  @Test
  public void testGetBackupTypeDirectories() {
    final List<File> fullBackupDirs = Utils.getBackupTypeDirectories(backupDir, BackupType.FULL);
    Assert.assertEquals(3, fullBackupDirs.size());

    final List<File> diffBackupDirs = Utils.getBackupTypeDirectories(backupDir, BackupType.DIFF);
    Assert.assertEquals(7, diffBackupDirs.size());
  }

  @Test
  public void testGetReferencedFullBackup() {
    File fullBackup = Utils.getReferencedFullBackup(diff11);
    Assert.assertEquals(full1, fullBackup);

    fullBackup = Utils.getReferencedFullBackup(diff12);
    Assert.assertEquals(full1, fullBackup);

    fullBackup = Utils.getReferencedFullBackup(diff13);
    Assert.assertEquals(full1, fullBackup);

    fullBackup = Utils.getReferencedFullBackup(diff14);
    Assert.assertEquals(full1, fullBackup);

    fullBackup = Utils.getReferencedFullBackup(full1);
    Assert.assertEquals(full1, fullBackup);

    fullBackup = Utils.getReferencedFullBackup(diff41);
    Assert.assertEquals(null, fullBackup);
  }

  @Test
  public void testGetReferencingDiffBackups() {
    List<File> diffBackups = Utils.getReferencingDiffBackups(full1);
    Assert.assertEquals(4, diffBackups.size());
    Assert.assertTrue(diffBackups.contains(diff11));
    Assert.assertTrue(diffBackups.contains(diff12));
    Assert.assertTrue(diffBackups.contains(diff13));
    Assert.assertTrue(diffBackups.contains(diff14));

    diffBackups = Utils.getReferencingDiffBackups(diff41);
    Assert.assertEquals(0, diffBackups.size());
  }

  @Test
  public void testGetBackups() {
    final List<String> backups = Utils.getBackupsAsDates(backupDir);
    Assert.assertEquals(10, backups.size());
  }

  @Test
  public void testGetValidBackupSets() {
    final List<BackupSet> validBackupSets = Utils.getValidBackupSetsFromDirectories(backupDir);
    Assert.assertEquals(3, validBackupSets.size());
  }

}
