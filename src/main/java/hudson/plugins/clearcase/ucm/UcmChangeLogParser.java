package hudson.plugins.clearcase.ucm;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;

/***********************************************************************************************************************
 * ClearCase change log parser.
 * 
 * @author Erik Ramfelt
 */
public class UcmChangeLogParser extends ChangeLogParser {

    /**
     * Parses the change log file and returns a ClearCase change log set.
     * 
     * @param build the build for the change log
     * @param changeLogFile the change log file
     * @return the change log set
     */
    @Override
    public UcmChangeLogSet parse(AbstractBuild build, File changeLogFile) throws IOException, SAXException {
        return parse(build, new FileInputStream(changeLogFile));
    }

    /**
     * Parses the change log stream and returns a ClearCase change log set.
     * 
     * @param build the build for the change log
     * @param changeLogStream input stream containing the change log
     * @return the change log set
     */
    public UcmChangeLogSet parse(AbstractBuild build, InputStream changeLogStream) throws IOException, SAXException {

        ArrayList<UcmActivity> history = new ArrayList<UcmActivity>();

        // Parse the change log file.
        Digester digester = new Digester();
        digester.setClassLoader(UcmChangeLogSet.class.getClassLoader());
        digester.push(history);
        digester.addObjectCreate("*/entry", UcmActivity.class);

        digester.addBeanPropertySetter("*/entry/name");
        digester.addBeanPropertySetter("*/entry/headline");
        digester.addBeanPropertySetter("*/entry/stream");
        digester.addBeanPropertySetter("*/entry/view");
        digester.addBeanPropertySetter("*/entry/user");
        // TODO, is the user name really needed? 
        // digester.addBeanPropertySetter("*/entry/userName");

        digester.addObjectCreate("*/entry/file", UcmActivity.File.class);
        digester.addBeanPropertySetter("*/entry/file/name");
        digester.addBeanPropertySetter("*/entry/file/date", "dateStr");
        digester.addBeanPropertySetter("*/entry/file/comment");
        digester.addBeanPropertySetter("*/entry/file/version");
        digester.addBeanPropertySetter("*/entry/file/event");
        digester.addBeanPropertySetter("*/entry/file/operation");
        digester.addSetNext("*/entry/file", "addFile");

        digester.addSetNext("*/entry", "add");
        digester.parse(changeLogStream);

        return new UcmChangeLogSet(build, history);
    }

}
