package hudson.plugins.perforce;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import hudson.Util;
import hudson.util.WriterOutputStream;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.nio.charset.Charset;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author Mike Wille
 */
public class PerforceChangeLogSet extends ChangeLogSet<PerforceChangeLogEntry> {

    private List<PerforceChangeLogEntry> history = null;

    public PerforceChangeLogSet(AbstractBuild<?, ?> build, List<PerforceChangeLogEntry> logs) {
        super(build);
        this.history = Collections.unmodifiableList(logs);
    }

    public List<PerforceChangeLogEntry> getHistory() {
        return history;
    }

    /*
     * @see hudson.scm.ChangeLogSet#isEmptySet()
     */
    @Override
    public boolean isEmptySet() {
        return history.size() == 0;
    }

    /*
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<PerforceChangeLogEntry> iterator() {
        return history.iterator();
    }

    /**
     * Parses the change log stream and returns a Perforce change log set.
     *
     * @param build
     *            the build for the change log
     * @param changeLogStream
     *            input stream containing the change log
     * @return the change log set
     */
    @SuppressWarnings("unchecked")
    public static PerforceChangeLogSet parse(AbstractBuild build, InputStream changeLogStream) throws IOException, SAXException {

        ArrayList<PerforceChangeLogEntry> changeLogEntries = new ArrayList<PerforceChangeLogEntry>();
        PerforceChangeLogSet changeLogSet = new PerforceChangeLogSet(build, changeLogEntries);

        try {
            SAXReader reader = new SAXReader();
            Document changeDoc = reader.read(changeLogStream);

            Node historyNode = changeDoc.selectSingleNode("/changelog");
            if (historyNode == null)
                return changeLogSet;

            List<Node> entries = historyNode.selectNodes("entry");
            if (entries == null)
                return changeLogSet;

            for (Node node : entries) {
                PerforceChangeLogEntry change = new PerforceChangeLogEntry(changeLogSet);

                if (node.selectSingleNode("changenumber") != null)
                    change.setChangeNumber(new Integer(node.selectSingleNode("changenumber").getStringValue()));

                if (node.selectSingleNode("date") != null)
                    change.setDate(stringDateToJavaDate(node.selectSingleNode("date").getStringValue()));

                if (node.selectSingleNode("description") != null)
                    change.setDescription(node.selectSingleNode("description").getStringValue());

                if (node.selectSingleNode("user") != null)
                    change.setUser(node.selectSingleNode("user").getStringValue());

                if (node.selectSingleNode("workspace") != null)
                    change.setWorkspace(node.selectSingleNode("workspace").getStringValue());

                List<Node> fileNodes = node.selectSingleNode("files").selectNodes("file");
                List<PerforceChangeLogEntry.FileEntry> files = new ArrayList<PerforceChangeLogEntry.FileEntry>();
                for (Node fnode : fileNodes) {
                    PerforceChangeLogEntry.FileEntry file = new PerforceChangeLogEntry.FileEntry();
                    file.setFilename(fnode.selectSingleNode("name").getStringValue());
                    file.setRevision(fnode.selectSingleNode("rev").getStringValue());
                    file.setAction(fnode.selectSingleNode("action").getStringValue());
                    files.add(file);
                }
                change.setFiles(files);

                List<Node> jobNodes = node.selectSingleNode("jobs").selectNodes("job");
                List<PerforceChangeLogEntry.JobEntry> jobs = new ArrayList<PerforceChangeLogEntry.JobEntry>();
                for (Node jnode : jobNodes) {
                    PerforceChangeLogEntry.JobEntry job = new PerforceChangeLogEntry.JobEntry();
                    job.setJob(jnode.selectSingleNode("name").getStringValue());
                    job.setDescription(jnode.selectSingleNode("description").getStringValue());
                    job.setStatus(jnode.selectSingleNode("status").getStringValue());
                    jobs.add(job);
                }
                change.setJobs(jobs);
                
                changeLogEntries.add(change);
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse changelog file: " + e.getMessage(), e);
        }

        return changeLogSet;
    }

    /**
     * Stores the history objects to the output stream as xml
     *
     * @param outputStream
     *            the stream to write to
     * @param changes
     *            the history objects to store
     * @throws IOException
     */
    public static void saveToChangeLog(OutputStream outputStream, List<IChangelist> changes) throws IOException, P4JavaException {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, Charset.forName("UTF-8"));
        WriterOutputStream stream1 = new WriterOutputStream(writer);
        PrintStream stream = new PrintStream(stream1);

        stream.println("<?xml version='1.0' encoding='UTF-8'?>");
        stream.println("<changelog>");
        for (IChangelist change : changes) {
            stream.println("\t<entry>");
            stream.println("\t\t<changenumber>" + change.getId() + "</changenumber>");
            stream.println("\t\t<date>" + Util.xmlEscape(javaDateToStringDate(change.getDate())) + "</date>");
            stream.println("\t\t<description>" + Util.xmlEscape(change.getDescription()) + "</description>");
            stream.println("\t\t<user>" + Util.xmlEscape(change.getUsername()) + "</user>");
            stream.println("\t\t<workspace>" + Util.xmlEscape(change.getClientId()) + "</workspace>");
            stream.println("\t\t<files>");
            for (IFileSpec fileSpec : change.getFiles(false)) {
                stream.println("\t\t\t<file>");
                stream.println("\t\t\t\t<name>" + Util.xmlEscape(fileSpec.getDepotPathString()) + "</name>");
                stream.println("\t\t\t\t<rev>" + fileSpec.getEndRevision() + "</rev>");
                stream.println("\t\t\t\t<action>" + fileSpec.getAction() + "</action>");
                stream.println("\t\t\t</file>");
            }
            stream.println("\t\t</files>");
            stream.println("\t\t<jobs>");
            for (IJob job : change.getJobs()) {
                stream.println("\t\t\t<job>");
                stream.println("\t\t\t\t<name>" + Util.xmlEscape(job.getId()) + "</name>");
                stream.println("\t\t\t\t<description>" + Util.xmlEscape(job.getDescription()) + "</description>");
                stream.println("\t\t\t\t<status>" + Util.xmlEscape(P4jUtil.jobStatus(job)) + "</status>");
                stream.println("\t\t\t</job>");
            }
            stream.println("\t\t</jobs>");
            stream.println("\t</entry>");
        }
        stream.println("</changelog>");
        stream.close();
    }

    /**
     * This takes a java.util.Date and converts it to a string.
     *
     * @return A string representation of the date
     */
    public static String javaDateToStringDate(java.util.Date newDate) {
        if (newDate == null)
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
        if (hour + min + sec > 0)
            date += " " + putZero(hour) + ":" + putZero(min) + ":" + putZero(sec);

        return date;
    }

    /**
     * Returns a java.util.Date object set to the time specified in newDate. The
     * format expected is the format of: YYYY-MM-DD HH:MM:SS
     *
     * @param newDate
     *            the string date to convert
     * @return A java.util.Date based off of the string format.
     */
    protected static java.util.Date stringDateToJavaDate(String newDate) throws IOException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date date = new java.util.Date();
        try {
            date = df.parse(newDate);
        } catch (ParseException e) {
            throw new IOException("Could not parse changelog date: " + e.getMessage(), e);
        }
        return date;
    }

    private static String putZero(int i) {
        if (i < 10) {
            return "0" + i;
        }
        return i + "";
    }
}
