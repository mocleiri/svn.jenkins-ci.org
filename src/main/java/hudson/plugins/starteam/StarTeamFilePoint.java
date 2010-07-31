package hudson.plugins.starteam;

import java.io.Serializable;
import java.io.File;

/**
 * Stores a reference to the file and the particular file revision.
 */
public class StarTeamFilePoint extends StarTeamChangeLogEntry implements Serializable, Comparable {

  private String fullfilepath;

  public StarTeamFilePoint() {
  }

  public StarTeamFilePoint(com.starbase.starteam.File f) {
    this.fullfilepath = f.getFullName();

    setRevisionNumber(f.getRevisionNumber());
    setMsg(f.getComment());
    setDate(f.getModifiedTime().createDate());
    setFileName(f.getName());
    setUsername(f.getServer().getUser(f.getModifiedBy()).getName());

//    try {
//      key = f.getParentFolderHierarchy() + f.getName();
//    } catch (RuntimeException e) {
//      throw new RuntimeException("This is sometimes thrown deep inside starteam",e);
//    }
  }

  public String getFullfilepath() {
    return fullfilepath;
  }

  public void setFullfilepath(String fullfilepath) {
    this.fullfilepath = fullfilepath;
  }

  public File getFile() {
    return new File(getFullfilepath());
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StarTeamFilePoint that = (StarTeamFilePoint) o;

    if (fullfilepath != null ? !fullfilepath.equals(that.fullfilepath) : that.fullfilepath != null) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (fullfilepath != null ? fullfilepath.hashCode() : 0);
    return result;
  }

  public int compareTo(Object o) {
    return fullfilepath.toLowerCase().compareTo(((StarTeamFilePoint)o).fullfilepath.toLowerCase());
  }

}
