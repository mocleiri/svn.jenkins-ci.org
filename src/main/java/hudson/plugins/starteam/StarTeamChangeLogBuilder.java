package hudson.plugins.starteam;

import hudson.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.kohsuke.stapler.framework.io.WriterOutputStream;

/**
 * Builds <tt>changelog.xml</tt> for {@link StarTeamSCM}.
 *
 * @author Eric D. Broyles
 */
public final class StarTeamChangeLogBuilder {
  
  /**
   * Stores the history objects to the output stream as xml.
   * <p>
   * Current version supports a format like the following:
   * 
   * <pre>
   * &lt;?xml version='1.0' encoding='UTF-8'?&gt;
   *   &lt;changelog&gt;
   *         &lt;entry&gt;
   *                 &lt;revisionNumber&gt;73&lt;/revisionNumber&gt;
   *                 &lt;date&gt;2008-06-23 09:46:27&lt;/date&gt;
   *                 &lt;message&gt;Checkin message&lt;/message&gt;
   *                 &lt;user&gt;Author Name&lt;/user&gt;
   *         &lt;/entry&gt;
   *   &lt;/changelog&gt;
   * 
   * </pre>
   * </p>
   * 
   * @param outputStream the stream to write to
   * @param changes the history objects to store
   * @param connection the connection to the StarTeam Server (required to
   *          determine the name of the user who made changes)
   * @throws IOException
   * 
   */
  public static boolean writeChangeLog(OutputStream outputStream, StarTeamChangeSet changeSet) throws IOException {
    OutputStreamWriter writer = new OutputStreamWriter(outputStream, Charset.forName("UTF-8"));
    //hudson.util.WriterOutputStream stream1 = new WriterOutputStream(writer);
    org.kohsuke.stapler.framework.io.WriterOutputStream stream1 = new WriterOutputStream(writer);
    PrintStream stream = new PrintStream(stream1);

    stream.println("<?xml version='1.0' encoding='UTF-8'?>");
    stream.println("<changelog>");

    if (changeSet.isComparisonAvailable()) {
      for(StarTeamFilePoint change : changeSet.getAdded()) {
        stream.println("\t<entry>");
        stream.println("\t\t<fileName>" + change.getFileName() + "</fileName>");
        stream.println("\t\t<revisionNumber>" + change.getRevisionNumber() + "</revisionNumber>");
        stream.println("\t\t<date>" + Util.xmlEscape(javaDateToStringDate(change.getDate())) + "</date>");
        if (change.getMsg()!=null) {
          stream.println("\t\t<message>" + Util.xmlEscape(change.getMsg()) + "</message>");
        }
        stream.println("\t\t<user>" + change.getAuthor() + "</user>");
        stream.println("\t\t<change>added</change>");
        stream.println("\t</entry>");
      }
      for(StarTeamFilePoint change : changeSet.getHigher()) {
        stream.println("\t<entry>");
        stream.println("\t\t<fileName>" + change.getFileName() + "</fileName>");
        stream.println("\t\t<revisionNumber>" + change.getRevisionNumber() + "</revisionNumber>");
        stream.println("\t\t<date>" + Util.xmlEscape(javaDateToStringDate(change.getDate())) + "</date>");
        if (change.getMsg()!=null) {
          stream.println("\t\t<message>" + Util.xmlEscape(change.getMsg()) + "</message>");
        }
        stream.println("\t\t<user>" + change.getAuthor() + "</user>");
        stream.println("\t\t<change>changed</change>");
        stream.println("\t</entry>");
      }
      for(StarTeamFilePoint change : changeSet.getLower()) {
        stream.println("\t<entry>");
        stream.println("\t\t<fileName>" + change.getFileName() + "</fileName>");
        stream.println("\t\t<revisionNumber>" + change.getRevisionNumber() + "</revisionNumber>");
        stream.println("\t\t<date>" + Util.xmlEscape(javaDateToStringDate(change.getDate())) + "</date>");
        if (change.getMsg()!=null) {
          stream.println("\t\t<message>" + Util.xmlEscape(change.getMsg()) + "</message>");
        }
        stream.println("\t\t<user>" + change.getAuthor() + "</user>");
        stream.println("\t\t<change>rollback</change>");
        stream.println("\t</entry>");
      }
      for(StarTeamFilePoint change : changeSet.getDelete()) {
        stream.println("\t<entry>");
        stream.println("\t\t<fileName>" + change.getFileName() + "</fileName>");
        stream.println("\t\t<change>deleted</change>");
        stream.println("\t</entry>");
      }
    } else {
        stream.println("\t<entry>");
        stream.println("\t\t<message>Unable to accurately compare changes against previous build.</message>");
        stream.println("\t</entry>");

        for(StarTeamFilePoint change : changeSet.getDirty()) {
          stream.println("\t<entry>");
          stream.println("\t\t<fileName>" + change.getFileName() + "</fileName>");
          stream.println("\t\t<revisionNumber>" + change.getRevisionNumber() + "</revisionNumber>");
          stream.println("\t\t<date>" + Util.xmlEscape(javaDateToStringDate(change.getDate())) + "</date>");
          if (change.getMsg()!=null) {
            stream.println("\t\t<message>" + Util.xmlEscape(change.getMsg()) + "</message>");
          }
          stream.println("\t\t<user>" + change.getAuthor() + "</user>");
          if (change.getChange() != null) {
            stream.println("\t\t<change>"+change.getChange()+"</change>");
          }
          stream.println("\t</entry>");
        }
    }

    stream.println("</changelog>");
    stream.close();
    return true;
  }

  /**
   * This takes a java.util.Date and converts it to a string.
   * 
   * @return A string representation of the date
   * @author Mike Wille
   */
  public static String javaDateToStringDate(java.util.Date newDate) {
    if(newDate == null)
      return "";

    GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
    cal.clear();
    cal.setTime(newDate);

    int year = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH) + 1;
    int day = cal.get(Calendar.DAY_OF_MONTH);

    int hour = cal.get(Calendar.HOUR_OF_DAY);
    int min = cal.get(Calendar.MINUTE);
    int sec = cal.get(Calendar.SECOND);

    String date = year + "-" + putZero(month) + "-" + putZero(day);
    if(hour + min + sec > 0)
      date += " " + putZero(hour) + ":" + putZero(min) + ":" + putZero(sec);

    return date;
  }

  private static String putZero(int i) {
    if(i < 10) {
      return "0" + i;
    }
    return i + "";
  }

}
