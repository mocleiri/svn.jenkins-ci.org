package hudson.plugins.clearcase.ucm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;

/**
 * UCM ClearCase change log set.
 * 
 * @author Henrik L. Hansen
 */
public class UcmChangeLogSet extends ChangeLogSet<UcmActivity> {

    static final String[] ACTIVITY_TAGS = new String[]{"name", "headline", "stream", "view", "user", "userName"};
    static final String[] FILE_TAGS = new String[]{"name", "date", "comment", "version", "event", "operation"};
    private List<UcmActivity> history = null;

    public UcmChangeLogSet(AbstractBuild<?, ?> build, List<UcmActivity> logs) {
        super(build);
        for (UcmActivity entry : logs) {
            entry.setParent(this);
        }
        this.history = Collections.unmodifiableList(logs);
    }

    @Override
    public boolean isEmptySet() {
        return history.size() == 0;
    }

    public Iterator<UcmActivity> iterator() {
        return history.iterator();
    }

    public List<UcmActivity> getLogs() {
        return history;
    }

    /**
     * Stores the history objects to the output stream as xml
     * 
     * @param outputStream the stream to write to
     * @param history the history objects to store
     * @throws IOException
     */
    public static void saveToChangeLog(OutputStream outputStream, List<UcmActivity> history)
            throws IOException {
        PrintStream stream = new PrintStream(outputStream, false, "UTF-8");

        stream.println("<?xml version='1.0' encoding='UTF-8'?>");
        stream.println("<history>");
        for (UcmActivity entry : history) {
            stream.println("\t<entry>");
            String[] activityValues = getEntryAsStrings(entry);
            for (int tag = 0; tag < ACTIVITY_TAGS.length; tag++) {
                stream.print("\t\t<");
                stream.print(UcmChangeLogSet.ACTIVITY_TAGS[tag]);
                stream.print('>');
                stream.print(escapeForXml(activityValues[tag]));
                stream.print("</");
                stream.print(UcmChangeLogSet.ACTIVITY_TAGS[tag]);
                stream.println('>');
            }
            for (UcmActivity.File file : entry.getFiles()) {
                stream.println("\t\t<file>");
                String[] fileValues = getFileAsStrings(file);
                for (int tag = 0; tag < FILE_TAGS.length; tag++) {
                    stream.print("\t\t\t<");
                    stream.print(UcmChangeLogSet.FILE_TAGS[tag]);
                    stream.print('>');
                    stream.print(escapeForXml(fileValues[tag]));
                    stream.print("</");
                    stream.print(UcmChangeLogSet.FILE_TAGS[tag]);
                    stream.println('>');

                }
                stream.println("\t\t</file>");
            }
            stream.println("\t</entry>");
        }
        stream.println("</history>");
        stream.close();
    }

    private static String[] getEntryAsStrings(UcmActivity entry) {
        String[] array = new String[ACTIVITY_TAGS.length];
        array[0] = entry.getName();
        array[1] = entry.getHeadline();
        array[2] = entry.getStream();
        array[3] = entry.getView();
        array[4] = entry.getUser();
        array[5] = entry.getUserName();
        return array;
    }

    private static String[] getFileAsStrings(UcmActivity.File entry) {
        String[] array = new String[FILE_TAGS.length];
        array[0] = entry.getName();
        array[1] = entry.getDateStr();
        array[2] = entry.getComment();
        array[3] = entry.getVersion();
        array[4] = entry.getEvent();
        array[5] = entry.getOperation();
        return array;
    }    
    
    private static String escapeForXml(String string) {
        if (string == null) {
            return "";
        }

        // Loop through and replace the special chars.
        int size = string.length();
        char ch = 0;
        StringBuffer escapedString = new StringBuffer(size);
        for (int index = 0; index < size; index++) {
            // Convert special chars.
            ch = string.charAt(index);
            switch (ch) {
                case '&':
                    escapedString.append("&amp;");
                    break;
                case '<':
                    escapedString.append("&lt;");
                    break;
                case '>':
                    escapedString.append("&gt;");
                    break;
                case '\'':
                    escapedString.append("&apos;");
                    break;
                case '\"':
                    escapedString.append("&quot;");
                    break;
                default:
                    escapedString.append(ch);
            }
        }

        return escapedString.toString().trim();
    }
}
