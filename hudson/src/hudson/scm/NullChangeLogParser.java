package hudson.scm;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

/**
 * {@link ChangeLogParser} for no SCM.
 * @author Kohsuke Kawaguchi
 */
public class NullChangeLogParser extends ChangeLogParser {
    public ChangeLogSet parse(File changelogFile) throws IOException, SAXException {
        return ChangeLogSet.EMPTY;
    }
}
