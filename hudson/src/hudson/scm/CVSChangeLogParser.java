package hudson.scm;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import hudson.model.Build;

/**
 * {@link ChangeLogParser} for CVS.
 * @author Kohsuke Kawaguchi
 */
public class CVSChangeLogParser extends ChangeLogParser {
    public CVSChangeLogSet parse(Build build, File changelogFile) throws IOException, SAXException {
        return CVSChangeLogSet.parse(changelogFile);
    }
}
