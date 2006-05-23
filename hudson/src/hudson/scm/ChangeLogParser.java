package hudson.scm;

import hudson.model.Build;

import java.io.File;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * Encapsulates the file format of the changelog.
 *
 * Instances should be stateless, but
 * persisted as a part of {@link Build}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ChangeLogParser {
    public abstract ChangeLogSet parse(File changelogFile) throws IOException, SAXException;
}
