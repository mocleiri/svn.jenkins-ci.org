package hudson.plugins.starteam;

import com.starbase.starteam.File;

import java.util.*;

/**
 * The collection of actions that need to be performed upon checkout.
 *
 * Files to remove: Typically folders get removed in starteam and the files get left on disk.
 *
 * Files to checkout: Files that are out of date, missing, etc.
 *
 * File Points to remember: When using promotions states/labels file changes may be pushed forward
 *    or rolled backwards.  Either way, it is difficult (using starteam) to accurately determine
 *    the previous build when various different labelling strategies are being used (e.g. promotion
 *    states, etc).  For this reason we persist a list of the filepoints used upon checkout in the
 *    build folder.  This is then used to compare current v.s. historic and compute the changelist.
 */
public class StarTeamChangeSet {

  private boolean comparisonAvailable;

  private Collection<java.io.File> filesToRemove = new ArrayList<java.io.File>();

  private Collection<File> filesToCheckout = new ArrayList<File>();

  private Collection<StarTeamFilePoint> filePointsToRemember = new ArrayList<StarTeamFilePoint>();

  public boolean hasChanges() {
      return isComparisonAvailable()
      || getAdded() != null && getAdded().isEmpty()
      || getHigher() != null && getHigher().isEmpty()
      || getLower() != null && getLower().isEmpty()
      || getDelete() != null && getDelete().isEmpty()
      || getDirty() != null && getDirty().isEmpty();
  }

  public Collection<java.io.File> getFilesToRemove() {
    return filesToRemove;
  }

  public void setFilesToRemove(Collection<java.io.File> filesToRemove) {
    this.filesToRemove = filesToRemove;
  }

  public Collection<File> getFilesToCheckout() {
    return filesToCheckout;
  }

  public void setFilesToCheckout(Collection<File> filesToCheckout) {
    this.filesToCheckout = filesToCheckout;
  }

  public void setFilePointsToRemember(Collection<StarTeamFilePoint> filePointsToRemember) {
    this.filePointsToRemember = filePointsToRemember;
  }

  public Collection<StarTeamFilePoint> getFilePointsToRemember() {
    return filePointsToRemember;
  }

  private Set<StarTeamFilePoint> added;
  private Set<StarTeamFilePoint> higher;
  private Set<StarTeamFilePoint> lower;
  private Set<StarTeamFilePoint> delete;
  private Set<StarTeamFilePoint> dirty;

  public boolean isComparisonAvailable() {
    return comparisonAvailable;
  }

  public void setComparisonAvailable(boolean comparisonAvailable) {
    this.comparisonAvailable = comparisonAvailable;
  }

  public void setAdded(Collection<StarTeamFilePoint> value) {
    added = new TreeSet<StarTeamFilePoint>(value);
  }

  public void setHigher(Collection<StarTeamFilePoint> value) {
    higher = new TreeSet<StarTeamFilePoint>(value);
  }

  public void setLower(Collection<StarTeamFilePoint> value) {
    lower = new TreeSet<StarTeamFilePoint>(value);
  }

  public void setDelete(Collection<StarTeamFilePoint> value) {
    delete = new TreeSet<StarTeamFilePoint>(value);
  }

  public void setDirty(Collection<StarTeamFilePoint> value) {
    dirty = new TreeSet<StarTeamFilePoint>(value);
  }

  public Set<StarTeamFilePoint> getAdded() {
    return added;
  }

  public Set<StarTeamFilePoint> getHigher() {
    return higher;
  }

  public Set<StarTeamFilePoint> getLower() {
    return lower;
  }

  public Set<StarTeamFilePoint> getDelete() {
    return delete;
  }


  public Set<StarTeamFilePoint> getDirty() {
    return dirty;
  }

}
