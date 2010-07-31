package hudson.plugins.starteam;

import hudson.model.User;
import hudson.scm.ChangeLogSet;

import java.util.Collection;
import java.util.Date;

/**
 * <p>
 * Implementation of {@link ChangeLogSet.Entry} for StarTeam SCM.
 * </p>
 * 
 * @author Eric D. Broyles
 * @version 1.0
 */
public class StarTeamChangeLogEntry extends hudson.scm.ChangeLogSet.Entry
{
  private Integer revisionNumber;

  private String username;

  private String msg;

  private Date date;
  
  private String fileName;

  private String change;

  @Override
  public Collection<String> getAffectedPaths()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Gets the Hudson user based upon the StarTeam {@link #username}.
   * 
   * @see hudson.scm.ChangeLogSet.Entry#getAuthor()
   */
  @Override
  public User getAuthor()
  {
    if (username == null) {
      username = "Unknown";
    }
    return User.get(username);
  }

  public void setUsername(String username)
  {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

  @Override
  public String getMsg()
  {
    return msg;
  }

  public void setMsg(String msg)
  {
    this.msg = msg;
  }

  public Integer getRevisionNumber()
  {
    return revisionNumber;
  }

  public void setRevisionNumber(Integer revisionNumber)
  {
    this.revisionNumber = revisionNumber;
  }

  public Date getDate()
  {
    return date;
  }

  public void setDate(Date date)
  {
    this.date = date;
  }

  public String getFileName()
  {
    return fileName;
  }

  public void setFileName(String fileName)
  {
    this.fileName = fileName;
  }

  public String getChange() {
    return change;
  }

  public void setChange(String change) {
    this.change = change;
  }

}
