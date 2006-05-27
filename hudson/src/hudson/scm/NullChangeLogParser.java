package hudson.scm;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import hudson.model.Build;

/**
 * {@link ChangeLogParser} for no SCM.
 * @author Kohsuke Kawaguchi
 */
public class NullChangeLogParser extends ChangeLogParser {
    public ChangeLogSet parse(Build build, File changelogFile) throws IOException, SAXException {
        return ChangeLogSet.EMPTY;
    }
}
